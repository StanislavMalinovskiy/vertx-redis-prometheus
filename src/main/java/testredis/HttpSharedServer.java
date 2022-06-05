package testredis;

import io.vertx.core.AbstractVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpSharedServer extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(PeriodicWorker.class.getName());

  @Override
  public void start() {

    vertx.createHttpServer().requestHandler(request -> {
      request.response().end("Hello from shared server " + this);
    }).listen(8889, http -> {
      if (http.succeeded()) {
        LOGGER.info("HTTP server started on port 8889");
      } else {
        LOGGER.error("HTTP server start failed");
      }
    });

  }
}
