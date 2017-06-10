package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.buffer.UnpooledUnsafeDirectByteBuf;
import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

final class ThreadLocalPooledDirectByteBuf
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadLocalPooledDirectByteBuf.class);
  public static final int threadLocalDirectBufferSize = SystemPropertyUtil.getInt("io.netty.threadLocalDirectBufferSize", 65536);
  
  static
  {
    logger.debug("-Dio.netty.threadLocalDirectBufferSize: {}", Integer.valueOf(threadLocalDirectBufferSize));
  }
  
  public static ByteBuf newInstance()
  {
    if (PlatformDependent.hasUnsafe()) {
      return ThreadLocalUnsafeDirectByteBuf.newInstance();
    }
    return ThreadLocalDirectByteBuf.newInstance();
  }
  
  static final class ThreadLocalUnsafeDirectByteBuf
    extends UnpooledUnsafeDirectByteBuf
  {
    private static final Recycler<ThreadLocalUnsafeDirectByteBuf> RECYCLER = new Recycler()
    {
      protected ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf newObject(Recycler.Handle<ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf> handle)
      {
        return new ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf(handle, null);
      }
    };
    private final Recycler.Handle<ThreadLocalUnsafeDirectByteBuf> handle;
    
    static ThreadLocalUnsafeDirectByteBuf newInstance()
    {
      ThreadLocalUnsafeDirectByteBuf buf = (ThreadLocalUnsafeDirectByteBuf)RECYCLER.get();
      buf.setRefCnt(1);
      return buf;
    }
    
    private ThreadLocalUnsafeDirectByteBuf(Recycler.Handle<ThreadLocalUnsafeDirectByteBuf> handle)
    {
      super(256, Integer.MAX_VALUE);
      this.handle = handle;
    }
    
    protected void deallocate()
    {
      if (capacity() > ThreadLocalPooledDirectByteBuf.threadLocalDirectBufferSize)
      {
        super.deallocate();
      }
      else
      {
        clear();
        RECYCLER.recycle(this, this.handle);
      }
    }
  }
  
  static final class ThreadLocalDirectByteBuf
    extends UnpooledDirectByteBuf
  {
    private static final Recycler<ThreadLocalDirectByteBuf> RECYCLER = new Recycler()
    {
      protected ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf newObject(Recycler.Handle<ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf> handle)
      {
        return new ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf(handle, null);
      }
    };
    private final Recycler.Handle<ThreadLocalDirectByteBuf> handle;
    
    static ThreadLocalDirectByteBuf newInstance()
    {
      ThreadLocalDirectByteBuf buf = (ThreadLocalDirectByteBuf)RECYCLER.get();
      buf.setRefCnt(1);
      return buf;
    }
    
    private ThreadLocalDirectByteBuf(Recycler.Handle<ThreadLocalDirectByteBuf> handle)
    {
      super(256, Integer.MAX_VALUE);
      this.handle = handle;
    }
    
    protected void deallocate()
    {
      if (capacity() > ThreadLocalPooledDirectByteBuf.threadLocalDirectBufferSize)
      {
        super.deallocate();
      }
      else
      {
        clear();
        RECYCLER.recycle(this, this.handle);
      }
    }
  }
}
