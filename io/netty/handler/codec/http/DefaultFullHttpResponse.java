package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DefaultFullHttpResponse
  extends DefaultHttpResponse
  implements FullHttpResponse
{
  private static final int HASH_CODE_PRIME = 31;
  private final ByteBuf content;
  private final HttpHeaders trailingHeaders;
  private final boolean validateHeaders;
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status)
  {
    this(version, status, Unpooled.buffer(0));
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content)
  {
    this(version, status, content, false);
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders)
  {
    this(version, status, Unpooled.buffer(0), validateHeaders, false);
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders, boolean singleFieldHeaders)
  {
    this(version, status, Unpooled.buffer(0), validateHeaders, singleFieldHeaders);
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content, boolean singleFieldHeaders)
  {
    this(version, status, content, true, singleFieldHeaders);
  }
  
  public DefaultFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content, boolean validateHeaders, boolean singleFieldHeaders)
  {
    super(version, status, validateHeaders, singleFieldHeaders);
    if (content == null) {
      throw new NullPointerException("content");
    }
    this.content = content;
    this.trailingHeaders = new DefaultHttpHeaders(validateHeaders, singleFieldHeaders);
    this.validateHeaders = validateHeaders;
  }
  
  public HttpHeaders trailingHeaders()
  {
    return this.trailingHeaders;
  }
  
  public ByteBuf content()
  {
    return this.content;
  }
  
  public int refCnt()
  {
    return this.content.refCnt();
  }
  
  public FullHttpResponse retain()
  {
    this.content.retain();
    return this;
  }
  
  public FullHttpResponse retain(int increment)
  {
    this.content.retain(increment);
    return this;
  }
  
  public FullHttpResponse touch()
  {
    this.content.touch();
    return this;
  }
  
  public FullHttpResponse touch(Object hint)
  {
    this.content.touch(hint);
    return this;
  }
  
  public boolean release()
  {
    return this.content.release();
  }
  
  public boolean release(int decrement)
  {
    return this.content.release(decrement);
  }
  
  public FullHttpResponse setProtocolVersion(HttpVersion version)
  {
    super.setProtocolVersion(version);
    return this;
  }
  
  public FullHttpResponse setStatus(HttpResponseStatus status)
  {
    super.setStatus(status);
    return this;
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
    DefaultFullHttpResponse duplicate = new DefaultFullHttpResponse(protocolVersion(), status(), content().duplicate(), this.validateHeaders);
    
    duplicate.headers().set(headers());
    duplicate.trailingHeaders().set(trailingHeaders());
    return duplicate;
  }
  
  public int hashCode()
  {
    int result = 1;
    result = 31 * result + content().hashCode();
    result = 31 * result + trailingHeaders().hashCode();
    result = 31 * result + super.hashCode();
    return result;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultFullHttpResponse)) {
      return false;
    }
    DefaultFullHttpResponse other = (DefaultFullHttpResponse)o;
    
    return (super.equals(other)) && (content().equals(other.content())) && (trailingHeaders().equals(other.trailingHeaders()));
  }
  
  public String toString()
  {
    return HttpMessageUtil.appendFullResponse(new StringBuilder(256), this).toString();
  }
}
