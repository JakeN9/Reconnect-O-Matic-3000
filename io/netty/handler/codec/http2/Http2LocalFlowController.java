package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public abstract interface Http2LocalFlowController
  extends Http2FlowController
{
  public abstract void receiveFlowControlledFrame(ChannelHandlerContext paramChannelHandlerContext, Http2Stream paramHttp2Stream, ByteBuf paramByteBuf, int paramInt, boolean paramBoolean)
    throws Http2Exception;
  
  public abstract void consumeBytes(ChannelHandlerContext paramChannelHandlerContext, Http2Stream paramHttp2Stream, int paramInt)
    throws Http2Exception;
  
  public abstract int unconsumedBytes(Http2Stream paramHttp2Stream);
}
