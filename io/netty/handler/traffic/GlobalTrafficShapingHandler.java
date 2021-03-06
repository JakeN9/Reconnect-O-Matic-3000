package io.netty.handler.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.PlatformDependent;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ChannelHandler.Sharable
public class GlobalTrafficShapingHandler
  extends AbstractTrafficShapingHandler
{
  private final ConcurrentMap<Integer, PerChannel> channelQueues = PlatformDependent.newConcurrentHashMap();
  private final AtomicLong queuesSize = new AtomicLong();
  long maxGlobalWriteSize = 419430400L;
  
  void createGlobalTrafficCounter(ScheduledExecutorService executor)
  {
    if (executor == null) {
      throw new NullPointerException("executor");
    }
    TrafficCounter tc = new TrafficCounter(this, executor, "GlobalTC", this.checkInterval);
    setTrafficCounter(tc);
    tc.start();
  }
  
  public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit, long checkInterval, long maxTime)
  {
    super(writeLimit, readLimit, checkInterval, maxTime);
    createGlobalTrafficCounter(executor);
  }
  
  public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit, long checkInterval)
  {
    super(writeLimit, readLimit, checkInterval);
    createGlobalTrafficCounter(executor);
  }
  
  public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit)
  {
    super(writeLimit, readLimit);
    createGlobalTrafficCounter(executor);
  }
  
  public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long checkInterval)
  {
    super(checkInterval);
    createGlobalTrafficCounter(executor);
  }
  
  public GlobalTrafficShapingHandler(EventExecutor executor)
  {
    createGlobalTrafficCounter(executor);
  }
  
  public long getMaxGlobalWriteSize()
  {
    return this.maxGlobalWriteSize;
  }
  
  public void setMaxGlobalWriteSize(long maxGlobalWriteSize)
  {
    this.maxGlobalWriteSize = maxGlobalWriteSize;
  }
  
  public long queuesSize()
  {
    return this.queuesSize.get();
  }
  
  public final void release()
  {
    this.trafficCounter.stop();
  }
  
  private PerChannel getOrSetPerChannel(ChannelHandlerContext ctx)
  {
    Channel channel = ctx.channel();
    Integer key = Integer.valueOf(channel.hashCode());
    PerChannel perChannel = (PerChannel)this.channelQueues.get(key);
    if (perChannel == null)
    {
      perChannel = new PerChannel(null);
      perChannel.messagesQueue = new ArrayDeque();
      perChannel.queueSize = 0L;
      perChannel.lastReadTimestamp = TrafficCounter.milliSecondFromNano();
      perChannel.lastWriteTimestamp = perChannel.lastReadTimestamp;
      this.channelQueues.put(key, perChannel);
    }
    return perChannel;
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {
    getOrSetPerChannel(ctx);
    super.handlerAdded(ctx);
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {
    Channel channel = ctx.channel();
    Integer key = Integer.valueOf(channel.hashCode());
    PerChannel perChannel = (PerChannel)this.channelQueues.remove(key);
    if (perChannel != null) {
      synchronized (perChannel)
      {
        if (channel.isActive())
        {
          for (ToSend toSend : perChannel.messagesQueue)
          {
            long size = calculateSize(toSend.toSend);
            this.trafficCounter.bytesRealWriteFlowControl(size);
            perChannel.queueSize -= size;
            this.queuesSize.addAndGet(-size);
            ctx.write(toSend.toSend, toSend.promise);
          }
        }
        else
        {
          this.queuesSize.addAndGet(-perChannel.queueSize);
          for (ToSend toSend : perChannel.messagesQueue) {
            if ((toSend.toSend instanceof ByteBuf)) {
              ((ByteBuf)toSend.toSend).release();
            }
          }
        }
        perChannel.messagesQueue.clear();
      }
    }
    releaseWriteSuspended(ctx);
    releaseReadSuspended(ctx);
    super.handlerRemoved(ctx);
  }
  
  long checkWaitReadTime(ChannelHandlerContext ctx, long wait, long now)
  {
    Integer key = Integer.valueOf(ctx.channel().hashCode());
    PerChannel perChannel = (PerChannel)this.channelQueues.get(key);
    if ((perChannel != null) && 
      (wait > this.maxTime) && (now + wait - perChannel.lastReadTimestamp > this.maxTime)) {
      wait = this.maxTime;
    }
    return wait;
  }
  
  void informReadOperation(ChannelHandlerContext ctx, long now)
  {
    Integer key = Integer.valueOf(ctx.channel().hashCode());
    PerChannel perChannel = (PerChannel)this.channelQueues.get(key);
    if (perChannel != null) {
      perChannel.lastReadTimestamp = now;
    }
  }
  
  private static final class ToSend
  {
    final long relativeTimeAction;
    final Object toSend;
    final long size;
    final ChannelPromise promise;
    
    private ToSend(long delay, Object toSend, long size, ChannelPromise promise)
    {
      this.relativeTimeAction = delay;
      this.toSend = toSend;
      this.size = size;
      this.promise = promise;
    }
  }
  
  void submitWrite(final ChannelHandlerContext ctx, Object msg, long size, long writedelay, long now, ChannelPromise promise)
  {
    Channel channel = ctx.channel();
    Integer key = Integer.valueOf(channel.hashCode());
    PerChannel perChannel = (PerChannel)this.channelQueues.get(key);
    if (perChannel == null) {
      perChannel = getOrSetPerChannel(ctx);
    }
    long delay = writedelay;
    boolean globalSizeExceeded = false;
    ToSend newToSend;
    synchronized (perChannel)
    {
      if ((writedelay == 0L) && (perChannel.messagesQueue.isEmpty()))
      {
        this.trafficCounter.bytesRealWriteFlowControl(size);
        ctx.write(msg, promise);
        perChannel.lastWriteTimestamp = now;
        return;
      }
      if ((delay > this.maxTime) && (now + delay - perChannel.lastWriteTimestamp > this.maxTime)) {
        delay = this.maxTime;
      }
      newToSend = new ToSend(delay + now, msg, size, promise, null);
      perChannel.messagesQueue.addLast(newToSend);
      perChannel.queueSize += size;
      this.queuesSize.addAndGet(size);
      checkWriteSuspend(ctx, delay, perChannel.queueSize);
      if (this.queuesSize.get() > this.maxGlobalWriteSize) {
        globalSizeExceeded = true;
      }
    }
    if (globalSizeExceeded) {
      setUserDefinedWritability(ctx, false);
    }
    final long futureNow = newToSend.relativeTimeAction;
    final PerChannel forSchedule = perChannel;
    ctx.executor().schedule(new Runnable()
    {
      public void run()
      {
        GlobalTrafficShapingHandler.this.sendAllValid(ctx, forSchedule, futureNow);
      }
    }, delay, TimeUnit.MILLISECONDS);
  }
  
  private void sendAllValid(ChannelHandlerContext ctx, PerChannel perChannel, long now)
  {
    synchronized (perChannel)
    {
      for (ToSend newToSend = (ToSend)perChannel.messagesQueue.pollFirst(); newToSend != null; newToSend = (ToSend)perChannel.messagesQueue.pollFirst()) {
        if (newToSend.relativeTimeAction <= now)
        {
          long size = newToSend.size;
          this.trafficCounter.bytesRealWriteFlowControl(size);
          perChannel.queueSize -= size;
          this.queuesSize.addAndGet(-size);
          ctx.write(newToSend.toSend, newToSend.promise);
          perChannel.lastWriteTimestamp = now;
        }
        else
        {
          perChannel.messagesQueue.addFirst(newToSend);
          break;
        }
      }
      if (perChannel.messagesQueue.isEmpty()) {
        releaseWriteSuspended(ctx);
      }
    }
    ctx.flush();
  }
  
  private static final class PerChannel
  {
    ArrayDeque<GlobalTrafficShapingHandler.ToSend> messagesQueue;
    long queueSize;
    long lastWriteTimestamp;
    long lastReadTimestamp;
  }
}
