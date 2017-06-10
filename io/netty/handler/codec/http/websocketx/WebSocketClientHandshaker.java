package io.netty.handler.codec.http.websocketx;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.StringUtil;
import java.net.URI;
import java.nio.channels.ClosedChannelException;

public abstract class WebSocketClientHandshaker
{
  private static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();
  private final URI uri;
  private final WebSocketVersion version;
  private volatile boolean handshakeComplete;
  private final String expectedSubprotocol;
  private volatile String actualSubprotocol;
  protected final HttpHeaders customHeaders;
  private final int maxFramePayloadLength;
  
  static
  {
    CLOSED_CHANNEL_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
  }
  
  protected WebSocketClientHandshaker(URI uri, WebSocketVersion version, String subprotocol, HttpHeaders customHeaders, int maxFramePayloadLength)
  {
    this.uri = uri;
    this.version = version;
    this.expectedSubprotocol = subprotocol;
    this.customHeaders = customHeaders;
    this.maxFramePayloadLength = maxFramePayloadLength;
  }
  
  public URI uri()
  {
    return this.uri;
  }
  
  public WebSocketVersion version()
  {
    return this.version;
  }
  
  public int maxFramePayloadLength()
  {
    return this.maxFramePayloadLength;
  }
  
  public boolean isHandshakeComplete()
  {
    return this.handshakeComplete;
  }
  
  private void setHandshakeComplete()
  {
    this.handshakeComplete = true;
  }
  
  public String expectedSubprotocol()
  {
    return this.expectedSubprotocol;
  }
  
  public String actualSubprotocol()
  {
    return this.actualSubprotocol;
  }
  
  private void setActualSubprotocol(String actualSubprotocol)
  {
    this.actualSubprotocol = actualSubprotocol;
  }
  
  public ChannelFuture handshake(Channel channel)
  {
    if (channel == null) {
      throw new NullPointerException("channel");
    }
    return handshake(channel, channel.newPromise());
  }
  
  public final ChannelFuture handshake(Channel channel, final ChannelPromise promise)
  {
    FullHttpRequest request = newHandshakeRequest();
    
    HttpResponseDecoder decoder = (HttpResponseDecoder)channel.pipeline().get(HttpResponseDecoder.class);
    if (decoder == null)
    {
      HttpClientCodec codec = (HttpClientCodec)channel.pipeline().get(HttpClientCodec.class);
      if (codec == null)
      {
        promise.setFailure(new IllegalStateException("ChannelPipeline does not contain a HttpResponseDecoder or HttpClientCodec"));
        
        return promise;
      }
    }
    channel.writeAndFlush(request).addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future)
      {
        if (future.isSuccess())
        {
          ChannelPipeline p = future.channel().pipeline();
          ChannelHandlerContext ctx = p.context(HttpRequestEncoder.class);
          if (ctx == null) {
            ctx = p.context(HttpClientCodec.class);
          }
          if (ctx == null)
          {
            promise.setFailure(new IllegalStateException("ChannelPipeline does not contain a HttpRequestEncoder or HttpClientCodec"));
            
            return;
          }
          p.addAfter(ctx.name(), "ws-encoder", WebSocketClientHandshaker.this.newWebSocketEncoder());
          
          promise.setSuccess();
        }
        else
        {
          promise.setFailure(future.cause());
        }
      }
    });
    return promise;
  }
  
  public final void finishHandshake(Channel channel, FullHttpResponse response)
  {
    verify(response);
    
    String receivedProtocol = (String)response.headers().getAndConvert(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
    receivedProtocol = receivedProtocol != null ? receivedProtocol.trim() : null;
    String expectedProtocol = this.expectedSubprotocol != null ? this.expectedSubprotocol : "";
    boolean protocolValid = false;
    if ((expectedProtocol.isEmpty()) && (receivedProtocol == null))
    {
      protocolValid = true;
      setActualSubprotocol(this.expectedSubprotocol);
    }
    else if ((!expectedProtocol.isEmpty()) && (receivedProtocol != null) && (!receivedProtocol.isEmpty()))
    {
      for (String protocol : StringUtil.split(this.expectedSubprotocol, ',')) {
        if (protocol.trim().equals(receivedProtocol))
        {
          protocolValid = true;
          setActualSubprotocol(receivedProtocol);
          break;
        }
      }
    }
    if (!protocolValid) {
      throw new WebSocketHandshakeException(String.format("Invalid subprotocol. Actual: %s. Expected one of: %s", new Object[] { receivedProtocol, this.expectedSubprotocol }));
    }
    setHandshakeComplete();
    
    ChannelPipeline p = channel.pipeline();
    
    HttpContentDecompressor decompressor = (HttpContentDecompressor)p.get(HttpContentDecompressor.class);
    if (decompressor != null) {
      p.remove(decompressor);
    }
    HttpObjectAggregator aggregator = (HttpObjectAggregator)p.get(HttpObjectAggregator.class);
    if (aggregator != null) {
      p.remove(aggregator);
    }
    ChannelHandlerContext ctx = p.context(HttpResponseDecoder.class);
    if (ctx == null)
    {
      ctx = p.context(HttpClientCodec.class);
      if (ctx == null) {
        throw new IllegalStateException("ChannelPipeline does not contain a HttpRequestEncoder or HttpClientCodec");
      }
      p.replace(ctx.name(), "ws-decoder", newWebsocketDecoder());
    }
    else
    {
      if (p.get(HttpRequestEncoder.class) != null) {
        p.remove(HttpRequestEncoder.class);
      }
      p.replace(ctx.name(), "ws-decoder", newWebsocketDecoder());
    }
  }
  
  public final ChannelFuture processHandshake(Channel channel, HttpResponse response)
  {
    return processHandshake(channel, response, channel.newPromise());
  }
  
  public final ChannelFuture processHandshake(final Channel channel, HttpResponse response, final ChannelPromise promise)
  {
    if ((response instanceof FullHttpResponse))
    {
      try
      {
        finishHandshake(channel, (FullHttpResponse)response);
        promise.setSuccess();
      }
      catch (Throwable cause)
      {
        promise.setFailure(cause);
      }
    }
    else
    {
      ChannelPipeline p = channel.pipeline();
      ChannelHandlerContext ctx = p.context(HttpResponseDecoder.class);
      if (ctx == null)
      {
        ctx = p.context(HttpClientCodec.class);
        if (ctx == null) {
          return promise.setFailure(new IllegalStateException("ChannelPipeline does not contain a HttpResponseDecoder or HttpClientCodec"));
        }
      }
      String aggregatorName = "httpAggregator";
      p.addAfter(ctx.name(), aggregatorName, new HttpObjectAggregator(8192));
      p.addAfter(aggregatorName, "handshaker", new SimpleChannelInboundHandler()
      {
        protected void messageReceived(ChannelHandlerContext ctx, FullHttpResponse msg)
          throws Exception
        {
          ctx.pipeline().remove(this);
          try
          {
            WebSocketClientHandshaker.this.finishHandshake(channel, msg);
            promise.setSuccess();
          }
          catch (Throwable cause)
          {
            promise.setFailure(cause);
          }
        }
        
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
          throws Exception
        {
          ctx.pipeline().remove(this);
          promise.setFailure(cause);
        }
        
        public void channelInactive(ChannelHandlerContext ctx)
          throws Exception
        {
          promise.tryFailure(WebSocketClientHandshaker.CLOSED_CHANNEL_EXCEPTION);
          ctx.fireChannelInactive();
        }
      });
      try
      {
        ctx.fireChannelRead(ReferenceCountUtil.retain(response));
      }
      catch (Throwable cause)
      {
        promise.setFailure(cause);
      }
    }
    return promise;
  }
  
  public ChannelFuture close(Channel channel, CloseWebSocketFrame frame)
  {
    if (channel == null) {
      throw new NullPointerException("channel");
    }
    return close(channel, frame, channel.newPromise());
  }
  
  public ChannelFuture close(Channel channel, CloseWebSocketFrame frame, ChannelPromise promise)
  {
    if (channel == null) {
      throw new NullPointerException("channel");
    }
    return channel.writeAndFlush(frame, promise);
  }
  
  protected abstract FullHttpRequest newHandshakeRequest();
  
  protected abstract void verify(FullHttpResponse paramFullHttpResponse);
  
  protected abstract WebSocketFrameDecoder newWebsocketDecoder();
  
  protected abstract WebSocketFrameEncoder newWebSocketEncoder();
}
