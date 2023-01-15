package webapp;

import static spark.Spark.*; //  http://sparkjava.com/documentation

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webapp.config.WebappServerConfig;
import webapp.proto.WebappConfigProtos;

public final class App {
  private static String getBanner(final String filename) throws Exception {
    // https://patorjk.com/software/taag/#p=display&f=Swamp%20Land

    String line;
    final InputStream in = App.class.getClassLoader().getResourceAsStream(filename);
    final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    final StringBuilder out = new StringBuilder();

    out.append(System.lineSeparator());

    while ((line = reader.readLine()) != null) {
      out.append(line).append(System.lineSeparator());
    }

    out.append(System.lineSeparator()).append(System.lineSeparator());

    reader.close();

    return out.toString();
  }

  private static Logger configureLogback(final String filename) throws Exception {
    //
    // assume SLF4J is bound to logback in the current environment
    //

    final JoranConfigurator configurator = new JoranConfigurator();
    final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    configurator.setContext(context);

    //
    // Call context.reset() to clear any previous configuration, e.g. default
    // configuration. For multi-step configuration, omit calling context.reset().
    //

    context.reset();

    //
    // Test for presence of LOGBACK_CONFIG environment variable override
    //

    if (System.getenv().containsKey("LOGBACK_CONFIG")) {
      System.out.println(
          "Loading JoranConfigurator configuration file from env property LOGBACK_CONFIG: "
          + System.getenv().get("LOGBACK_CONFIG"));
      configurator.doConfigure(System.getenv().get("LOGBACK_CONFIG"));
    } else {
      System.out.println(
          "JoranConfigurator configuration file from application configuration: " + filename);
      configurator.doConfigure(App.class.getClassLoader().getResourceAsStream(filename));
    }

    StatusPrinter.printInCaseOfErrorsOrWarnings(context);

    return LoggerFactory.getLogger(App.class);
  }

  public static void main(final String[] args) throws Exception {
    //
    // Load the config -- can (should) be added to args[], but this is dummy example code...
    //

    WebappConfigProtos.WebappServer config = WebappServerConfig.fromResource("config/webapp.conf");

    //
    // Configure logger and print banner
    //

    final Logger log = App.configureLogback(config.getLogback());
    log.info(App.getBanner(config.getBanner()));

    //
    // Single WebSocket Handler used by all webapp instances
    //

    final WebSocketHandler websocketHandler = WebSocketHandler.getInstance();

    //
    //  Load the instances
    //

    final List<SparkEndpoint> endpoints = new ArrayList<>();
    for (WebappConfigProtos.WebappServer.Instance instance : config.getInstanceList()) {
      final WebappConfigProtos.Spark spark = instance.getSpark();
      endpoints.add(
          new SparkEndpoint(spark.getPort(), spark.getStaticFilePath(), websocketHandler));
    }

    int returnValue = 0;
    final CountDownLatch shutdownSignal = new CountDownLatch(1);

    //
    //  Create the shutdown hook
    //

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          for (final SparkEndpoint endpoint : endpoints) {
            endpoint.shutdown();
          }
        } catch (Throwable t) {
          log.error("shutdown hook error:", t);
        } finally {
          shutdownSignal.countDown();
        }
      }
    });

    //
    //  Start and run our webapp endpoints, waiting for ctrl-c
    //

    try {
      for (final SparkEndpoint endpoint : endpoints) {
        endpoint.start();
        endpoint.setEndpoints();
      }

      //
      //  Wait for ctrl-c or signal
      //

      shutdownSignal.await();
    } catch (Throwable t) {
      returnValue = 1;
      log.error("fatal runService error", t);
    } finally {
      Runtime.getRuntime().exit(returnValue);
    }
  }
}
