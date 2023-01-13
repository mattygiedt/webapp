package webapp;

import static java.util.Map.entry;
import static spark.Spark.*; //  http://sparkjava.com/documentation

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkEndpoint {
  private static final Logger log = LoggerFactory.getLogger(SparkEndpoint.class);

  private final int port;
  private final String staticFilePath;
  private final WebSocketHandler webSocket;

  public SparkEndpoint(final int port, final String staticFilePath,
      final WebSocketHandler webSocket) throws Exception {
    this.port = port;
    this.staticFilePath = staticFilePath;
    this.webSocket = webSocket;
  }

  public void start() throws Exception {
    //
    //  Set the Spark port
    //

    port(port);

    //
    //  root is 'src/main/resources', so put files in 'src/main/resources/public/...'
    //  and configure staticFilePath = "/public/..."
    //

    staticFiles.location(staticFilePath);

    //
    //  http://sparkjava.com/documentation#embedded-web-server
    //  https://github.com/perwendel/spark/blob/master/src/main/java/spark/Spark.java
    //

    webSocket("/websocket", webSocket);

    //
    //  Start the spark Jetty webserver
    //

    init();
  }

  private String nextPosition() {
    return Integer.toString((int) (Math.random() * 100000));
  }

  public void setEndpoints() throws Exception {
    get("/strategies", (request, response) -> {
      return Map.ofEntries(entry("Strategy One", "strategy_1"), entry("Strategy Two", "strategy_2"),
          entry("Strategy Three", "strategy_3"), entry("Strategy Four", "strategy_4"));
    }, new JsonTransformer());

    get("/positions/:strategy", (request, response) -> {
      final String strategy = request.params(":strategy");

      log.info("GET positions for strategy: {}", strategy);
      Map<String, Map<String, String>> position_map = new HashMap<>();

      for (final String symbol : Arrays.asList("Apple Equity:AAPL:037833100",
               "Netflix Equity:NFLX:64110L106", "Google Class A Equity:GOOGL:02079K305",
               "Exxon Equity:XOM:30231G102", "Amazon Equity:AMZN:023135106")) {
        final String[] symbolArray = symbol.split(":");

        position_map.put(symbol,
            Map.ofEntries(entry("security", symbolArray[0]), entry("cusip", symbolArray[2]),
                entry("ticker", symbolArray[1]), entry("net", nextPosition()),
                entry("open", nextPosition())));
      }

      return position_map;
    }, new JsonTransformer());
  }

  public void shutdown() throws Exception {
    //
    //  https://sparkjava.com/documentation#stopping-the-server
    //

    stop();
  }
}
