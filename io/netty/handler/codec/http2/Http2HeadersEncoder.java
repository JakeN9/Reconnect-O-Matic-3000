package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;

public abstract interface Http2HeadersEncoder
{
  public abstract void encodeHeaders(Http2Headers paramHttp2Headers, ByteBuf paramByteBuf)
    throws Http2Exception;
  
  public abstract Configuration configuration();
  
  public static abstract interface Configuration
  {
    public abstract Http2HeaderTable headerTable();
  }
}
