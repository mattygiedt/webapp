package webapp.config;

import com.google.protobuf.TextFormat;
import webapp.proto.WebappConfigProtos.WebappServer;

public class WebappServerConfig {
  private WebappServerConfig() {}

  public static WebappServer from(final String data) throws Exception {
    final WebappServer.Builder builder = WebappServer.newBuilder();
    TextFormat.merge(data, builder);
    return builder.build();
  }

  public static WebappServer fromFile(final String filename) throws Exception {
    return WebappServerConfig.from(
        new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filename)),
            java.nio.charset.Charset.defaultCharset()));
  }

  public static WebappServer fromResource(final String filename) throws Exception {
    return WebappServerConfig.from(new String(
        WebappServerConfig.class.getClassLoader().getResourceAsStream(filename).readAllBytes(),
        java.nio.charset.Charset.defaultCharset()));
  }

  public static String print(final WebappServer.Instance config) throws Exception {
    final StringBuilder sb = new StringBuilder();
    TextFormat.print(config, sb);
    return sb.toString();
  }

  public static String print(final WebappServer config) throws Exception {
    final StringBuilder sb = new StringBuilder();
    TextFormat.print(config, sb);
    return sb.toString();
  }
}
