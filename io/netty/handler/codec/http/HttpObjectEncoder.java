package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.util.List;

public abstract class HttpObjectEncoder<H extends HttpMessage>
  extends MessageToMessageEncoder<Object>
{
  private static final byte[] CRLF = { 13, 10 };
  private static final byte[] ZERO_CRLF = { 48, 13, 10 };
  private static final byte[] ZERO_CRLF_CRLF = { 48, 13, 10, 13, 10 };
  private static final ByteBuf CRLF_BUF = Unpooled.unreleasableBuffer(Unpooled.directBuffer(CRLF.length).writeBytes(CRLF));
  private static final ByteBuf ZERO_CRLF_CRLF_BUF = Unpooled.unreleasableBuffer(Unpooled.directBuffer(ZERO_CRLF_CRLF.length).writeBytes(ZERO_CRLF_CRLF));
  private static final int ST_INIT = 0;
  private static final int ST_CONTENT_NON_CHUNK = 1;
  private static final int ST_CONTENT_CHUNK = 2;
  private int state = 0;
  
  protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out)
    throws Exception
  {
    ByteBuf buf = null;
    if ((msg instanceof HttpMessage))
    {
      if (this.state != 0) {
        throw new IllegalStateException("unexpected message type: " + StringUtil.simpleClassName(msg));
      }
      H m = (HttpMessage)msg;
      
      buf = ctx.alloc().buffer();
      
      encodeInitialLine(buf, m);
      encodeHeaders(m.headers(), buf);
      buf.writeBytes(CRLF);
      this.state = (HttpHeaderUtil.isTransferEncodingChunked(m) ? 2 : 1);
    }
    if (((msg instanceof ByteBuf)) && (!((ByteBuf)msg).isReadable()))
    {
      out.add(Unpooled.EMPTY_BUFFER);
      return;
    }
    if (((msg instanceof HttpContent)) || ((msg instanceof ByteBuf)) || ((msg instanceof FileRegion)))
    {
      if (this.state == 0) {
        throw new IllegalStateException("unexpected message type: " + StringUtil.simpleClassName(msg));
      }
      long contentLength = contentLength(msg);
      if (this.state == 1)
      {
        if (contentLength > 0L)
        {
          if ((buf != null) && (buf.writableBytes() >= contentLength) && ((msg instanceof HttpContent)))
          {
            buf.writeBytes(((HttpContent)msg).content());
            out.add(buf);
          }
          else
          {
            if (buf != null) {
              out.add(buf);
            }
            out.add(encodeAndRetain(msg));
          }
        }
        else if (buf != null) {
          out.add(buf);
        } else {
          out.add(Unpooled.EMPTY_BUFFER);
        }
        if ((msg instanceof LastHttpContent)) {
          this.state = 0;
        }
      }
      else if (this.state == 2)
      {
        if (buf != null) {
          out.add(buf);
        }
        encodeChunkedContent(ctx, msg, contentLength, out);
      }
      else
      {
        throw new Error();
      }
    }
    else if (buf != null)
    {
      out.add(buf);
    }
  }
  
  protected void encodeHeaders(HttpHeaders headers, ByteBuf buf)
    throws Exception
  {
    headers.forEachEntry(new HttpHeadersEncoder(buf));
  }
  
  private void encodeChunkedContent(ChannelHandlerContext ctx, Object msg, long contentLength, List<Object> out)
  {
    if (contentLength > 0L)
    {
      byte[] length = Long.toHexString(contentLength).getBytes(CharsetUtil.US_ASCII);
      ByteBuf buf = ctx.alloc().buffer(length.length + 2);
      buf.writeBytes(length);
      buf.writeBytes(CRLF);
      out.add(buf);
      out.add(encodeAndRetain(msg));
      out.add(CRLF_BUF.duplicate());
    }
    if ((msg instanceof LastHttpContent))
    {
      HttpHeaders headers = ((LastHttpContent)msg).trailingHeaders();
      if (headers.isEmpty())
      {
        out.add(ZERO_CRLF_CRLF_BUF.duplicate());
      }
      else
      {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes(ZERO_CRLF);
        try
        {
          encodeHeaders(headers, buf);
        }
        catch (Exception ex)
        {
          buf.release();
          PlatformDependent.throwException(ex);
        }
        buf.writeBytes(CRLF);
        out.add(buf);
      }
      this.state = 0;
    }
    else if (contentLength == 0L)
    {
      out.add(Unpooled.EMPTY_BUFFER);
    }
  }
  
  public boolean acceptOutboundMessage(Object msg)
    throws Exception
  {
    return ((msg instanceof HttpObject)) || ((msg instanceof ByteBuf)) || ((msg instanceof FileRegion));
  }
  
  private static Object encodeAndRetain(Object msg)
  {
    if ((msg instanceof ByteBuf)) {
      return ((ByteBuf)msg).retain();
    }
    if ((msg instanceof HttpContent)) {
      return ((HttpContent)msg).content().retain();
    }
    if ((msg instanceof FileRegion)) {
      return ((FileRegion)msg).retain();
    }
    throw new IllegalStateException("unexpected message type: " + StringUtil.simpleClassName(msg));
  }
  
  private static long contentLength(Object msg)
  {
    if ((msg instanceof HttpContent)) {
      return ((HttpContent)msg).content().readableBytes();
    }
    if ((msg instanceof ByteBuf)) {
      return ((ByteBuf)msg).readableBytes();
    }
    if ((msg instanceof FileRegion)) {
      return ((FileRegion)msg).count();
    }
    throw new IllegalStateException("unexpected message type: " + StringUtil.simpleClassName(msg));
  }
  
  protected abstract void encodeInitialLine(ByteBuf paramByteBuf, H paramH)
    throws Exception;
}
