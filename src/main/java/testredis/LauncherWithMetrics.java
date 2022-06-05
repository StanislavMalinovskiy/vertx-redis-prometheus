package testredis;

import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

public class LauncherWithMetrics extends Launcher {
  public static void main(String[] args) {
    new LauncherWithMetrics().dispatch(args);
//    System.out.println("-----START LauncherWithMetrics-----");
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
//    System.out.println("-----START LauncherWithMetrics 2222-----");
    options.setMetricsOptions(new MicrometerMetricsOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
//          .setPublishQuantiles(true)
//        .setStartEmbeddedServer(true)
//        .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090))
//        .setEmbeddedServerEndpoint("/metrics")
          .setEnabled(true)
      )
//      .addLabels(Label.LOCAL, Label.HTTP_PATH, Label.REMOTE)

      .setEnabled(true));


  }
}
