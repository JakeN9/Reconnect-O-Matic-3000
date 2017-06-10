package io.netty.buffer;

import io.netty.util.internal.PlatformDependent;

public final class UnpooledByteBufAllocator
  extends AbstractByteBufAllocator
{
  public static final UnpooledByteBufAllocator DEFAULT = new UnpooledByteBufAllocator(PlatformDependent.directBufferPreferred());
  
  public UnpooledByteBufAllocator(boolean preferDirect)
  {
    super(preferDirect);
  }
  
  protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity)
  {
    return new UnpooledHeapByteBuf(this, initialCapacity, maxCapacity);
  }
  
  protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity)
  {
    ByteBuf buf;
    ByteBuf buf;
    if (PlatformDependent.hasUnsafe()) {
      buf = new UnpooledUnsafeDirectByteBuf(this, initialCapacity, maxCapacity);
    } else {
      buf = new UnpooledDirectByteBuf(this, initialCapacity, maxCapacity);
    }
    return toLeakAwareBuffer(buf);
  }
  
  public boolean isDirectBufferPooled()
  {
    return false;
  }
}
