package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import java.util.List;

public abstract class HttpContentDecoder
  extends MessageToMessageDecoder<HttpObject>
{
  private static final String IDENTITY = HttpHeaderValues.IDENTITY.toString();
  private EmbeddedChannel decoder;
  private boolean continueResponse;
  
  protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
    throws Exception
  {
    if (((msg instanceof HttpResponse)) && (((HttpResponse)msg).status().code() == 100))
    {
      if (!(msg instanceof LastHttpContent)) {
        this.continueResponse = true;
      }
      out.add(ReferenceCountUtil.retain(msg));
      return;
    }
    if (this.continueResponse)
    {
      if ((msg instanceof LastHttpContent)) {
        this.continueResponse = false;
      }
      out.add(ReferenceCountUtil.retain(msg));
      return;
    }
    if ((msg instanceof HttpMessage))
    {
      cleanup();
      HttpMessage message = (HttpMessage)msg;
      HttpHeaders headers = message.headers();
      
      String contentEncoding = (String)headers.getAndConvert(HttpHeaderNames.CONTENT_ENCODING);
      if (contentEncoding != null) {
        contentEncoding = contentEncoding.trim();
      } else {
        contentEncoding = IDENTITY;
      }
      this.decoder = newContentDecoder(contentEncoding);
      if (this.decoder == null)
      {
        if ((message instanceof HttpContent)) {
          ((HttpContent)message).retain();
        }
        out.add(message);
        return;
      }
      headers.remove(HttpHeaderNames.CONTENT_LENGTH);
      
      CharSequence targetContentEncoding = getTargetContentEncoding(contentEncoding);
      if (HttpHeaderValues.IDENTITY.equals(targetContentEncoding)) {
        headers.remove(HttpHeaderNames.CONTENT_ENCODING);
      } else {
        headers.set(HttpHeaderNames.CONTENT_ENCODING, targetContentEncoding);
      }
      if ((message instanceof HttpContent))
      {
        HttpMessage copy;
        if ((message instanceof HttpRequest))
        {
          HttpRequest r = (HttpRequest)message;
          copy = new DefaultHttpRequest(r.protocolVersion(), r.method(), r.uri());
        }
        else
        {
          HttpMessage copy;
          if ((message instanceof HttpResponse))
          {
            HttpResponse r = (HttpResponse)message;
            copy = new DefaultHttpResponse(r.protocolVersion(), r.status());
          }
          else
          {
            throw new CodecException("Object of class " + message.getClass().getName() + " is not a HttpRequest or HttpResponse");
          }
        }
        HttpMessage copy;
        copy.headers().set(message.headers());
        copy.setDecoderResult(message.decoderResult());
        out.add(copy);
      }
      else
      {
        out.add(message);
      }
    }
    if ((msg instanceof HttpContent))
    {
      HttpContent c = (HttpContent)msg;
      if (this.decoder == null) {
        out.add(c.retain());
      } else {
        decodeContent(c, out);
      }
    }
  }
  
  private void decodeContent(HttpContent c, List<Object> out)
  {
    ByteBuf content = c.content();
    
    decode(content, out);
    if ((c instanceof LastHttpContent))
    {
      finishDecode(out);
      
      LastHttpContent last = (LastHttpContent)c;
      
      HttpHeaders headers = last.trailingHeaders();
      if (headers.isEmpty()) {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
      } else {
        out.add(new ComposedLastHttpContent(headers));
      }
    }
  }
  
  protected abstract EmbeddedChannel newContentDecoder(String paramString)
    throws Exception;
  
  protected CharSequence getTargetContentEncoding(String contentEncoding)
    throws Exception
  {
    return HttpHeaderValues.IDENTITY;
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {
    cleanup();
    super.handlerRemoved(ctx);
  }
  
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    cleanup();
    super.channelInactive(ctx);
  }
  
  private void cleanup()
  {
    if (this.decoder != null)
    {
      if (this.decoder.finish()) {
        for (;;)
        {
          ByteBuf buf = (ByteBuf)this.decoder.readInbound();
          if (buf == null) {
            break;
          }
          buf.release();
        }
      }
      this.decoder = null;
    }
  }
  
  private void decode(ByteBuf in, List<Object> out)
  {
    this.decoder.writeInbound(new Object[] { in.retain() });
    fetchDecoderOutput(out);
  }
  
  private void finishDecode(List<Object> out)
  {
    if (this.decoder.finish()) {
      fetchDecoderOutput(out);
    }
    this.decoder = null;
  }
  
  private void fetchDecoderOutput(List<Object> out)
  {
    for (;;)
    {
      ByteBuf buf = (ByteBuf)this.decoder.readInbound();
      if (buf == null) {
        break;
      }
      if (!buf.isReadable()) {
        buf.release();
      } else {
        out.add(new DefaultHttpContent(buf));
      }
    }
  }
}
