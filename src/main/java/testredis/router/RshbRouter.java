package testredis.router;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class RshbRouter {
  private WebClient webClient;
  private final Router router;

  public RshbRouter(Vertx vertx) {
    router = Router.router(vertx);
    createWebClient(vertx);

    setRoutes();
  }

  private void createWebClient(Vertx vertx) {
    WebClientOptions webClientOptions = new WebClientOptions();
    webClientOptions.setDefaultPort(80)
      .setDefaultHost("iss.moex.com");
    webClient = WebClient.create(vertx, webClientOptions);
  }

  private void setRoutes() {

    router.get("/rshb_bonds").handler(rc -> {
      webClient
        .get(80, "iss.moex.com", "/iss/securities.json")
        .addQueryParam("q", "РСХБ")
        .send(response -> {
          rc.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(processMoexBondsRequest(response.result().bodyAsJsonObject()).encodePrettily());
        });
    });
    router.get("/rshb_bonds/:bondId").handler(rc -> {
      String bondId = rc.request().getParam("bondId");
      webClient
        .get("/iss/securities/" + bondId + ".json")
        .send(response -> {
          rc.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(processMoexBondDescriptionRequest(response.result().bodyAsJsonObject()));
        });
    });

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

  public Router getRouter() {
    return router;
  }
}
