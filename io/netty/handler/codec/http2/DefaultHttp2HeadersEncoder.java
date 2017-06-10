package io.netty.handler.codec.http2;

import com.twitter.hpack.Encoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.BinaryHeaders.EntryVisitor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class DefaultHttp2HeadersEncoder
  implements Http2HeadersEncoder, Http2HeadersEncoder.Configuration
{
  private final Encoder encoder;
  private final ByteArrayOutputStream tableSizeChangeOutput = new ByteArrayOutputStream();
  private final Set<String> sensitiveHeaders = new TreeSet(String.CASE_INSENSITIVE_ORDER);
  private final Http2HeaderTable headerTable;
  
  public DefaultHttp2HeadersEncoder()
  {
    this(4096, Collections.emptySet());
  }
  
  public DefaultHttp2HeadersEncoder(int maxHeaderTableSize, Set<String> sensitiveHeaders)
  {
    this.encoder = new Encoder(maxHeaderTableSize);
    this.sensitiveHeaders.addAll(sensitiveHeaders);
    this.headerTable = new Http2HeaderTableEncoder(null);
  }
  
  public void encodeHeaders(Http2Headers headers, ByteBuf buffer)
    throws Http2Exception
  {
    final OutputStream stream = new ByteBufOutputStream(buffer);
    try
    {
      if (headers.size() > this.headerTable.maxHeaderListSize()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Number of headers (%d) exceeds maxHeaderListSize (%d)", new Object[] { Integer.valueOf(headers.size()), Integer.valueOf(this.headerTable.maxHeaderListSize()) });
      }
      if (this.tableSizeChangeOutput.size() > 0)
      {
        buffer.writeBytes(this.tableSizeChangeOutput.toByteArray());
        this.tableSizeChangeOutput.reset();
      }
      for (Http2Headers.PseudoHeaderName pseudoHeader : Http2Headers.PseudoHeaderName.values())
      {
        AsciiString name = pseudoHeader.value();
        AsciiString value = (AsciiString)headers.get(name);
        if (value != null) {
          encodeHeader(name, value, stream);
        }
      }
      headers.forEachEntry(new BinaryHeaders.EntryVisitor()
      {
        public boolean visit(Map.Entry<AsciiString, AsciiString> entry)
          throws Exception
        {
          AsciiString name = (AsciiString)entry.getKey();
          AsciiString value = (AsciiString)entry.getValue();
          if (!Http2Headers.PseudoHeaderName.isPseudoHeader(name)) {
            DefaultHttp2HeadersEncoder.this.encodeHeader(name, value, stream);
          }
          return true;
        }
      }); return;
    }
    catch (Http2Exception e)
    {
      throw e;
    }
    catch (Throwable t)
    {
      throw Http2Exception.connectionError(Http2Error.COMPRESSION_ERROR, t, "Failed encoding headers block: %s", new Object[] { t.getMessage() });
    }
    finally
    {
      try
      {
        stream.close();
      }
      catch (IOException e)
      {
        throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, e, e.getMessage(), new Object[0]);
      }
    }
  }
  
  public Http2HeaderTable headerTable()
  {
    return this.headerTable;
  }
  
  public Http2HeadersEncoder.Configuration configuration()
  {
    return this;
  }
  
  private void encodeHeader(AsciiString key, AsciiString value, OutputStream stream)
    throws IOException
  {
    boolean sensitive = this.sensitiveHeaders.contains(key.toString());
    this.encoder.encodeHeader(stream, key.array(), value.array(), sensitive);
  }
  
  private final class Http2HeaderTableEncoder
    extends DefaultHttp2HeaderTableListSize
    implements Http2HeaderTable
  {
    private Http2HeaderTableEncoder() {}
    
    public void maxHeaderTableSize(int max)
      throws Http2Exception
    {
      if (max < 0) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Header Table Size must be non-negative but was %d", new Object[] { Integer.valueOf(max) });
      }
      try
      {
        DefaultHttp2HeadersEncoder.this.encoder.setMaxHeaderTableSize(DefaultHttp2HeadersEncoder.this.tableSizeChangeOutput, max);
      }
      catch (IOException e)
      {
        throw new Http2Exception(Http2Error.COMPRESSION_ERROR, e.getMessage(), e);
      }
      catch (Throwable t)
      {
        throw new Http2Exception(Http2Error.PROTOCOL_ERROR, t.getMessage(), t);
      }
    }
    
    public int maxHeaderTableSize()
    {
      return DefaultHttp2HeadersEncoder.this.encoder.getMaxHeaderTableSize();
    }
  }
}
