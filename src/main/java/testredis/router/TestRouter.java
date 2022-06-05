package testredis.router;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class TestRouter {
  private Router router;

  public TestRouter(Vertx vertx) {
    router = Router.router(vertx);

    setRoutes();
  }


  private void setRoutes() {

    router.get("/").handler(rc -> {
      rc.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .end(new JsonObject().put("sd", 1000).encodePrettily());
    });

    router.post("/add").handler(rc -> {
      JsonObject parameters = rc.body().asJsonObject();

    });

    router.get("/:id").handler(rc -> {
      String id = rc.request().getParam("id");

      rc.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .end(new JsonObject().put("sd", id).encodePrettily());
    });

  }

  public Router getRouter() {
    return router;
  }
}
