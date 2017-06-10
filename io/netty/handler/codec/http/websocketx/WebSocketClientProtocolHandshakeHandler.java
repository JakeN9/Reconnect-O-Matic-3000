package io.netty.handler.codec.http.websocketx;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpResponse;

class WebSocketClientProtocolHandshakeHandler
  extends ChannelHandlerAdapter
{
  private final WebSocketClientHandshaker handshaker;
  
  WebSocketClientProtocolHandshakeHandler(WebSocketClientHandshaker handshaker)
  {
    this.handshaker = handshaker;
  }
  
  public void channelActive(final ChannelHandlerContext ctx)
    throws Exception
  {
    super.channelActive(ctx);
    this.handshaker.handshake(ctx.channel()).addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future)
        throws Exception
      {
        if (!future.isSuccess()) {
          ctx.fireExceptionCaught(future.cause());
        } else {
          ctx.fireUserEventTriggered(WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_ISSUED);
        }
      }
    });
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    if (!(msg instanceof FullHttpResponse))
    {
      ctx.fireChannelRead(msg);
      return;
    }
    FullHttpResponse response = (FullHttpResponse)msg;
    try
    {
      if (!this.handshaker.isHandshakeComplete())
      {
        this.handshaker.finishHandshake(ctx.channel(), response);
        ctx.fireUserEventTriggered(WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE);
        
        ctx.pipeline().remove(this);
      }
      else
      {
        throw new IllegalStateException("WebSocketClientHandshaker should have been non finished yet");
      }
    }
    finally
    {
      response.release();
    }
  }
}
