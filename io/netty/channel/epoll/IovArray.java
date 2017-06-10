package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelOutboundBuffer.MessageProcessor;
import io.netty.util.internal.PlatformDependent;
import java.nio.ByteBuffer;

final class IovArray
  implements ChannelOutboundBuffer.MessageProcessor
{
  private static final int ADDRESS_SIZE = PlatformDependent.addressSize();
  private static final int IOV_SIZE = 2 * ADDRESS_SIZE;
  private static final int CAPACITY = Native.IOV_MAX * IOV_SIZE;
  private final long memoryAddress;
  private int count;
  private long size;
  
  IovArray()
  {
    this.memoryAddress = PlatformDependent.allocateMemory(CAPACITY);
  }
  
  void clear()
  {
    this.count = 0;
    this.size = 0L;
  }
  
  boolean add(ByteBuf buf)
  {
    if (this.count == Native.IOV_MAX) {
      return false;
    }
    int len = buf.readableBytes();
    if (len == 0) {
      return true;
    }
    long addr = buf.memoryAddress();
    int offset = buf.readerIndex();
    add(addr, offset, len);
    return true;
  }
  
  private void add(long addr, int offset, int len)
  {
    if (len == 0) {
      return;
    }
    long baseOffset = memoryAddress(this.count++);
    long lengthOffset = baseOffset + ADDRESS_SIZE;
    if (ADDRESS_SIZE == 8)
    {
      PlatformDependent.putLong(baseOffset, addr + offset);
      PlatformDependent.putLong(lengthOffset, len);
    }
    else
    {
      assert (ADDRESS_SIZE == 4);
      PlatformDependent.putInt(baseOffset, (int)addr + offset);
      PlatformDependent.putInt(lengthOffset, len);
    }
    this.size += len;
  }
  
  boolean add(CompositeByteBuf buf)
  {
    ByteBuffer[] buffers = buf.nioBuffers();
    if (this.count + buffers.length >= Native.IOV_MAX) {
      return false;
    }
    for (int i = 0; i < buffers.length; i++)
    {
      ByteBuffer nioBuffer = buffers[i];
      int offset = nioBuffer.position();
      int len = nioBuffer.limit() - nioBuffer.position();
      if (len != 0)
      {
        long addr = PlatformDependent.directBufferAddress(nioBuffer);
        
        add(addr, offset, len);
      }
    }
    return true;
  }
  
  long processWritten(int index, long written)
  {
    long baseOffset = memoryAddress(index);
    long lengthOffset = baseOffset + ADDRESS_SIZE;
    if (ADDRESS_SIZE == 8)
    {
      long len = PlatformDependent.getLong(lengthOffset);
      if (len > written)
      {
        long offset = PlatformDependent.getLong(baseOffset);
        PlatformDependent.putLong(baseOffset, offset + written);
        PlatformDependent.putLong(lengthOffset, len - written);
        return -1L;
      }
      return len;
    }
    assert (ADDRESS_SIZE == 4);
    long len = PlatformDependent.getInt(lengthOffset);
    if (len > written)
    {
      int offset = PlatformDependent.getInt(baseOffset);
      PlatformDependent.putInt(baseOffset, (int)(offset + written));
      PlatformDependent.putInt(lengthOffset, (int)(len - written));
      return -1L;
    }
    return len;
  }
  
  int count()
  {
    return this.count;
  }
  
  long size()
  {
    return this.size;
  }
  
  long memoryAddress(int offset)
  {
    return this.memoryAddress + IOV_SIZE * offset;
  }
  
  void release()
  {
    PlatformDependent.freeMemory(this.memoryAddress);
  }
  
  public boolean processMessage(Object msg)
    throws Exception
  {
    if ((msg instanceof ByteBuf))
    {
      if ((msg instanceof CompositeByteBuf)) {
        return add((CompositeByteBuf)msg);
      }
      return add((ByteBuf)msg);
    }
    return false;
  }
}
