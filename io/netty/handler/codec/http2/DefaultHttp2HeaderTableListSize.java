package io.netty.handler.codec.http2;

class DefaultHttp2HeaderTableListSize
{
  private int maxHeaderListSize = Integer.MAX_VALUE;
  
  public void maxHeaderListSize(int max)
    throws Http2Exception
  {
    if (max < 0) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Header List Size must be non-negative but was %d", new Object[] { Integer.valueOf(max) });
    }
    this.maxHeaderListSize = max;
  }
  
  public int maxHeaderListSize()
  {
    return this.maxHeaderListSize;
  }
}
