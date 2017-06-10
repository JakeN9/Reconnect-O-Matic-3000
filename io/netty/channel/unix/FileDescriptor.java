package io.netty.channel.unix;

import java.io.IOException;

public class FileDescriptor
{
  private final int fd;
  private volatile boolean open = true;
  
  public FileDescriptor(int fd)
  {
    if (fd < 0) {
      throw new IllegalArgumentException("fd must be >= 0");
    }
    this.fd = fd;
  }
  
  public int intValue()
  {
    return this.fd;
  }
  
  public void close()
    throws IOException
  {
    this.open = false;
    close(this.fd);
  }
  
  public boolean isOpen()
  {
    return this.open;
  }
  
  public String toString()
  {
    return "FileDescriptor{fd=" + this.fd + '}';
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FileDescriptor)) {
      return false;
    }
    return this.fd == ((FileDescriptor)o).fd;
  }
  
  public int hashCode()
  {
    return this.fd;
  }
  
  private static native int close(int paramInt);
}
