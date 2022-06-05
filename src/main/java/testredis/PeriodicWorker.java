package testredis;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.redis.client.RedisOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PeriodicWorker extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(PeriodicWorker.class.getName());

  private ConcurrentHashMap<Long, JsonObject> hashMap = new ConcurrentHashMap<Long, JsonObject>();

  private RedisClient redisClient;

  @Override
  public void start() throws Exception {
    initRedis();
    addToRedisPeriodic();
    startEventsPeriodic();

    startCallSharedServerPeriodic();
  }

  private void initRedis() {
    RedisOptions options = new RedisOptions()
      .setConnectionString("redis://127.0.0.1:" + 6379)
      .setMaxPoolSize(12)
      .setMaxWaitingHandlers(24)
      .setMaxPoolWaiting(200000000);

    redisClient = new RedisClient(vertx, options, "mobjects");
  }

  private void addToRedisPeriodic() {
    try {
      Random random = new Random(1000);

      final Long[] z = {0L};

      vertx.setPeriodic(1000, x -> {

        for (int i = 0; i < 10000; ++i) {
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
      LOGGER.error(e);
    }
  }

  private void startEventsPeriodic() {
    // Producer side
    vertx.setPeriodic(2000, x -> {
      vertx.eventBus().request("greeting", "Hello Micrometer from event bus!")
        .onComplete(replyRes -> {
//          LOGGER.info(replyRes.result().body());
        });
    });

    // Consumer side
    vertx.eventBus().<String>consumer("greeting", message -> {
      String greeting = message.body();
      LOGGER.info("receive greeting");
      message.reply("Hello back!");
    });

    WebClient webClient = createWebClient();
    // Producer side
    vertx.setPeriodic(10000, x -> {
      webClient
        .get(80, "iss.moex.com", "/iss/securities.json")
        .addQueryParam("q", "РСХБ")
        .send()
        .onComplete(res -> {
          if (res.succeeded()) {
            try {
              LOGGER.info("success request: " + res.result().bodyAsString().length());
            } catch (Exception e) {
              LOGGER.error(e);
            }
          } else {
            LOGGER.info("request failed: " + res.cause());
          }
        });
    });
  }

  private void startCallSharedServerPeriodic() {
    vertx.deployVerticle(() -> new AbstractVerticle() {
      HttpClient client;
      @Override
      public void start() {
        // The client creates and use two event-loops for 4 instances
        client = vertx.createHttpClient(new HttpClientOptions().setPoolEventLoopSize(2).setShared(true).setName("my-client"));

        vertx.setPeriodic(100, (l) -> {
          client.request(HttpMethod.GET, 8889, "localhost", "/", ar1 -> {
            if (ar1.succeeded()) {
              HttpClientRequest request = ar1.result();
              request.send(ar2 -> {
                if (ar2.succeeded()) {
                  HttpClientResponse resp = ar2.result();
                  resp.bodyHandler(body -> {
                    LOGGER.info(body);
                  });
                }
              });
            }
          });
        });

      }
    }, new DeploymentOptions().setInstances(4));



  }

  private WebClient createWebClient() {
    WebClientOptions webClientOptions = new WebClientOptions();
    webClientOptions.setDefaultPort(80)
      .setDefaultHost("iss.moex.com");
    WebClient webClient = WebClient.create(vertx, webClientOptions);
    return webClient;
  }

}
