package testredis;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.backends.BackendRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import testredis.metrics.MetricsBusPublisher;
import testredis.router.RshbRouter;
import testredis.router.TestRouter;

public class MainVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(MainVerticle.class.getName());

  @Override
  public void start() throws Exception {
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

    Router mainRouter = createMainRouter();
    mainRouter.route("/rest/api/*").subRouter(new RshbRouter(vertx).getRouter());
    mainRouter.route("/rest/api/model/*").subRouter(new TestRouter(vertx).getRouter());

    createHttpServer(mainRouter)
      .onComplete(res -> {
        if (res.succeeded()) {
          vertx.deployVerticle(MetricsBusPublisher.class.getName());
          vertx.deployVerticle(PeriodicWorker.class.getName(), new DeploymentOptions().setWorkerPoolSize(6));
        } else {
          LOGGER.error(res.cause());
        }
      });

    vertx.deployVerticle(HttpSharedServer.class.getName(),
      new DeploymentOptions().setInstances(2)
        .setWorker(true)
        .setWorkerPoolName("worker_1"));
  }

  private Future<Void> createHttpServer(Router mainRouter) {
    Promise<Void> promise = Promise.promise();

    vertx.createHttpServer()
      .requestHandler(mainRouter)
      .listen(8888, http -> {
        if (http.succeeded()) {
          promise.complete();
          LOGGER.info("HTTP server started on port 8888");
        } else {
          promise.fail(http.cause());
        }
      });

    return promise.future();
  }

  private Router createMainRouter() {
    Router mainRouter = Router.router(vertx);

    mainRouter.get("/hello").handler(rc -> {
      rc.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
        .end("Hello from Vert.x!");
    });

    mainRouter.route("/metrics").handler(PrometheusScrapingHandler.create());

    SockJSBridgeOptions opts = new SockJSBridgeOptions()
      .addOutboundPermitted(new PermittedOptions()
        .setAddress("metrics"));

    mainRouter.route("/eventbus/*").subRouter(SockJSHandler.create(vertx).bridge(opts));

    return mainRouter;
  }

}
