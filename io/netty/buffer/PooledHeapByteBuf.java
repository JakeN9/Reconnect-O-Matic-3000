package io.netty.buffer;

import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

final class PooledHeapByteBuf
  extends PooledByteBuf<byte[]>
{
  private static final Recycler<PooledHeapByteBuf> RECYCLER = new Recycler()
  {
    protected PooledHeapByteBuf newObject(Recycler.Handle<PooledHeapByteBuf> handle)
    {
      return new PooledHeapByteBuf(handle, 0, null);
    }
  };
  
  static PooledHeapByteBuf newInstance(int maxCapacity)
  {
    PooledHeapByteBuf buf = (PooledHeapByteBuf)RECYCLER.get();
    buf.setRefCnt(1);
    buf.maxCapacity(maxCapacity);
    return buf;
  }
  
  private PooledHeapByteBuf(Recycler.Handle<PooledHeapByteBuf> recyclerHandle, int maxCapacity)
  {
    super(recyclerHandle, maxCapacity);
  }
  
  public boolean isDirect()
  {
    return false;
  }
  
  protected byte _getByte(int index)
  {
    return ((byte[])this.memory)[idx(index)];
  }
  
  protected short _getShort(int index)
  {
    index = idx(index);
    return (short)(((byte[])this.memory)[index] << 8 | ((byte[])this.memory)[(index + 1)] & 0xFF);
  }
  
  protected int _getUnsignedMedium(int index)
  {
    index = idx(index);
    return (((byte[])this.memory)[index] & 0xFF) << 16 | (((byte[])this.memory)[(index + 1)] & 0xFF) << 8 | ((byte[])this.memory)[(index + 2)] & 0xFF;
  }
  
  protected int _getInt(int index)
  {
    index = idx(index);
    return (((byte[])this.memory)[index] & 0xFF) << 24 | (((byte[])this.memory)[(index + 1)] & 0xFF) << 16 | (((byte[])this.memory)[(index + 2)] & 0xFF) << 8 | ((byte[])this.memory)[(index + 3)] & 0xFF;
  }
  
  protected long _getLong(int index)
  {
    index = idx(index);
    return (((byte[])this.memory)[index] & 0xFF) << 56 | (((byte[])this.memory)[(index + 1)] & 0xFF) << 48 | (((byte[])this.memory)[(index + 2)] & 0xFF) << 40 | (((byte[])this.memory)[(index + 3)] & 0xFF) << 32 | (((byte[])this.memory)[(index + 4)] & 0xFF) << 24 | (((byte[])this.memory)[(index + 5)] & 0xFF) << 16 | (((byte[])this.memory)[(index + 6)] & 0xFF) << 8 | ((byte[])this.memory)[(index + 7)] & 0xFF;
  }
  
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.capacity());
    if (dst.hasMemoryAddress()) {
      PlatformDependent.copyMemory((byte[])this.memory, idx(index), dst.memoryAddress() + dstIndex, length);
    } else if (dst.hasArray()) {
      getBytes(index, dst.array(), dst.arrayOffset() + dstIndex, length);
    } else {
      dst.setBytes(dstIndex, (byte[])this.memory, idx(index), length);
    }
    return this;
  }
  
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.length);
    System.arraycopy(this.memory, idx(index), dst, dstIndex, length);
    return this;
  }
  
  public ByteBuf getBytes(int index, ByteBuffer dst)
  {
    checkIndex(index);
    dst.put((byte[])this.memory, idx(index), Math.min(capacity() - index, dst.remaining()));
    return this;
  }
  
  public ByteBuf getBytes(int index, OutputStream out, int length)
    throws IOException
  {
    checkIndex(index, length);
    out.write((byte[])this.memory, idx(index), length);
    return this;
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length)
    throws IOException
  {
    return getBytes(index, out, length, false);
  }
  
  private int getBytes(int index, GatheringByteChannel out, int length, boolean internal)
    throws IOException
  {
    checkIndex(index, length);
    index = idx(index);
    ByteBuffer tmpBuf;
    ByteBuffer tmpBuf;
    if (internal) {
      tmpBuf = internalNioBuffer();
    } else {
      tmpBuf = ByteBuffer.wrap((byte[])this.memory);
    }
    return out.write((ByteBuffer)tmpBuf.clear().position(index).limit(index + length));
  }
  
  public int readBytes(GatheringByteChannel out, int length)
    throws IOException
  {
    checkReadableBytes(length);
    int readBytes = getBytes(this.readerIndex, out, length, true);
    this.readerIndex += readBytes;
    return readBytes;
  }
  
  protected void _setByte(int index, int value)
  {
    ((byte[])this.memory)[idx(index)] = ((byte)value);
  }
  
  protected void _setShort(int index, int value)
  {
    index = idx(index);
    ((byte[])this.memory)[index] = ((byte)(value >>> 8));
    ((byte[])this.memory)[(index + 1)] = ((byte)value);
  }
  
  protected void _setMedium(int index, int value)
  {
    index = idx(index);
    ((byte[])this.memory)[index] = ((byte)(value >>> 16));
    ((byte[])this.memory)[(index + 1)] = ((byte)(value >>> 8));
    ((byte[])this.memory)[(index + 2)] = ((byte)value);
  }
  
  protected void _setInt(int index, int value)
  {
    index = idx(index);
    ((byte[])this.memory)[index] = ((byte)(value >>> 24));
    ((byte[])this.memory)[(index + 1)] = ((byte)(value >>> 16));
    ((byte[])this.memory)[(index + 2)] = ((byte)(value >>> 8));
    ((byte[])this.memory)[(index + 3)] = ((byte)value);
  }
  
  protected void _setLong(int index, long value)
  {
    index = idx(index);
    ((byte[])this.memory)[index] = ((byte)(int)(value >>> 56));
    ((byte[])this.memory)[(index + 1)] = ((byte)(int)(value >>> 48));
    ((byte[])this.memory)[(index + 2)] = ((byte)(int)(value >>> 40));
    ((byte[])this.memory)[(index + 3)] = ((byte)(int)(value >>> 32));
    ((byte[])this.memory)[(index + 4)] = ((byte)(int)(value >>> 24));
    ((byte[])this.memory)[(index + 5)] = ((byte)(int)(value >>> 16));
    ((byte[])this.memory)[(index + 6)] = ((byte)(int)(value >>> 8));
    ((byte[])this.memory)[(index + 7)] = ((byte)(int)value);
  }
  
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.capacity());
    if (src.hasMemoryAddress()) {
      PlatformDependent.copyMemory(src.memoryAddress() + srcIndex, (byte[])this.memory, idx(index), length);
    } else if (src.hasArray()) {
      setBytes(index, src.array(), src.arrayOffset() + srcIndex, length);
    } else {
      src.getBytes(srcIndex, (byte[])this.memory, idx(index), length);
    }
    return this;
  }
  
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.length);
    System.arraycopy(src, srcIndex, this.memory, idx(index), length);
    return this;
  }
  
  public ByteBuf setBytes(int index, ByteBuffer src)
  {
    int length = src.remaining();
    checkIndex(index, length);
    src.get((byte[])this.memory, idx(index), length);
    return this;
  }
  
  public int setBytes(int index, InputStream in, int length)
    throws IOException
  {
    checkIndex(index, length);
    return in.read((byte[])this.memory, idx(index), length);
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length)
    throws IOException
  {
    checkIndex(index, length);
    index = idx(index);
    try
    {
      return in.read((ByteBuffer)internalNioBuffer().clear().position(index).limit(index + length));
    }
    catch (ClosedChannelException ignored) {}
    return -1;
  }
  
  public ByteBuf copy(int index, int length)
  {
    checkIndex(index, length);
    ByteBuf copy = alloc().heapBuffer(length, maxCapacity());
    copy.writeBytes((byte[])this.memory, idx(index), length);
    return copy;
  }
  
  public int nioBufferCount()
  {
    return 1;
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    return new ByteBuffer[] { nioBuffer(index, length) };
  }
  
  public ByteBuffer nioBuffer(int index, int length)
  {
    checkIndex(index, length);
    index = idx(index);
    ByteBuffer buf = ByteBuffer.wrap((byte[])this.memory, index, length);
    return buf.slice();
  }
  
  public ByteBuffer internalNioBuffer(int index, int length)
  {
    checkIndex(index, length);
    index = idx(index);
    return (ByteBuffer)internalNioBuffer().clear().position(index).limit(index + length);
  }
  
  public boolean hasArray()
  {
    return true;
  }
  
  public byte[] array()
  {
    ensureAccessible();
    return (byte[])this.memory;
  }
  
  public int arrayOffset()
  {
    return this.offset;
  }
  
  public boolean hasMemoryAddress()
  {
    return false;
  }
  
  public long memoryAddress()
  {
    throw new UnsupportedOperationException();
  }
  
  protected ByteBuffer newInternalNioBuffer(byte[] memory)
  {
    return ByteBuffer.wrap(memory);
  }
}
