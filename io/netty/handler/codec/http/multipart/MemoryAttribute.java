package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.http.HttpConstants;
import java.io.IOException;
import java.nio.charset.Charset;

public class MemoryAttribute
  extends AbstractMemoryHttpData
  implements Attribute
{
  public MemoryAttribute(String name)
  {
    this(name, HttpConstants.DEFAULT_CHARSET);
  }
  
  public MemoryAttribute(String name, Charset charset)
  {
    super(name, charset, 0L);
  }
  
  public MemoryAttribute(String name, String value)
    throws IOException
  {
    this(name, value, HttpConstants.DEFAULT_CHARSET);
  }
  
  public MemoryAttribute(String name, String value, Charset charset)
    throws IOException
  {
    super(name, charset, 0L);
    setValue(value);
  }
  
  public InterfaceHttpData.HttpDataType getHttpDataType()
  {
    return InterfaceHttpData.HttpDataType.Attribute;
  }
  
  public String getValue()
  {
    return getByteBuf().toString(getCharset());
  }
  
  public void setValue(String value)
    throws IOException
  {
    if (value == null) {
      throw new NullPointerException("value");
    }
    byte[] bytes = value.getBytes(getCharset());
    checkSize(bytes.length);
    ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
    if (this.definedSize > 0L) {
      this.definedSize = buffer.readableBytes();
    }
    setContent(buffer);
  }
  
  public void addContent(ByteBuf buffer, boolean last)
    throws IOException
  {
    int localsize = buffer.readableBytes();
    checkSize(this.size + localsize);
    if ((this.definedSize > 0L) && (this.definedSize < this.size + localsize)) {
      this.definedSize = (this.size + localsize);
    }
    super.addContent(buffer, last);
  }
  
  public int hashCode()
  {
    return getName().hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof Attribute)) {
      return false;
    }
    Attribute attribute = (Attribute)o;
    return getName().equalsIgnoreCase(attribute.getName());
  }
  
  public int compareTo(InterfaceHttpData other)
  {
    if (!(other instanceof Attribute)) {
      throw new ClassCastException("Cannot compare " + getHttpDataType() + " with " + other.getHttpDataType());
    }
    return compareTo((Attribute)other);
  }
  
  public int compareTo(Attribute o)
  {
    return getName().compareToIgnoreCase(o.getName());
  }
  
  public String toString()
  {
    return getName() + '=' + getValue();
  }
  
  public Attribute copy()
  {
    MemoryAttribute attr = new MemoryAttribute(getName());
    attr.setCharset(getCharset());
    ByteBuf content = content();
    if (content != null) {
      try
      {
        attr.setContent(content.copy());
      }
      catch (IOException e)
      {
        throw new ChannelException(e);
      }
    }
    return attr;
  }
  
  public Attribute duplicate()
  {
    MemoryAttribute attr = new MemoryAttribute(getName());
    attr.setCharset(getCharset());
    ByteBuf content = content();
    if (content != null) {
      try
      {
        attr.setContent(content.duplicate());
      }
      catch (IOException e)
      {
        throw new ChannelException(e);
      }
    }
    return attr;
  }
  
  public Attribute retain()
  {
    super.retain();
    return this;
  }
  
  public Attribute retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public Attribute touch()
  {
    super.touch();
    return this;
  }
  
  public Attribute touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
}
