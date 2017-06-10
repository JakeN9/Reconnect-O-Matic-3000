package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.nio.charset.Charset;

public class MemoryFileUpload
  extends AbstractMemoryHttpData
  implements FileUpload
{
  private String filename;
  private String contentType;
  private String contentTransferEncoding;
  
  public MemoryFileUpload(String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size)
  {
    super(name, charset, size);
    setFilename(filename);
    setContentType(contentType);
    setContentTransferEncoding(contentTransferEncoding);
  }
  
  public InterfaceHttpData.HttpDataType getHttpDataType()
  {
    return InterfaceHttpData.HttpDataType.FileUpload;
  }
  
  public String getFilename()
  {
    return this.filename;
  }
  
  public void setFilename(String filename)
  {
    if (filename == null) {
      throw new NullPointerException("filename");
    }
    this.filename = filename;
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
    if (!(o instanceof FileUpload)) {
      throw new ClassCastException("Cannot compare " + getHttpDataType() + " with " + o.getHttpDataType());
    }
    return compareTo((FileUpload)o);
  }
  
  public int compareTo(FileUpload o)
  {
    int v = getName().compareToIgnoreCase(o.getName());
    if (v != 0) {
      return v;
    }
    return v;
  }
  
  public void setContentType(String contentType)
  {
    if (contentType == null) {
      throw new NullPointerException("contentType");
    }
    this.contentType = contentType;
  }
  
  public String getContentType()
  {
    return this.contentType;
  }
  
  public String getContentTransferEncoding()
  {
    return this.contentTransferEncoding;
  }
  
  public void setContentTransferEncoding(String contentTransferEncoding)
  {
    this.contentTransferEncoding = contentTransferEncoding;
  }
  
  public String toString()
  {
    return HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + getName() + "\"; " + HttpHeaderValues.FILENAME + "=\"" + this.filename + "\"\r\n" + HttpHeaderNames.CONTENT_TYPE + ": " + this.contentType + (getCharset() != null ? "; " + HttpHeaderValues.CHARSET + '=' + getCharset() + "\r\n" : "\r\n") + HttpHeaderNames.CONTENT_LENGTH + ": " + length() + "\r\n" + "Completed: " + isCompleted() + "\r\nIsInMemory: " + isInMemory();
  }
  
  public FileUpload copy()
  {
    MemoryFileUpload upload = new MemoryFileUpload(getName(), getFilename(), getContentType(), getContentTransferEncoding(), getCharset(), this.size);
    
    ByteBuf buf = content();
    if (buf != null) {
      try
      {
        upload.setContent(buf.copy());
        return upload;
      }
      catch (IOException e)
      {
        throw new ChannelException(e);
      }
    }
    return upload;
  }
  
  public FileUpload duplicate()
  {
    MemoryFileUpload upload = new MemoryFileUpload(getName(), getFilename(), getContentType(), getContentTransferEncoding(), getCharset(), this.size);
    
    ByteBuf buf = content();
    if (buf != null) {
      try
      {
        upload.setContent(buf.duplicate());
        return upload;
      }
      catch (IOException e)
      {
        throw new ChannelException(e);
      }
    }
    return upload;
  }
  
  public FileUpload retain()
  {
    super.retain();
    return this;
  }
  
  public FileUpload retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public FileUpload touch()
  {
    super.touch();
    return this;
  }
  
  public FileUpload touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
}
