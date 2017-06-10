package io.netty.handler.codec.spdy;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAppender;

public final class SpdyHttpCodec
  extends ChannelHandlerAppender
{
  public SpdyHttpCodec(SpdyVersion version, int maxContentLength)
  {
    super(new ChannelHandler[] { new SpdyHttpDecoder(version, maxContentLength), new SpdyHttpEncoder(version) });
  }
  
  public SpdyHttpCodec(SpdyVersion version, int maxContentLength, boolean validateHttpHeaders)
  {
    super(new ChannelHandler[] { new SpdyHttpDecoder(version, maxContentLength, validateHttpHeaders), new SpdyHttpEncoder(version) });
  }
}
