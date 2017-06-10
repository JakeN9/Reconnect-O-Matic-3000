package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.MessageAggregator;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class HttpObjectAggregator
  extends MessageAggregator<HttpObject, HttpMessage, HttpContent, FullHttpMessage>
{
  private static final InternalLogger logger;
  private static final FullHttpResponse CONTINUE;
  private static final FullHttpResponse TOO_LARGE;
  
  static
  {
    logger = InternalLoggerFactory.getInstance(HttpObjectAggregator.class);
    CONTINUE = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);
    
    TOO_LARGE = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER);
    
    TOO_LARGE.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
  }
  
  public HttpObjectAggregator(int maxContentLength)
  {
    super(maxContentLength);
  }
  
  protected boolean isStartMessage(HttpObject msg)
    throws Exception
  {
    return msg instanceof HttpMessage;
  }
  
  protected boolean isContentMessage(HttpObject msg)
    throws Exception
  {
    return msg instanceof HttpContent;
  }
  
  protected boolean isLastContentMessage(HttpContent msg)
    throws Exception
  {
    return msg instanceof LastHttpContent;
  }
  
  protected boolean isAggregated(HttpObject msg)
    throws Exception
  {
    return msg instanceof FullHttpMessage;
  }
  
  protected boolean hasContentLength(HttpMessage start)
    throws Exception
  {
    return HttpHeaderUtil.isContentLengthSet(start);
  }
  
  protected long contentLength(HttpMessage start)
    throws Exception
  {
    return HttpHeaderUtil.getContentLength(start);
  }
  
  protected Object newContinueResponse(HttpMessage start)
    throws Exception
  {
    if (HttpHeaderUtil.is100ContinueExpected(start)) {
      return CONTINUE;
    }
    return null;
  }
  
  protected FullHttpMessage beginAggregation(HttpMessage start, ByteBuf content)
    throws Exception
  {
    assert (!(start instanceof FullHttpMessage));
    
    HttpHeaderUtil.setTransferEncodingChunked(start, false);
    AggregatedFullHttpMessage ret;
    if ((start instanceof HttpRequest))
    {
      ret = new AggregatedFullHttpRequest((HttpRequest)start, content, null);
    }
    else
    {
      AggregatedFullHttpMessage ret;
      if ((start instanceof HttpResponse)) {
        ret = new AggregatedFullHttpResponse((HttpResponse)start, content, null);
      } else {
        throw new Error();
      }
    }
    AggregatedFullHttpMessage ret;
    return ret;
  }
  
  protected void aggregate(FullHttpMessage aggregated, HttpContent content)
    throws Exception
  {
    if ((content instanceof LastHttpContent)) {
      ((AggregatedFullHttpMessage)aggregated).setTrailingHeaders(((LastHttpContent)content).trailingHeaders());
    }
  }
  
  protected void finishAggregation(FullHttpMessage aggregated)
    throws Exception
  {
    if (!HttpHeaderUtil.isContentLengthSet(aggregated)) {
      aggregated.headers().set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(aggregated.content().readableBytes()));
    }
  }
  
  protected void handleOversizedMessage(final ChannelHandlerContext ctx, HttpMessage oversized)
    throws Exception
  {
    if ((oversized instanceof HttpRequest))
    {
      ChannelFuture future = ctx.writeAndFlush(TOO_LARGE).addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future)
          throws Exception
        {
          if (!future.isSuccess())
          {
            HttpObjectAggregator.logger.debug("Failed to send a 413 Request Entity Too Large.", future.cause());
            ctx.close();
          }
        }
      });
      if (((oversized instanceof FullHttpMessage)) || ((!HttpHeaderUtil.is100ContinueExpected(oversized)) && (!HttpHeaderUtil.isKeepAlive(oversized)))) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
      HttpObjectDecoder decoder = (HttpObjectDecoder)ctx.pipeline().get(HttpObjectDecoder.class);
      if (decoder != null) {
        decoder.reset();
      }
    }
    else
    {
      if ((oversized instanceof HttpResponse))
      {
        ctx.close();
        throw new TooLongFrameException("Response entity too large: " + oversized);
      }
      throw new IllegalStateException();
    }
  }
  
  private static abstract class AggregatedFullHttpMessage
    extends DefaultByteBufHolder
    implements FullHttpMessage
  {
    protected final HttpMessage message;
    private HttpHeaders trailingHeaders;
    
    AggregatedFullHttpMessage(HttpMessage message, ByteBuf content, HttpHeaders trailingHeaders)
    {
      super();
      this.message = message;
      this.trailingHeaders = trailingHeaders;
    }
    
    public HttpHeaders trailingHeaders()
    {
      HttpHeaders trailingHeaders = this.trailingHeaders;
      if (trailingHeaders == null) {
        return EmptyHttpHeaders.INSTANCE;
      }
      return trailingHeaders;
    }
    
    void setTrailingHeaders(HttpHeaders trailingHeaders)
    {
      this.trailingHeaders = trailingHeaders;
    }
    
    public HttpVersion protocolVersion()
    {
      return this.message.protocolVersion();
    }
    
    public FullHttpMessage setProtocolVersion(HttpVersion version)
    {
      this.message.setProtocolVersion(version);
      return this;
    }
    
    public HttpHeaders headers()
    {
      return this.message.headers();
    }
    
    public DecoderResult decoderResult()
    {
      return this.message.decoderResult();
    }
    
    public void setDecoderResult(DecoderResult result)
    {
      this.message.setDecoderResult(result);
    }
    
    public FullHttpMessage retain(int increment)
    {
      super.retain(increment);
      return this;
    }
    
    public FullHttpMessage retain()
    {
      super.retain();
      return this;
    }
    
    public FullHttpMessage touch(Object hint)
    {
      super.touch(hint);
      return this;
    }
    
    public FullHttpMessage touch()
    {
      super.touch();
      return this;
    }
    
    public abstract FullHttpMessage copy();
    
    public abstract FullHttpMessage duplicate();
  }
  
  private static final class AggregatedFullHttpRequest
    extends HttpObjectAggregator.AggregatedFullHttpMessage
    implements FullHttpRequest
  {
    AggregatedFullHttpRequest(HttpRequest request, ByteBuf content, HttpHeaders trailingHeaders)
    {
      super(content, trailingHeaders);
    }
    
    private FullHttpRequest copy(boolean copyContent, ByteBuf newContent)
    {
      DefaultFullHttpRequest copy = new DefaultFullHttpRequest(protocolVersion(), method(), uri(), newContent == null ? Unpooled.buffer(0) : copyContent ? content().copy() : newContent);
      
      copy.headers().set(headers());
      copy.trailingHeaders().set(trailingHeaders());
      return copy;
    }
    
    public FullHttpRequest copy(ByteBuf newContent)
    {
      return copy(false, newContent);
    }
    
    public FullHttpRequest copy()
    {
      return copy(true, null);
    }
    
    public FullHttpRequest duplicate()
    {
      DefaultFullHttpRequest duplicate = new DefaultFullHttpRequest(protocolVersion(), method(), uri(), content().duplicate());
      
      duplicate.headers().set(headers());
      duplicate.trailingHeaders().set(trailingHeaders());
      return duplicate;
    }
    
    public FullHttpRequest retain(int increment)
    {
      super.retain(increment);
      return this;
    }
    
    public FullHttpRequest retain()
    {
      super.retain();
      return this;
    }
    
    public FullHttpRequest touch()
    {
      super.touch();
      return this;
    }
    
    public FullHttpRequest touch(Object hint)
    {
      super.touch(hint);
      return this;
    }
    
    public FullHttpRequest setMethod(HttpMethod method)
    {
      ((HttpRequest)this.message).setMethod(method);
      return this;
    }
    
    public FullHttpRequest setUri(String uri)
    {
      ((HttpRequest)this.message).setUri(uri);
      return this;
    }
    
    public HttpMethod method()
    {
      return ((HttpRequest)this.message).method();
    }
    
    public String uri()
    {
      return ((HttpRequest)this.message).uri();
    }
    
    public FullHttpRequest setProtocolVersion(HttpVersion version)
    {
      super.setProtocolVersion(version);
      return this;
    }
    
    public String toString()
    {
      return HttpMessageUtil.appendFullRequest(new StringBuilder(256), this).toString();
    }
  }
  
  private static final class AggregatedFullHttpResponse
    extends HttpObjectAggregator.AggregatedFullHttpMessage
    implements FullHttpResponse
  {
    AggregatedFullHttpResponse(HttpResponse message, ByteBuf content, HttpHeaders trailingHeaders)
    {
      super(content, trailingHeaders);
    }
    
    private FullHttpResponse copy(boolean copyContent, ByteBuf newContent)
    {
      DefaultFullHttpResponse copy = new DefaultFullHttpResponse(protocolVersion(), status(), newContent == null ? Unpooled.buffer(0) : copyContent ? content().copy() : newContent);
      
      copy.headers().set(headers());
      copy.trailingHeaders().set(trailingHeaders());
      return copy;
    }
    
    public FullHttpResponse copy(ByteBuf newContent)
    {
      return copy(false, newContent);
    }
    
    public FullHttpResponse copy()
    {
      return copy(true, null);
    }
    
    public FullHttpResponse duplicate()
    {
      DefaultFullHttpResponse duplicate = new DefaultFullHttpResponse(protocolVersion(), status(), content().duplicate());
      
      duplicate.headers().set(headers());
      duplicate.trailingHeaders().set(trailingHeaders());
      return duplicate;
    }
    
    public FullHttpResponse setStatus(HttpResponseStatus status)
    {
      ((HttpResponse)this.message).setStatus(status);
      return this;
    }
    
    public HttpResponseStatus status()
    {
      return ((HttpResponse)this.message).status();
    }
    
    public FullHttpResponse setProtocolVersion(HttpVersion version)
    {
      super.setProtocolVersion(version);
      return this;
    }
    
    public FullHttpResponse retain(int increment)
    {
      super.retain(increment);
      return this;
    }
    
    public FullHttpResponse retain()
    {
      super.retain();
      return this;
    }
    
    public FullHttpResponse touch(Object hint)
    {
      super.touch(hint);
      return this;
    }
    
    public FullHttpResponse touch()
    {
      super.touch();
      return this;
    }
    
    public String toString()
    {
      return HttpMessageUtil.appendFullResponse(new StringBuilder(256), this).toString();
    }
  }
}
