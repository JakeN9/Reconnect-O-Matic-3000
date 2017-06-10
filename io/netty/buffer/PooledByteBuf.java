package io.netty.buffer;

import io.netty.util.Recycler.Handle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

abstract class PooledByteBuf<T>
  extends AbstractReferenceCountedByteBuf
{
  private final Recycler.Handle<PooledByteBuf<T>> recyclerHandle;
  protected PoolChunk<T> chunk;
  protected long handle;
  protected T memory;
  protected int offset;
  protected int length;
  int maxLength;
  Thread initThread;
  private ByteBuffer tmpNioBuf;
  
  protected PooledByteBuf(Recycler.Handle<? extends PooledByteBuf<T>> recyclerHandle, int maxCapacity)
  {
    super(maxCapacity);
    this.recyclerHandle = recyclerHandle;
  }
  
  void init(PoolChunk<T> chunk, long handle, int offset, int length, int maxLength)
  {
    assert (handle >= 0L);
    assert (chunk != null);
    
    this.chunk = chunk;
    this.handle = handle;
    this.memory = chunk.memory;
    this.offset = offset;
    this.length = length;
    this.maxLength = maxLength;
    setIndex(0, 0);
    this.tmpNioBuf = null;
    this.initThread = Thread.currentThread();
  }
  
  void initUnpooled(PoolChunk<T> chunk, int length)
  {
    assert (chunk != null);
    
    this.chunk = chunk;
    this.handle = 0L;
    this.memory = chunk.memory;
    this.offset = 0;
    this.length = (this.maxLength = length);
    setIndex(0, 0);
    this.tmpNioBuf = null;
    this.initThread = Thread.currentThread();
  }
  
  public final int capacity()
  {
    return this.length;
  }
  
  public final ByteBuf capacity(int newCapacity)
  {
    ensureAccessible();
    if (this.chunk.unpooled)
    {
      if (newCapacity == this.length) {
        return this;
      }
    }
    else if (newCapacity > this.length)
    {
      if (newCapacity <= this.maxLength)
      {
        this.length = newCapacity;
        return this;
      }
    }
    else if (newCapacity < this.length)
    {
      if (newCapacity > this.maxLength >>> 1) {
        if (this.maxLength <= 512)
        {
          if (newCapacity > this.maxLength - 16)
          {
            this.length = newCapacity;
            setIndex(Math.min(readerIndex(), newCapacity), Math.min(writerIndex(), newCapacity));
            return this;
          }
        }
        else
        {
          this.length = newCapacity;
          setIndex(Math.min(readerIndex(), newCapacity), Math.min(writerIndex(), newCapacity));
          return this;
        }
      }
    }
    else {
      return this;
    }
    this.chunk.arena.reallocate(this, newCapacity, true);
    return this;
  }
  
  public final ByteBufAllocator alloc()
  {
    return this.chunk.arena.parent;
  }
  
  public final ByteOrder order()
  {
    return ByteOrder.BIG_ENDIAN;
  }
  
  public final ByteBuf unwrap()
  {
    return null;
  }
  
  protected final ByteBuffer internalNioBuffer()
  {
    ByteBuffer tmpNioBuf = this.tmpNioBuf;
    if (tmpNioBuf == null) {
      this.tmpNioBuf = (tmpNioBuf = newInternalNioBuffer(this.memory));
    }
    return tmpNioBuf;
  }
  
  protected abstract ByteBuffer newInternalNioBuffer(T paramT);
  
  protected final void deallocate()
  {
    if (this.handle >= 0L)
    {
      long handle = this.handle;
      this.handle = -1L;
      this.memory = null;
      boolean sameThread = this.initThread == Thread.currentThread();
      this.initThread = null;
      this.chunk.arena.free(this.chunk, handle, this.maxLength, sameThread);
      recycle();
    }
  }
  
  private void recycle()
  {
    this.recyclerHandle.recycle(this);
  }
  
  protected final int idx(int index)
  {
    return this.offset + index;
  }
}
