package io.netty.handler.codec.http2;

import com.twitter.hpack.Decoder;
import com.twitter.hpack.HeaderListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.AsciiString;
import java.io.IOException;
import java.io.InputStream;

public class DefaultHttp2HeadersDecoder
  implements Http2HeadersDecoder, Http2HeadersDecoder.Configuration
{
  private final Decoder decoder;
  private final Http2HeaderTable headerTable;
  
  public DefaultHttp2HeadersDecoder()
  {
    this(8192, 4096);
  }
  
  public DefaultHttp2HeadersDecoder(int maxHeaderSize, int maxHeaderTableSize)
  {
    this.decoder = new Decoder(maxHeaderSize, maxHeaderTableSize);
    this.headerTable = new Http2HeaderTableDecoder(null);
  }
  
  public Http2HeaderTable headerTable()
  {
    return this.headerTable;
  }
  
  public Http2HeadersDecoder.Configuration configuration()
  {
    return this;
  }
  
  public Http2Headers decodeHeaders(ByteBuf headerBlock)
    throws Http2Exception
  {
    InputStream in = new ByteBufInputStream(headerBlock);
    try
    {
      final Http2Headers headers = new DefaultHttp2Headers();
      HeaderListener listener = new HeaderListener()
      {
        public void addHeader(byte[] key, byte[] value, boolean sensitive)
        {
          headers.add(new AsciiString(key, false), new AsciiString(value, false));
        }
      };
      this.decoder.decode(in, listener);
      boolean truncated = this.decoder.endHeaderBlock();
      if ((!truncated) || 
      
        (headers.size() > this.headerTable.maxHeaderListSize())) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Number of headers (%d) exceeds maxHeaderListSize (%d)", new Object[] { Integer.valueOf(headers.size()), Integer.valueOf(this.headerTable.maxHeaderListSize()) });
      }
      return headers;
    }
    catch (IOException e)
    {
      throw Http2Exception.connectionError(Http2Error.COMPRESSION_ERROR, e, e.getMessage(), new Object[0]);
    }
    catch (Http2Exception e)
    {
      throw e;
    }
    catch (Throwable e)
    {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, e, e.getMessage(), new Object[0]);
    }
    finally
    {
      try
      {
        in.close();
      }
      catch (IOException e)
      {
        throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, e, e.getMessage(), new Object[0]);
      }
    }
  }
  
  private final class Http2HeaderTableDecoder
    extends DefaultHttp2HeaderTableListSize
    implements Http2HeaderTable
  {
    private Http2HeaderTableDecoder() {}
    
    public void maxHeaderTableSize(int max)
      throws Http2Exception
    {
      if (max < 0) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Header Table Size must be non-negative but was %d", new Object[] { Integer.valueOf(max) });
      }
      try
      {
        DefaultHttp2HeadersDecoder.this.decoder.setMaxHeaderTableSize(max);
      }
      catch (Throwable t)
      {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, t.getMessage(), new Object[] { t });
      }
    }
    
    public int maxHeaderTableSize()
    {
      return DefaultHttp2HeadersDecoder.this.decoder.getMaxHeaderTableSize();
    }
  }
}
