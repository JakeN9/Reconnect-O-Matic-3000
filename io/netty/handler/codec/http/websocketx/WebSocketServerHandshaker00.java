package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.logging.InternalLogger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketServerHandshaker00
  extends WebSocketServerHandshaker
{
  private static final AsciiString WEBSOCKET = new AsciiString("WebSocket");
  private static final Pattern BEGINNING_DIGIT = Pattern.compile("[^0-9]");
  private static final Pattern BEGINNING_SPACE = Pattern.compile("[^ ]");
  
  public WebSocketServerHandshaker00(String webSocketURL, String subprotocols, int maxFramePayloadLength)
  {
    super(WebSocketVersion.V00, webSocketURL, subprotocols, maxFramePayloadLength);
  }
  
  protected FullHttpResponse newHandshakeResponse(FullHttpRequest req, HttpHeaders headers)
  {
    if ((!HttpHeaderValues.UPGRADE.equalsIgnoreCase((CharSequence)req.headers().get(HttpHeaderNames.CONNECTION))) || (!WEBSOCKET.equalsIgnoreCase((CharSequence)req.headers().get(HttpHeaderNames.UPGRADE)))) {
      throw new WebSocketHandshakeException("not a WebSocket handshake request: missing upgrade");
    }
    boolean isHixie76 = (req.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_KEY1)) && (req.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_KEY2));
    
    FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(101, isHixie76 ? "WebSocket Protocol Handshake" : "Web Socket Protocol Handshake"));
    if (headers != null) {
      res.headers().add(headers);
    }
    res.headers().add(HttpHeaderNames.UPGRADE, WEBSOCKET);
    res.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
    if (isHixie76)
    {
      res.headers().add(HttpHeaderNames.SEC_WEBSOCKET_ORIGIN, (CharSequence)req.headers().get(HttpHeaderNames.ORIGIN));
      res.headers().add(HttpHeaderNames.SEC_WEBSOCKET_LOCATION, uri());
      String subprotocols = (String)req.headers().getAndConvert(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
      if (subprotocols != null)
      {
        String selectedSubprotocol = selectSubprotocol(subprotocols);
        if (selectedSubprotocol == null)
        {
          if (logger.isDebugEnabled()) {
            logger.debug("Requested subprotocol(s) not supported: {}", subprotocols);
          }
        }
        else {
          res.headers().add(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, selectedSubprotocol);
        }
      }
      String key1 = (String)req.headers().getAndConvert(HttpHeaderNames.SEC_WEBSOCKET_KEY1);
      String key2 = (String)req.headers().getAndConvert(HttpHeaderNames.SEC_WEBSOCKET_KEY2);
      int a = (int)(Long.parseLong(BEGINNING_DIGIT.matcher(key1).replaceAll("")) / BEGINNING_SPACE.matcher(key1).replaceAll("").length());
      
      int b = (int)(Long.parseLong(BEGINNING_DIGIT.matcher(key2).replaceAll("")) / BEGINNING_SPACE.matcher(key2).replaceAll("").length());
      
      long c = req.content().readLong();
      ByteBuf input = Unpooled.buffer(16);
      input.writeInt(a);
      input.writeInt(b);
      input.writeLong(c);
      res.content().writeBytes(WebSocketUtil.md5(input.array()));
    }
    else
    {
      res.headers().add(HttpHeaderNames.WEBSOCKET_ORIGIN, (CharSequence)req.headers().get(HttpHeaderNames.ORIGIN));
      res.headers().add(HttpHeaderNames.WEBSOCKET_LOCATION, uri());
      String protocol = (String)req.headers().getAndConvert(HttpHeaderNames.WEBSOCKET_PROTOCOL);
      if (protocol != null) {
        res.headers().add(HttpHeaderNames.WEBSOCKET_PROTOCOL, selectSubprotocol(protocol));
      }
    }
    return res;
  }
  
  public ChannelFuture close(Channel channel, CloseWebSocketFrame frame, ChannelPromise promise)
  {
    return channel.writeAndFlush(frame, promise);
  }
  
  protected WebSocketFrameDecoder newWebsocketDecoder()
  {
    return new WebSocket00FrameDecoder(maxFramePayloadLength());
  }
  
  protected WebSocketFrameEncoder newWebSocketEncoder()
  {
    return new WebSocket00FrameEncoder();
  }
}
