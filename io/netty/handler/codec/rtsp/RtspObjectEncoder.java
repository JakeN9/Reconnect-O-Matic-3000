package io.netty.handler.codec.rtsp;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectEncoder;

@ChannelHandler.Sharable
public abstract class RtspObjectEncoder<H extends HttpMessage>
  extends HttpObjectEncoder<H>
{
  public boolean acceptOutboundMessage(Object msg)
    throws Exception
  {
    return msg instanceof FullHttpMessage;
  }
}
