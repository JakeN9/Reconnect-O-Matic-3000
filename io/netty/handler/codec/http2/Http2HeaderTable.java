package io.netty.handler.codec.http2;

public abstract interface Http2HeaderTable
{
  public abstract void maxHeaderTableSize(int paramInt)
    throws Http2Exception;
  
  public abstract int maxHeaderTableSize();
  
  public abstract void maxHeaderListSize(int paramInt)
    throws Http2Exception;
  
  public abstract int maxHeaderListSize();
}
