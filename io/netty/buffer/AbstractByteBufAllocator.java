package io.netty.buffer;

import io.netty.util.ResourceLeak;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;

public abstract class AbstractByteBufAllocator
  implements ByteBufAllocator
{
  private static final int DEFAULT_INITIAL_CAPACITY = 256;
  private static final int DEFAULT_MAX_COMPONENTS = 16;
  private final boolean directByDefault;
  private final ByteBuf emptyBuf;
  
  protected static ByteBuf toLeakAwareBuffer(ByteBuf buf)
  {
    ResourceLeak leak;
    switch (ResourceLeakDetector.getLevel())
    {
    case SIMPLE: 
      leak = AbstractByteBuf.leakDetector.open(buf);
      if (leak != null) {
        buf = new SimpleLeakAwareByteBuf(buf, leak);
      }
      break;
    case ADVANCED: 
    case PARANOID: 
      leak = AbstractByteBuf.leakDetector.open(buf);
      if (leak != null) {
        buf = new AdvancedLeakAwareByteBuf(buf, leak);
      }
      break;
    }
    return buf;
  }
  
  protected AbstractByteBufAllocator()
  {
    this(false);
  }
  
  protected AbstractByteBufAllocator(boolean preferDirect)
  {
    this.directByDefault = ((preferDirect) && (PlatformDependent.hasUnsafe()));
    this.emptyBuf = new EmptyByteBuf(this);
  }
  
  public ByteBuf buffer()
  {
    if (this.directByDefault) {
      return directBuffer();
    }
    return heapBuffer();
  }
  
  public ByteBuf buffer(int initialCapacity)
  {
    if (this.directByDefault) {
      return directBuffer(initialCapacity);
    }
    return heapBuffer(initialCapacity);
  }
  
  public ByteBuf buffer(int initialCapacity, int maxCapacity)
  {
    if (this.directByDefault) {
      return directBuffer(initialCapacity, maxCapacity);
    }
    return heapBuffer(initialCapacity, maxCapacity);
  }
  
  public ByteBuf ioBuffer()
  {
    if (PlatformDependent.hasUnsafe()) {
      return directBuffer(256);
    }
    return heapBuffer(256);
  }
  
  public ByteBuf ioBuffer(int initialCapacity)
  {
    if (PlatformDependent.hasUnsafe()) {
      return directBuffer(initialCapacity);
    }
    return heapBuffer(initialCapacity);
  }
  
  public ByteBuf ioBuffer(int initialCapacity, int maxCapacity)
  {
    if (PlatformDependent.hasUnsafe()) {
      return directBuffer(initialCapacity, maxCapacity);
    }
    return heapBuffer(initialCapacity, maxCapacity);
  }
  
  public ByteBuf heapBuffer()
  {
    return heapBuffer(256, Integer.MAX_VALUE);
  }
  
  public ByteBuf heapBuffer(int initialCapacity)
  {
    return heapBuffer(initialCapacity, Integer.MAX_VALUE);
  }
  
  public ByteBuf heapBuffer(int initialCapacity, int maxCapacity)
  {
    if ((initialCapacity == 0) && (maxCapacity == 0)) {
      return this.emptyBuf;
    }
    validate(initialCapacity, maxCapacity);
    return newHeapBuffer(initialCapacity, maxCapacity);
  }
  
  public ByteBuf directBuffer()
  {
    return directBuffer(256, Integer.MAX_VALUE);
  }
  
  public ByteBuf directBuffer(int initialCapacity)
  {
    return directBuffer(initialCapacity, Integer.MAX_VALUE);
  }
  
  public ByteBuf directBuffer(int initialCapacity, int maxCapacity)
  {
    if ((initialCapacity == 0) && (maxCapacity == 0)) {
      return this.emptyBuf;
    }
    validate(initialCapacity, maxCapacity);
    return newDirectBuffer(initialCapacity, maxCapacity);
  }
  
  public CompositeByteBuf compositeBuffer()
  {
    if (this.directByDefault) {
      return compositeDirectBuffer();
    }
    return compositeHeapBuffer();
  }
  
  public CompositeByteBuf compositeBuffer(int maxNumComponents)
  {
    if (this.directByDefault) {
      return compositeDirectBuffer(maxNumComponents);
    }
    return compositeHeapBuffer(maxNumComponents);
  }
  
  public CompositeByteBuf compositeHeapBuffer()
  {
    return compositeHeapBuffer(16);
  }
  
  public CompositeByteBuf compositeHeapBuffer(int maxNumComponents)
  {
    return new CompositeByteBuf(this, false, maxNumComponents);
  }
  
  public CompositeByteBuf compositeDirectBuffer()
  {
    return compositeDirectBuffer(16);
  }
  
  public CompositeByteBuf compositeDirectBuffer(int maxNumComponents)
  {
    return new CompositeByteBuf(this, true, maxNumComponents);
  }
  
  private static void validate(int initialCapacity, int maxCapacity)
  {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("initialCapacity: " + initialCapacity + " (expectd: 0+)");
    }
    if (initialCapacity > maxCapacity) {
      throw new IllegalArgumentException(String.format("initialCapacity: %d (expected: not greater than maxCapacity(%d)", new Object[] { Integer.valueOf(initialCapacity), Integer.valueOf(maxCapacity) }));
    }
  }
  
  protected abstract ByteBuf newHeapBuffer(int paramInt1, int paramInt2);
  
  protected abstract ByteBuf newDirectBuffer(int paramInt1, int paramInt2);
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + "(directByDefault: " + this.directByDefault + ')';
  }
  
  public int calculateNewCapacity(int minNewCapacity, int maxCapacity)
  {
    if (minNewCapacity < 0) {
      throw new IllegalArgumentException("minNewCapacity: " + minNewCapacity + " (expectd: 0+)");
    }
    if (minNewCapacity > maxCapacity) {
      throw new IllegalArgumentException(String.format("minNewCapacity: %d (expected: not greater than maxCapacity(%d)", new Object[] { Integer.valueOf(minNewCapacity), Integer.valueOf(maxCapacity) }));
    }
    int threshold = 4194304;
    if (minNewCapacity == 4194304) {
      return 4194304;
    }
    if (minNewCapacity > 4194304)
    {
      int newCapacity = minNewCapacity / 4194304 * 4194304;
      if (newCapacity > maxCapacity - 4194304) {
        newCapacity = maxCapacity;
      } else {
        newCapacity += 4194304;
      }
      return newCapacity;
    }
    int newCapacity = 64;
    while (newCapacity < minNewCapacity) {
      newCapacity <<= 1;
    }
    return Math.min(newCapacity, maxCapacity);
  }
}
