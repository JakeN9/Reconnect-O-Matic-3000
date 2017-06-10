package io.netty.handler.codec.http.websocketx;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.URI;

public class WebSocketClientHandshaker07
  extends WebSocketClientHandshaker
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketClientHandshaker07.class);
  public static final String MAGIC_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
  private String expectedChallengeResponseString;
  private final boolean allowExtensions;
  private final boolean performMasking;
  private final boolean allowMaskMismatch;
  
  public WebSocketClientHandshaker07(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength)
  {
    this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, true, false);
  }
  
  public WebSocketClientHandshaker07(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean performMasking, boolean allowMaskMismatch)
  {
    super(webSocketURL, version, subprotocol, customHeaders, maxFramePayloadLength);
    this.allowExtensions = allowExtensions;
    this.performMasking = performMasking;
    this.allowMaskMismatch = allowMaskMismatch;
  }
  
  protected FullHttpRequest newHandshakeRequest()
  {
    URI wsURL = uri();
    String path = wsURL.getPath();
    if ((wsURL.getQuery() != null) && (!wsURL.getQuery().isEmpty())) {
      path = wsURL.getPath() + '?' + wsURL.getQuery();
    }
    if ((path == null) || (path.isEmpty())) {
      path = "/";
    }
    byte[] nonce = WebSocketUtil.randomBytes(16);
    String key = WebSocketUtil.base64(nonce);
    
    String acceptSeed = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    byte[] sha1 = WebSocketUtil.sha1(acceptSeed.getBytes(CharsetUtil.US_ASCII));
    this.expectedChallengeResponseString = WebSocketUtil.base64(sha1);
    if (logger.isDebugEnabled()) {
      logger.debug("WebSocket version 07 client handshake key: {}, expected response: {}", key, this.expectedChallengeResponseString);
    }
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
    HttpHeaders headers = request.headers();
    
    headers.add(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET).add(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE).add(HttpHeaderNames.SEC_WEBSOCKET_KEY, key).add(HttpHeaderNames.HOST, wsURL.getHost());
    
    int wsPort = wsURL.getPort();
    String originValue = "http://" + wsURL.getHost();
    if ((wsPort != 80) && (wsPort != 443)) {
      originValue = originValue + ':' + wsPort;
    }
    headers.add(HttpHeaderNames.SEC_WEBSOCKET_ORIGIN, originValue);
    
    String expectedSubprotocol = expectedSubprotocol();
    if ((expectedSubprotocol != null) && (!expectedSubprotocol.isEmpty())) {
      headers.add(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, expectedSubprotocol);
    }
    headers.add(HttpHeaderNames.SEC_WEBSOCKET_VERSION, "7");
    if (this.customHeaders != null) {
      headers.add(this.customHeaders);
    }
    return request;
  }
  
  protected void verify(FullHttpResponse response)
  {
    HttpResponseStatus status = HttpResponseStatus.SWITCHING_PROTOCOLS;
    HttpHeaders headers = response.headers();
    if (!response.status().equals(status)) {
      throw new WebSocketHandshakeException("Invalid handshake response getStatus: " + response.status());
    }
    CharSequence upgrade = (CharSequence)headers.get(HttpHeaderNames.UPGRADE);
    if (!HttpHeaderValues.WEBSOCKET.equalsIgnoreCase(upgrade)) {
      throw new WebSocketHandshakeException("Invalid handshake response upgrade: " + upgrade);
    }
    CharSequence connection = (CharSequence)headers.get(HttpHeaderNames.CONNECTION);
    if (!HttpHeaderValues.UPGRADE.equalsIgnoreCase(connection)) {
      throw new WebSocketHandshakeException("Invalid handshake response connection: " + connection);
    }
    CharSequence accept = (CharSequence)headers.get(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT);
    if ((accept == null) || (!accept.equals(this.expectedChallengeResponseString))) {
      throw new WebSocketHandshakeException(String.format("Invalid challenge. Actual: %s. Expected: %s", new Object[] { accept, this.expectedChallengeResponseString }));
    }
  }
  
  protected WebSocketFrameDecoder newWebsocketDecoder()
  {
    return new WebSocket07FrameDecoder(false, this.allowExtensions, maxFramePayloadLength(), this.allowMaskMismatch);
  }
  
  protected WebSocketFrameEncoder newWebSocketEncoder()
  {
    return new WebSocket07FrameEncoder(this.performMasking);
  }
}
