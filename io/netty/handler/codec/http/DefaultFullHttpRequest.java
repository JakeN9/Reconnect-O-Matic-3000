package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DefaultFullHttpRequest
  extends DefaultHttpRequest
  implements FullHttpRequest
{
  private static final int HASH_CODE_PRIME = 31;
  private final ByteBuf content;
  private final HttpHeaders trailingHeader;
  private final boolean validateHeaders;
  
  public DefaultFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri)
  {
    this(httpVersion, method, uri, Unpooled.buffer(0));
  }
  
  public DefaultFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content)
  {
    this(httpVersion, method, uri, content, true);
  }
  
  public DefaultFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, boolean validateHeaders)
  {
    this(httpVersion, method, uri, Unpooled.buffer(0), true);
  }
  
  public DefaultFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content, boolean validateHeaders)
  {
    super(httpVersion, method, uri, validateHeaders);
    if (content == null) {
      throw new NullPointerException("content");
    }
    this.content = content;
    this.trailingHeader = new DefaultHttpHeaders(validateHeaders);
    this.validateHeaders = validateHeaders;
  }
  
  public HttpHeaders trailingHeaders()
  {
    return this.trailingHeader;
  }
  
  public ByteBuf content()
  {
    return this.content;
  }
  
  public int refCnt()
  {
    return this.content.refCnt();
  }
  
  public FullHttpRequest retain()
  {
    this.content.retain();
    return this;
  }
  
  public FullHttpRequest retain(int increment)
  {
    this.content.retain(increment);
    return this;
  }
  
  public FullHttpRequest touch()
  {
    this.content.touch();
    return this;
  }
  
  public FullHttpRequest touch(Object hint)
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
  
  public FullHttpRequest setProtocolVersion(HttpVersion version)
  {
    super.setProtocolVersion(version);
    return this;
  }
  
  public FullHttpRequest setMethod(HttpMethod method)
  {
    super.setMethod(method);
    return this;
  }
  
  public FullHttpRequest setUri(String uri)
  {
    super.setUri(uri);
    return this;
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
    DefaultFullHttpRequest duplicate = new DefaultFullHttpRequest(protocolVersion(), method(), uri(), content().duplicate(), this.validateHeaders);
    
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
    if (!(o instanceof DefaultFullHttpRequest)) {
      return false;
    }
    DefaultFullHttpRequest other = (DefaultFullHttpRequest)o;
    
    return (super.equals(other)) && (content().equals(other.content())) && (trailingHeaders().equals(other.trailingHeaders()));
  }
  
  public String toString()
  {
    return HttpMessageUtil.appendFullRequest(new StringBuilder(256), this).toString();
  }
}
