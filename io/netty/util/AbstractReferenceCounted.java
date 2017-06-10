package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class AbstractReferenceCounted
  implements ReferenceCounted
{
  private static final AtomicIntegerFieldUpdater<AbstractReferenceCounted> refCntUpdater;
  
  static
  {
    AtomicIntegerFieldUpdater<AbstractReferenceCounted> updater = PlatformDependent.newAtomicIntegerFieldUpdater(AbstractReferenceCounted.class, "refCnt");
    if (updater == null) {
      updater = AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCounted.class, "refCnt");
    }
    refCntUpdater = updater;
  }
  
  private volatile int refCnt = 1;
  
  public final int refCnt()
  {
    return this.refCnt;
  }
  
  protected final void setRefCnt(int refCnt)
  {
    this.refCnt = refCnt;
  }
  
  public ReferenceCounted retain()
  {
    for (;;)
    {
      int refCnt = this.refCnt;
      if (refCnt == 0) {
        throw new IllegalReferenceCountException(0, 1);
      }
      if (refCnt == Integer.MAX_VALUE) {
        throw new IllegalReferenceCountException(Integer.MAX_VALUE, 1);
      }
      if (refCntUpdater.compareAndSet(this, refCnt, refCnt + 1)) {
        break;
      }
    }
    return this;
  }
  
  public ReferenceCounted retain(int increment)
  {
    if (increment <= 0) {
      throw new IllegalArgumentException("increment: " + increment + " (expected: > 0)");
    }
    for (;;)
    {
      int refCnt = this.refCnt;
      if (refCnt == 0) {
        throw new IllegalReferenceCountException(0, 1);
      }
      if (refCnt > Integer.MAX_VALUE - increment) {
        throw new IllegalReferenceCountException(refCnt, increment);
      }
      if (refCntUpdater.compareAndSet(this, refCnt, refCnt + increment)) {
        break;
      }
    }
    return this;
  }
  
  public ReferenceCounted touch()
  {
    return touch(null);
  }
  
  public boolean release()
  {
    for (;;)
    {
      int refCnt = this.refCnt;
      if (refCnt == 0) {
        throw new IllegalReferenceCountException(0, -1);
      }
      if (refCntUpdater.compareAndSet(this, refCnt, refCnt - 1))
      {
        if (refCnt == 1)
        {
          deallocate();
          return true;
        }
        return false;
      }
    }
  }
  
  public boolean release(int decrement)
  {
    if (decrement <= 0) {
      throw new IllegalArgumentException("decrement: " + decrement + " (expected: > 0)");
    }
    for (;;)
    {
      int refCnt = this.refCnt;
      if (refCnt < decrement) {
        throw new IllegalReferenceCountException(refCnt, -decrement);
      }
      if (refCntUpdater.compareAndSet(this, refCnt, refCnt - decrement))
      {
        if (refCnt == decrement)
        {
          deallocate();
          return true;
        }
        return false;
      }
    }
  }
  
  protected abstract void deallocate();
}
