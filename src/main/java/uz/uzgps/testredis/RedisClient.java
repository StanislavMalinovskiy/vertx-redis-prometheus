package uz.uzgps.testredis;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Response;

import java.util.Arrays;

public class RedisClient {

  private Vertx vertx;
  private RedisOptions redisOptions;
  private Redis redis;
  private RedisAPI redisAPI;
  private String table;

  public RedisClient(Vertx vertx, RedisOptions redisOptions, String table) {
    this.vertx = vertx;
    this.redisOptions = redisOptions;
    this.redis = Redis.createClient(vertx, redisOptions);
    this.redisAPI = RedisAPI.api(redis);
    this.table = table;
  }

  public Future<JsonObject> getValue(String key) {
    Promise<JsonObject> promise = Promise.promise();

    redisAPI.hget(table, key, res -> {
      if (res.succeeded() && res.result() != null) {
        JsonObject object = new JsonObject(res.result().toString());
        promise.complete(object);
      } else {
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }


  long maxVal;
  public Future<Void> addValue(String key, String value) {
    Promise<Void> promise = Promise.promise();

    try {
      redisAPI.hset(Arrays.asList(table, key, value), res -> {
        if (res.succeeded()) {
          promise.complete();
        } else {
          System.out.println(res.cause().toString());
          promise.fail(res.cause());
        }
      });
    } catch (Exception e) {
      System.out.println(e);
    }

    return promise.future();
  }

  public Future<JsonArray> getAllHash() {
    Promise<JsonArray> promise = Promise.promise();

    redisAPI.hgetall(table, res -> {
      if (res.succeeded()) {
        JsonArray jsonArray = new JsonArray();
        Response response = res.result();

        for (int i = 0; i < res.result().size(); i += 2) {
          Response key = response.get(i);
          Response value = response.get(i + 1);
          jsonArray.add(new JsonObject().put(key.toString(), value));
        }

        promise.complete(jsonArray);
      } else {
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }

  public Future<JsonArray> getAllHashValues() {
    Promise<JsonArray> promise = Promise.promise();

    try {
      redisAPI.hgetall(table, res -> {
        if (res.succeeded()) {
          JsonArray jsonArray = new JsonArray();
          for (int i = 1; i < res.result().size(); i += 2) {

            jsonArray.add(new JsonObject(res.result().get(i).toString()));
          }
//          LOGGER.info(jsonArray);
          promise.complete(sort(jsonArray));
        } else {
          promise.fail(res.cause());
        }
      });
    } catch (Exception e) {
      System.out.println(e);
    }

    return promise.future();
  }

  private static JsonArray sort(JsonArray jsonArray) {
    int n = jsonArray.size();

    // One by one move boundary of unsorted subarray
    for (int i = 0; i < n - 1; i++) {
      // Find the minimum element in unsorted array
      int min_idx = i;
      for (int j = i + 1; j < n; j++)
        if (jsonArray.getJsonObject(j).getInteger("carId") < jsonArray.getJsonObject(min_idx).getInteger("carId")) {
          min_idx = j;
        }

      // Swap the found minimum element with the first
      // element
      JsonObject temp = jsonArray.getJsonObject(min_idx);
      jsonArray.set(min_idx, jsonArray.getJsonObject(i));
      jsonArray.set(i, temp);
    }

    return jsonArray;
  }
}

