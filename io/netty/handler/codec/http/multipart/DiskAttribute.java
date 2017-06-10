package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.http.HttpConstants;
import java.io.IOException;
import java.nio.charset.Charset;

public class DiskAttribute
  extends AbstractDiskHttpData
  implements Attribute
{
  public static String baseDirectory;
  public static boolean deleteOnExitTemporaryFile = true;
  public static final String prefix = "Attr_";
  public static final String postfix = ".att";
  
  public DiskAttribute(String name)
  {
    this(name, HttpConstants.DEFAULT_CHARSET);
  }
  
  public DiskAttribute(String name, Charset charset)
  {
    super(name, charset, 0L);
  }
  
  public DiskAttribute(String name, String value)
    throws IOException
  {
    this(name, value, HttpConstants.DEFAULT_CHARSET);
  }
  
  public DiskAttribute(String name, String value, Charset charset)
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
    throws IOException
  {
    byte[] bytes = get();
    return new String(bytes, getCharset());
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
    long newDefinedSize = this.size + buffer.readableBytes();
    checkSize(newDefinedSize);
    if ((this.definedSize > 0L) && (this.definedSize < newDefinedSize)) {
      this.definedSize = newDefinedSize;
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
  
  public int compareTo(InterfaceHttpData o)
  {
    if (!(o instanceof Attribute)) {
      throw new ClassCastException("Cannot compare " + getHttpDataType() + " with " + o.getHttpDataType());
    }
    return compareTo((Attribute)o);
  }
  
  public int compareTo(Attribute o)
  {
    return getName().compareToIgnoreCase(o.getName());
  }
  
  public String toString()
  {
    try
    {
      return getName() + '=' + getValue();
    }
    catch (IOException e)
    {
      return getName() + '=' + e;
    }
  }
  
  protected boolean deleteOnExit()
  {
    return deleteOnExitTemporaryFile;
  }
  
  protected String getBaseDirectory()
  {
    return baseDirectory;
  }
  
  protected String getDiskFilename()
  {
    return getName() + ".att";
  }
  
  protected String getPostfix()
  {
    return ".att";
  }
  
  protected String getPrefix()
  {
    return "Attr_";
  }
  
  public Attribute copy()
  {
    DiskAttribute attr = new DiskAttribute(getName());
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
    DiskAttribute attr = new DiskAttribute(getName());
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
  
  public Attribute retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public Attribute retain()
  {
    super.retain();
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
