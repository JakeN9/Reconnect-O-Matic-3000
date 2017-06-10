package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.util.List;

public class WebSocketServerProtocolHandler
  extends WebSocketProtocolHandler
{
  public static enum ServerHandshakeStateEvent
  {
    HANDSHAKE_COMPLETE;
    
    private ServerHandshakeStateEvent() {}
  }
  
  private static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER_ATTR_KEY = AttributeKey.valueOf(WebSocketServerHandshaker.class, "HANDSHAKER");
  private final String websocketPath;
  private final String subprotocols;
  private final boolean allowExtensions;
  private final int maxFramePayloadLength;
  private final boolean allowMaskMismatch;
  
  public WebSocketServerProtocolHandler(String websocketPath)
  {
    this(websocketPath, null, false);
  }
  
  public WebSocketServerProtocolHandler(String websocketPath, String subprotocols)
  {
    this(websocketPath, subprotocols, false);
  }
  
  public WebSocketServerProtocolHandler(String websocketPath, String subprotocols, boolean allowExtensions)
  {
    this(websocketPath, subprotocols, allowExtensions, 65536);
  }
  
  public WebSocketServerProtocolHandler(String websocketPath, String subprotocols, boolean allowExtensions, int maxFrameSize)
  {
    this(websocketPath, subprotocols, allowExtensions, maxFrameSize, false);
  }
  
  public WebSocketServerProtocolHandler(String websocketPath, String subprotocols, boolean allowExtensions, int maxFrameSize, boolean allowMaskMismatch)
  {
    this.websocketPath = websocketPath;
    this.subprotocols = subprotocols;
    this.allowExtensions = allowExtensions;
    this.maxFramePayloadLength = maxFrameSize;
    this.allowMaskMismatch = allowMaskMismatch;
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
  {
    ChannelPipeline cp = ctx.pipeline();
    if (cp.get(WebSocketServerProtocolHandshakeHandler.class) == null) {
      ctx.pipeline().addBefore(ctx.name(), WebSocketServerProtocolHandshakeHandler.class.getName(), new WebSocketServerProtocolHandshakeHandler(this.websocketPath, this.subprotocols, this.allowExtensions, this.maxFramePayloadLength, this.allowMaskMismatch));
    }
    if (cp.get(Utf8FrameValidator.class) == null) {
      ctx.pipeline().addBefore(ctx.name(), Utf8FrameValidator.class.getName(), new Utf8FrameValidator());
    }
  }
  
  protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out)
    throws Exception
  {
    if ((frame instanceof CloseWebSocketFrame))
    {
      WebSocketServerHandshaker handshaker = getHandshaker(ctx);
      if (handshaker != null)
      {
        frame.retain();
        handshaker.close(ctx.channel(), (CloseWebSocketFrame)frame);
      }
      else
      {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
      }
      return;
    }
    super.decode(ctx, frame, out);
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    if ((cause instanceof WebSocketHandshakeException))
    {
      FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, Unpooled.wrappedBuffer(cause.getMessage().getBytes()));
      
      ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    else
    {
      ctx.close();
    }
  }
  
  static WebSocketServerHandshaker getHandshaker(ChannelHandlerContext ctx)
  {
    return (WebSocketServerHandshaker)ctx.attr(HANDSHAKER_ATTR_KEY).get();
  }
  
  static void setHandshaker(ChannelHandlerContext ctx, WebSocketServerHandshaker handshaker)
  {
    ctx.attr(HANDSHAKER_ATTR_KEY).set(handshaker);
  }
  
  static ChannelHandler forbiddenHttpRequestResponder()
  {
    new ChannelHandlerAdapter()
    {
      public void channelRead(ChannelHandlerContext ctx, Object msg)
        throws Exception
      {
        if ((msg instanceof FullHttpRequest))
        {
          FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
          
          ctx.channel().writeAndFlush(response);
        }
        else
        {
          ctx.fireChannelRead(msg);
        }
      }
    };
  }
}
