package io.netty.channel;

import java.util.ArrayDeque;
import java.util.Queue;

public final class ChannelFlushPromiseNotifier
{
  private long writeCounter;
  private final Queue<FlushCheckpoint> flushCheckpoints = new ArrayDeque();
  private final boolean tryNotify;
  
  public ChannelFlushPromiseNotifier(boolean tryNotify)
  {
    this.tryNotify = tryNotify;
  }
  
  public ChannelFlushPromiseNotifier()
  {
    this(false);
  }
  
  public ChannelFlushPromiseNotifier add(ChannelPromise promise, long pendingDataSize)
  {
    if (promise == null) {
      throw new NullPointerException("promise");
    }
    if (pendingDataSize < 0L) {
      throw new IllegalArgumentException("pendingDataSize must be >= 0 but was " + pendingDataSize);
    }
    long checkpoint = this.writeCounter + pendingDataSize;
    if ((promise instanceof FlushCheckpoint))
    {
      FlushCheckpoint cp = (FlushCheckpoint)promise;
      cp.flushCheckpoint(checkpoint);
      this.flushCheckpoints.add(cp);
    }
    else
    {
      this.flushCheckpoints.add(new DefaultFlushCheckpoint(checkpoint, promise));
    }
    return this;
  }
  
  public ChannelFlushPromiseNotifier increaseWriteCounter(long delta)
  {
    if (delta < 0L) {
      throw new IllegalArgumentException("delta must be >= 0 but was " + delta);
    }
    this.writeCounter += delta;
    return this;
  }
  
  public long writeCounter()
  {
    return this.writeCounter;
  }
  
  public ChannelFlushPromiseNotifier notifyPromises()
  {
    notifyPromises0(null);
    return this;
  }
  
  public ChannelFlushPromiseNotifier notifyPromises(Throwable cause)
  {
    notifyPromises();
    for (;;)
    {
      FlushCheckpoint cp = (FlushCheckpoint)this.flushCheckpoints.poll();
      if (cp == null) {
        break;
      }
      if (this.tryNotify) {
        cp.promise().tryFailure(cause);
      } else {
        cp.promise().setFailure(cause);
      }
    }
    return this;
  }
  
  public ChannelFlushPromiseNotifier notifyPromises(Throwable cause1, Throwable cause2)
  {
    notifyPromises0(cause1);
    for (;;)
    {
      FlushCheckpoint cp = (FlushCheckpoint)this.flushCheckpoints.poll();
      if (cp == null) {
        break;
      }
      if (this.tryNotify) {
        cp.promise().tryFailure(cause2);
      } else {
        cp.promise().setFailure(cause2);
      }
    }
    return this;
  }
  
  @Deprecated
  public ChannelFlushPromiseNotifier notifyFlushFutures(Throwable cause1, Throwable cause2)
  {
    return notifyPromises(cause1, cause2);
  }
  
  private void notifyPromises0(Throwable cause)
  {
    if (this.flushCheckpoints.isEmpty())
    {
      this.writeCounter = 0L;
      return;
    }
    long writeCounter = this.writeCounter;
    for (;;)
    {
      FlushCheckpoint cp = (FlushCheckpoint)this.flushCheckpoints.peek();
      if (cp == null)
      {
        this.writeCounter = 0L;
        break;
      }
      if (cp.flushCheckpoint() > writeCounter)
      {
        if ((writeCounter <= 0L) || (this.flushCheckpoints.size() != 1)) {
          break;
        }
        this.writeCounter = 0L;
        cp.flushCheckpoint(cp.flushCheckpoint() - writeCounter); break;
      }
      this.flushCheckpoints.remove();
      ChannelPromise promise = cp.promise();
      if (cause == null)
      {
        if (this.tryNotify) {
          promise.trySuccess();
        } else {
          promise.setSuccess();
        }
      }
      else if (this.tryNotify) {
        promise.tryFailure(cause);
      } else {
        promise.setFailure(cause);
      }
    }
    long newWriteCounter = this.writeCounter;
    if (newWriteCounter >= 549755813888L)
    {
      this.writeCounter = 0L;
      for (FlushCheckpoint cp : this.flushCheckpoints) {
        cp.flushCheckpoint(cp.flushCheckpoint() - newWriteCounter);
      }
    }
  }
  
  private static class DefaultFlushCheckpoint
    implements ChannelFlushPromiseNotifier.FlushCheckpoint
  {
    private long checkpoint;
    private final ChannelPromise future;
    
    DefaultFlushCheckpoint(long checkpoint, ChannelPromise future)
    {
      this.checkpoint = checkpoint;
      this.future = future;
    }
    
    public long flushCheckpoint()
    {
      return this.checkpoint;
    }
    
    public void flushCheckpoint(long checkpoint)
    {
      this.checkpoint = checkpoint;
    }
    
    public ChannelPromise promise()
    {
      return this.future;
    }
  }
  
  static abstract interface FlushCheckpoint
  {
    public abstract long flushCheckpoint();
    
    public abstract void flushCheckpoint(long paramLong);
    
    public abstract ChannelPromise promise();
  }
}
