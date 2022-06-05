package uz.uzgps.testredis;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;

public class LauncherWithMetrics extends Launcher {
  public static void main(String[] args) {
    new LauncherWithMetrics().dispatch(args);
    System.out.println("-----START LauncherWithMetrics-----");
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    System.out.println("-----START LauncherWithMetrics 2222-----");
    options.setMetricsOptions(new MicrometerMetricsOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
        .setPublishQuantiles(true)
//        .setStartEmbeddedServer(true)
//        .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090))
//        .setEmbeddedServerEndpoint("/metrics")
        .setEnabled(true)
      )
//      .addLabels(Label.LOCAL, Label.HTTP_PATH, Label.REMOTE)

      .setEnabled(true));


  }
}
