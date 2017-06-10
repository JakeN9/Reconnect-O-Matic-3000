package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.AsciiString;
import io.netty.util.internal.StringUtil;
import java.util.Map.Entry;

public class DefaultLastHttpContent
  extends DefaultHttpContent
  implements LastHttpContent
{
  private final HttpHeaders trailingHeaders;
  private final boolean validateHeaders;
  
  public DefaultLastHttpContent()
  {
    this(Unpooled.buffer(0));
  }
  
  public DefaultLastHttpContent(ByteBuf content)
  {
    this(content, true);
  }
  
  public DefaultLastHttpContent(ByteBuf content, boolean validateHeaders)
  {
    super(content);
    this.trailingHeaders = new TrailingHttpHeaders(validateHeaders);
    this.validateHeaders = validateHeaders;
  }
  
  public LastHttpContent copy()
  {
    DefaultLastHttpContent copy = new DefaultLastHttpContent(content().copy(), this.validateHeaders);
    copy.trailingHeaders().set(trailingHeaders());
    return copy;
  }
  
  public LastHttpContent duplicate()
  {
    DefaultLastHttpContent copy = new DefaultLastHttpContent(content().duplicate(), this.validateHeaders);
    copy.trailingHeaders().set(trailingHeaders());
    return copy;
  }
  
  public LastHttpContent retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public LastHttpContent retain()
  {
    super.retain();
    return this;
  }
  
  public LastHttpContent touch()
  {
    super.touch();
    return this;
  }
  
  public LastHttpContent touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
  
  public HttpHeaders trailingHeaders()
  {
    return this.trailingHeaders;
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder(super.toString());
    buf.append(StringUtil.NEWLINE);
    appendHeaders(buf);
    
    buf.setLength(buf.length() - StringUtil.NEWLINE.length());
    return buf.toString();
  }
  
  private void appendHeaders(StringBuilder buf)
  {
    for (Map.Entry<CharSequence, CharSequence> e : trailingHeaders())
    {
      buf.append((CharSequence)e.getKey());
      buf.append(": ");
      buf.append((CharSequence)e.getValue());
      buf.append(StringUtil.NEWLINE);
    }
  }
  
  private static final class TrailingHttpHeaders
    extends DefaultHttpHeaders
  {
    private static final class TrailingHttpHeadersNameConverter
      extends DefaultHttpHeaders.HttpHeadersNameConverter
    {
      TrailingHttpHeadersNameConverter(boolean validate)
      {
        super();
      }
      
      public CharSequence convertName(CharSequence name)
      {
        name = super.convertName(name);
        if ((this.validate) && (
          (HttpHeaderNames.CONTENT_LENGTH.equalsIgnoreCase(name)) || (HttpHeaderNames.TRANSFER_ENCODING.equalsIgnoreCase(name)) || (HttpHeaderNames.TRAILER.equalsIgnoreCase(name)))) {
          throw new IllegalArgumentException("prohibited trailing header: " + name);
        }
        return name;
      }
    }
    
    private static final TrailingHttpHeadersNameConverter VALIDATE_NAME_CONVERTER = new TrailingHttpHeadersNameConverter(true);
    private static final TrailingHttpHeadersNameConverter NO_VALIDATE_NAME_CONVERTER = new TrailingHttpHeadersNameConverter(false);
    
    TrailingHttpHeaders(boolean validate)
    {
      super(validate ? VALIDATE_NAME_CONVERTER : NO_VALIDATE_NAME_CONVERTER, false);
    }
  }
}
