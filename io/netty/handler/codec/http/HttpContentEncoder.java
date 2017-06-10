package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public abstract class HttpContentEncoder
  extends MessageToMessageCodec<HttpRequest, HttpObject>
{
  private final Queue<CharSequence> acceptEncodingQueue;
  private CharSequence acceptEncoding;
  private EmbeddedChannel encoder;
  private State state;
  
  private static enum State
  {
    PASS_THROUGH,  AWAIT_HEADERS,  AWAIT_CONTENT;
    
    private State() {}
  }
  
  public HttpContentEncoder()
  {
    this.acceptEncodingQueue = new ArrayDeque();
    
    this.state = State.AWAIT_HEADERS;
  }
  
  public boolean acceptOutboundMessage(Object msg)
    throws Exception
  {
    return ((msg instanceof HttpContent)) || ((msg instanceof HttpResponse));
  }
  
  protected void decode(ChannelHandlerContext ctx, HttpRequest msg, List<Object> out)
    throws Exception
  {
    CharSequence acceptedEncoding = (CharSequence)msg.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
    if (acceptedEncoding == null) {
      acceptedEncoding = HttpHeaderValues.IDENTITY;
    }
    this.acceptEncodingQueue.add(acceptedEncoding);
    out.add(ReferenceCountUtil.retain(msg));
  }
  
  protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
    throws Exception
  {
    boolean isFull = ((msg instanceof HttpResponse)) && ((msg instanceof LastHttpContent));
    switch (this.state)
    {
    case AWAIT_HEADERS: 
      ensureHeaders(msg);
      assert (this.encoder == null);
      
      HttpResponse res = (HttpResponse)msg;
      if (isPassthru(res))
      {
        if (isFull)
        {
          out.add(ReferenceCountUtil.retain(res));
        }
        else
        {
          out.add(res);
          
          this.state = State.PASS_THROUGH;
        }
      }
      else
      {
        this.acceptEncoding = ((CharSequence)this.acceptEncodingQueue.poll());
        if (this.acceptEncoding == null) {
          throw new IllegalStateException("cannot send more responses than requests");
        }
        if (isFull) {
          if (!((ByteBufHolder)res).content().isReadable())
          {
            out.add(ReferenceCountUtil.retain(res));
            return;
          }
        }
        Result result = beginEncode(res, this.acceptEncoding);
        if (result == null)
        {
          if (isFull)
          {
            out.add(ReferenceCountUtil.retain(res));
          }
          else
          {
            out.add(res);
            
            this.state = State.PASS_THROUGH;
          }
        }
        else
        {
          this.encoder = result.contentEncoder();
          
          res.headers().set(HttpHeaderNames.CONTENT_ENCODING, result.targetContentEncoding());
          
          res.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
          res.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
          if (isFull)
          {
            HttpResponse newRes = new DefaultHttpResponse(res.protocolVersion(), res.status());
            newRes.headers().set(res.headers());
            out.add(newRes);
          }
          else
          {
            out.add(res);
            this.state = State.AWAIT_CONTENT;
            if (!(msg instanceof HttpContent)) {
              return;
            }
          }
        }
      }
      break;
    case AWAIT_CONTENT: 
      ensureContent(msg);
      if (encodeContent((HttpContent)msg, out)) {
        this.state = State.AWAIT_HEADERS;
      }
      break;
    case PASS_THROUGH: 
      ensureContent(msg);
      out.add(ReferenceCountUtil.retain(msg));
      if ((msg instanceof LastHttpContent)) {
        this.state = State.AWAIT_HEADERS;
      }
      break;
    }
  }
  
  private static boolean isPassthru(HttpResponse res)
  {
    int code = res.status().code();
    return (code < 200) || (code == 204) || (code == 304);
  }
  
  private static void ensureHeaders(HttpObject msg)
  {
    if (!(msg instanceof HttpResponse)) {
      throw new IllegalStateException("unexpected message type: " + msg.getClass().getName() + " (expected: " + HttpResponse.class.getSimpleName() + ')');
    }
  }
  
  private static void ensureContent(HttpObject msg)
  {
    if (!(msg instanceof HttpContent)) {
      throw new IllegalStateException("unexpected message type: " + msg.getClass().getName() + " (expected: " + HttpContent.class.getSimpleName() + ')');
    }
  }
  
  private boolean encodeContent(HttpContent c, List<Object> out)
  {
    ByteBuf content = c.content();
    
    encode(content, out);
    if ((c instanceof LastHttpContent))
    {
      finishEncode(out);
      LastHttpContent last = (LastHttpContent)c;
      
      HttpHeaders headers = last.trailingHeaders();
      if (headers.isEmpty()) {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
      } else {
        out.add(new ComposedLastHttpContent(headers));
      }
      return true;
    }
    return false;
  }
  
  protected abstract Result beginEncode(HttpResponse paramHttpResponse, CharSequence paramCharSequence)
    throws Exception;
  
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
    if (this.encoder != null)
    {
      if (this.encoder.finish()) {
        for (;;)
        {
          ByteBuf buf = (ByteBuf)this.encoder.readOutbound();
          if (buf == null) {
            break;
          }
          buf.release();
        }
      }
      this.encoder = null;
    }
  }
  
  private void encode(ByteBuf in, List<Object> out)
  {
    this.encoder.writeOutbound(new Object[] { in.retain() });
    fetchEncoderOutput(out);
  }
  
  private void finishEncode(List<Object> out)
  {
    if (this.encoder.finish()) {
      fetchEncoderOutput(out);
    }
    this.encoder = null;
  }
  
  private void fetchEncoderOutput(List<Object> out)
  {
    for (;;)
    {
      ByteBuf buf = (ByteBuf)this.encoder.readOutbound();
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
  
  public static final class Result
  {
    private final String targetContentEncoding;
    private final EmbeddedChannel contentEncoder;
    
    public Result(String targetContentEncoding, EmbeddedChannel contentEncoder)
    {
      if (targetContentEncoding == null) {
        throw new NullPointerException("targetContentEncoding");
      }
      if (contentEncoder == null) {
        throw new NullPointerException("contentEncoder");
      }
      this.targetContentEncoding = targetContentEncoding;
      this.contentEncoder = contentEncoder;
    }
    
    public String targetContentEncoding()
    {
      return this.targetContentEncoding;
    }
    
    public EmbeddedChannel contentEncoder()
    {
      return this.contentEncoder;
    }
  }
}
