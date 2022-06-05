package uz.uzgps.testredis;

import com.mchange.util.AssertException;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.redis.client.RedisOptions;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("ALL")
public class MainVerticle extends AbstractVerticle {
  private RedisClient redisClient;

  private ConcurrentHashMap<Long, JsonObject> hashMap = new ConcurrentHashMap<Long, JsonObject>();

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    PrometheusMeterRegistry registry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
    registry.config().meterFilter(
      new MeterFilter() {
        @Override
        public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
          return DistributionStatisticConfig.builder()
            .percentilesHistogram(true)
            .build()
            .merge(config);
        }
      });

    WebClient webClient = createWebClient();
    Router mainRouter = createRouter(webClient);

    createTestRouter(mainRouter);
    initRedis();

    vertx.createHttpServer()
      .requestHandler(mainRouter)
      .listen(8888, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8888");
        } else {
          startPromise.fail(http.cause());
        }
      });

    vertx.deployVerticle(MetricsBusPublisher.class.getName());

//    startPeriodicEvents();
    startPeriodicRedis();
  }

  private Router createRouter(WebClient webClient) {
    Router mainRouter = Router.router(vertx);

    mainRouter.get("/hello").handler(rc -> {
      rc.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
        .end("Hello from Vert.x!");
    });

    mainRouter.route("/rest/api/*").subRouter(createRestRouter(webClient));

    mainRouter.route("/metrics").handler(PrometheusScrapingHandler.create());

    SockJSBridgeOptions opts = new SockJSBridgeOptions()
      .addOutboundPermitted(new PermittedOptions()
        .setAddress("metrics"));
    mainRouter.route("/eventbus/*").subRouter(SockJSHandler.create(vertx).bridge(opts));

    return mainRouter;
  }

  private WebClient createWebClient() {
    WebClientOptions webClientOptions = new WebClientOptions();
    webClientOptions.setDefaultPort(80)
      .setDefaultHost("iss.moex.com");
    WebClient webClient = WebClient.create(vertx, webClientOptions);
    return webClient;
  }

  private Router createRestRouter(WebClient webClient) {
    Router restApi = Router.router(vertx);

    restApi.get("/rshb_bonds").handler(rc -> {
      webClient
        .get(80, "iss.moex.com", "/iss/securities.json")
        .addQueryParam("q", "РСХБ")
        .send(response -> {
          rc.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(processMoexBondsRequest(response.result().bodyAsJsonObject()).encodePrettily());
        });
    });
    restApi.get("/rshb_bonds/:bondId").handler(rc -> {
      String bondId = rc.request().getParam("bondId");
      webClient
        .get("/iss/securities/" + bondId + ".json")
        .send(response -> {
          rc.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(processMoexBondDescriptionRequest(response.result().bodyAsJsonObject()));
        });
    });

    return restApi;
  }

  private void createTestRouter(Router mainRouter) {
    Router testApi = Router.router(vertx);

    testApi.get("/").handler(rc -> {
      rc.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .end(new JsonObject().put("sd", 1000).encodePrettily());
    });

    testApi.post("/add").handler(rc -> {
      JsonObject parameters = rc.body().asJsonObject();

    });

    testApi.get("/:id").handler(rc -> {
      String id = rc.request().getParam("id");

      rc.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .end(new JsonObject().put("sd", id).encodePrettily());
    });

    mainRouter.route("/rest/api/model/*").subRouter(testApi);

  }


  private void startPeriodicEvents() {
    // Producer side
    vertx.setPeriodic(100, x -> {
      vertx.eventBus().send("greeting", "Hello Micrometer from event bus!");
    });

    // Consumer side
    vertx.eventBus().<String>consumer("greeting", message -> {
      String greeting = message.body();
      System.out.println("Received: " + greeting);
      message.reply("Hello back!");
    });

    WebClient webClient = createWebClient();
    // Producer side
    vertx.setPeriodic(3000, x -> {
      webClient
        .get(80, "iss.moex.com", "/iss/securities.json")
        .addQueryParam("q", "РСХБ")
        .send()
        .onComplete(res -> {
          if (res.succeeded()) {


            System.out.println("good");

            try {
              int sd = 0;
              int sdd = 0;
//              sdd = 1000 / sd;
              System.out.println(sd);
            } catch (Exception e) {
              System.out.println(e);
              e.printStackTrace();
              throw new AssertException(e.getCause().toString());
            }

          } else {
            System.out.println("fail");
          }
        });
    });

  }

  private void initRedis() {
    RedisOptions options = new RedisOptions()
      .setConnectionString("redis://127.0.0.1" + ":" + 6379)
      .setMaxPoolSize(12)
      .setMaxWaitingHandlers(24)
      .setMaxPoolWaiting(200000000);

    redisClient = new RedisClient(vertx, options, "mobjects");
  }

  private void startPeriodicRedis() {
    try {
      Random random = new Random(1000);

      final Long[] z = {0L};

      vertx.setPeriodic(6000, x -> {

        for (int i = 0; i < 100000; ++i) {
          Long mobjectId = (i + z[0] + 1);

          JsonObject mobject = new JsonObject()
            .put("mobjectId", mobjectId)
            .put("name", "mobject name")
            .put("description", "mobject description")
            .put("weight", 123)
            .put("val_1", 1)
            .put("val_2", 2)
            .put("val_3", 3)
            .put("val_4", "1")
            .put("val_5", "text")
            .put("val_6", new JsonObject().put("test", 9999))
            .put("val_7", 23.88);

          redisClient.addValue(String.valueOf(mobjectId), String.valueOf(mobject));
        }
        z[0] += 100000L;
      });

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String processMoexBondDescriptionRequest(JsonObject moexJson) {
    return moexJson.getJsonObject("description").getJsonArray("data")
      .stream()
      .map(arr -> (JsonArray) arr)
      .collect(
        JsonObject::new, (json, arr) -> json.put(arr.getString(1), arr.getString(2)), JsonObject::mergeIn
      )
      .encodePrettily();
  }

  private JsonObject processMoexBondsRequest(JsonObject moexJson) {
    return moexJson.getJsonObject("securities")
      .getJsonArray("data")
      .stream()
      .map(arr -> (JsonArray) arr)
      .filter(arr -> arr.getString(1).startsWith("RU"))
      .collect(
        JsonObject::new, (json, arr) -> json.put(arr.getString(1), arr.getString(2)), JsonObject::mergeIn
      );
  }
}
