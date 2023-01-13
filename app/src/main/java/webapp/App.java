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
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    final Logger log = App.configureLogback("logging/logback.xml");
    log.info(App.getBanner("banner/webapp.txt"));

    int returnValue = 0;
    final WebSocketHandler websocketHandler = WebSocketHandler.getInstance();
    final SparkEndpoint endpoint = new SparkEndpoint(8888, "/public/webapp", websocketHandler);
    final CountDownLatch shutdownSignal = new CountDownLatch(1);

    //
    //  Create the shutdown hook
    //

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          endpoint.shutdown();
        } catch (Throwable t) {
          log.error("shutdown hook error:", t);
        } finally {
          shutdownSignal.countDown();
        }
      }
    });

    //
    //  Start and run our webapp, waiting for ctrl-c
    //

    try {
      endpoint.start();
      endpoint.setEndpoints();
      shutdownSignal.await();
    } catch (Throwable t) {
      returnValue = 1;
      log.error("fatal runService error", t);
    } finally {
      Runtime.getRuntime().exit(returnValue);
    }
  }
}
