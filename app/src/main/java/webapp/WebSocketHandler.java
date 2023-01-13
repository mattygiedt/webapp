package webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.StampedLock;
import org.eclipse.jetty.websocket.api.CloseException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class WebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

  public interface WebSocketHandlerCallback {
    void onOpenConnection(final Session session);
    void onCloseConnection(final Session session);
  }

  private final List<WebSocketHandlerCallback> callbackList;
  private final Queue<Session> sessions;
  private final StampedLock lock;

  //
  //  Singleton Holder (initialization on demand)
  //

  private WebSocketHandler() {
    callbackList = new ArrayList<>();
    sessions = new ConcurrentLinkedQueue<>();
    lock = new StampedLock();
  }

  private static class WebSocketHandlerHolder {
    static final WebSocketHandler INSTANCE = new WebSocketHandler();
  }

  public static WebSocketHandler getInstance() {
    return WebSocketHandlerHolder.INSTANCE;
  }

  private void sendString(final String msg, final Session session) {
    try {
      session.getRemote().sendString(msg);
    } catch (IOException ioe) {
      log.error("send error:", ioe);
    }
  }

  @OnWebSocketConnect
  public void connected(final Session session) {
    log.info("websocket session connect: {}", session);

    final long stamp = lock.writeLock();
    try {
      sessions.add(session);
      callbackList.forEach(c -> c.onOpenConnection(session));
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  @OnWebSocketClose
  public void closed(final Session session, final int statusCode, final String reason) {
    log.info("websocket session disconnect: {} {} {}", session, statusCode, reason);

    final long stamp = lock.writeLock();
    try {
      sessions.remove(session);
      callbackList.forEach(c -> c.onCloseConnection(session));
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  @OnWebSocketMessage
  public void message(final Session session, final String message) throws IOException {
    log.info("OnWebSocketMessage: {}", message); // Print message
    sendString(message, session); // and send it back
  }

  @OnWebSocketError
  public void onError(final Session session, final Throwable error) {
    log.error("OnWebSocketError:Throwable:", error);
  }

  public void registerCallback(final WebSocketHandlerCallback callback) {
    final long stamp = lock.writeLock();
    try {
      callbackList.add(callback);
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  public void deregisterCallback(final WebSocketHandlerCallback callback) {
    final long stamp = lock.writeLock();
    try {
      callbackList.remove(callback);
    } finally {
      lock.unlockWrite(stamp);
    }
  }
}
