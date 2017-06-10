package io.netty.handler.codec.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

public class HttpContentDecompressor
  extends HttpContentDecoder
{
  private final boolean strict;
  
  public HttpContentDecompressor()
  {
    this(false);
  }
  
  public HttpContentDecompressor(boolean strict)
  {
    this.strict = strict;
  }
  
  protected EmbeddedChannel newContentDecoder(String contentEncoding)
    throws Exception
  {
    if ((HttpHeaderValues.GZIP.equalsIgnoreCase(contentEncoding)) || (HttpHeaderValues.X_GZIP.equalsIgnoreCase(contentEncoding))) {
      return new EmbeddedChannel(new ChannelHandler[] { ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP) });
    }
    if ((HttpHeaderValues.DEFLATE.equalsIgnoreCase(contentEncoding)) || (HttpHeaderValues.X_DEFLATE.equalsIgnoreCase(contentEncoding)))
    {
      ZlibWrapper wrapper = this.strict ? ZlibWrapper.ZLIB : ZlibWrapper.ZLIB_OR_NONE;
      
      return new EmbeddedChannel(new ChannelHandler[] { ZlibCodecFactory.newZlibDecoder(wrapper) });
    }
    return null;
  }
}
