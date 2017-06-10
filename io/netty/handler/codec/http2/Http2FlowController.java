package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandlerContext;

public abstract interface Http2FlowController
{
  public abstract void initialWindowSize(int paramInt)
    throws Http2Exception;
  
  public abstract int initialWindowSize();
  
  public abstract int windowSize(Http2Stream paramHttp2Stream);
  
  public abstract void incrementWindowSize(ChannelHandlerContext paramChannelHandlerContext, Http2Stream paramHttp2Stream, int paramInt)
    throws Http2Exception;
}
