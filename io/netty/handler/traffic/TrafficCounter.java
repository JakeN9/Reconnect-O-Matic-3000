package io.netty.handler.traffic;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TrafficCounter
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(TrafficCounter.class);
  
  public static long milliSecondFromNano()
  {
    return System.nanoTime() / 1000000L;
  }
  
  private final AtomicLong currentWrittenBytes = new AtomicLong();
  private final AtomicLong currentReadBytes = new AtomicLong();
  private long writingTime;
  private long readingTime;
  private final AtomicLong cumulativeWrittenBytes = new AtomicLong();
  private final AtomicLong cumulativeReadBytes = new AtomicLong();
  private long lastCumulativeTime;
  private long lastWriteThroughput;
  private long lastReadThroughput;
  final AtomicLong lastTime = new AtomicLong();
  private volatile long lastWrittenBytes;
  private volatile long lastReadBytes;
  private volatile long lastWritingTime;
  private volatile long lastReadingTime;
  private final AtomicLong realWrittenBytes = new AtomicLong();
  private long realWriteThroughput;
  final AtomicLong checkInterval = new AtomicLong(1000L);
  final String name;
  final AbstractTrafficShapingHandler trafficShapingHandler;
  final ScheduledExecutorService executor;
  Runnable monitor;
  volatile ScheduledFuture<?> scheduledFuture;
  volatile boolean monitorActive;
  
  private static class TrafficMonitoringTask
    implements Runnable
  {
    private final AbstractTrafficShapingHandler trafficShapingHandler1;
    private final TrafficCounter counter;
    
    protected TrafficMonitoringTask(AbstractTrafficShapingHandler trafficShapingHandler, TrafficCounter counter)
    {
      this.trafficShapingHandler1 = trafficShapingHandler;
      this.counter = counter;
    }
    
    public void run()
    {
      if (!this.counter.monitorActive) {
        return;
      }
      this.counter.resetAccounting(TrafficCounter.milliSecondFromNano());
      if (this.trafficShapingHandler1 != null) {
        this.trafficShapingHandler1.doAccounting(this.counter);
      }
      this.counter.scheduledFuture = this.counter.executor.schedule(this, this.counter.checkInterval.get(), TimeUnit.MILLISECONDS);
    }
  }
  
  public synchronized void start()
  {
    if (this.monitorActive) {
      return;
    }
    this.lastTime.set(milliSecondFromNano());
    long localCheckInterval = this.checkInterval.get();
    if ((localCheckInterval > 0L) && (this.executor != null))
    {
      this.monitorActive = true;
      this.monitor = new TrafficMonitoringTask(this.trafficShapingHandler, this);
      this.scheduledFuture = this.executor.schedule(this.monitor, localCheckInterval, TimeUnit.MILLISECONDS);
    }
  }
  
  public synchronized void stop()
  {
    if (!this.monitorActive) {
      return;
    }
    this.monitorActive = false;
    resetAccounting(milliSecondFromNano());
    if (this.trafficShapingHandler != null) {
      this.trafficShapingHandler.doAccounting(this);
    }
    if (this.scheduledFuture != null) {
      this.scheduledFuture.cancel(true);
    }
  }
  
  synchronized void resetAccounting(long newLastTime)
  {
    long interval = newLastTime - this.lastTime.getAndSet(newLastTime);
    if (interval == 0L) {
      return;
    }
    if ((logger.isDebugEnabled()) && (interval > checkInterval() << 1)) {
      logger.debug("Acct schedule not ok: " + interval + " > 2*" + checkInterval() + " from " + this.name);
    }
    this.lastReadBytes = this.currentReadBytes.getAndSet(0L);
    this.lastWrittenBytes = this.currentWrittenBytes.getAndSet(0L);
    this.lastReadThroughput = (this.lastReadBytes * 1000L / interval);
    
    this.lastWriteThroughput = (this.lastWrittenBytes * 1000L / interval);
    
    this.realWriteThroughput = (this.realWrittenBytes.getAndSet(0L) * 1000L / interval);
    this.lastWritingTime = Math.max(this.lastWritingTime, this.writingTime);
    this.lastReadingTime = Math.max(this.lastReadingTime, this.readingTime);
  }
  
  public TrafficCounter(AbstractTrafficShapingHandler trafficShapingHandler, ScheduledExecutorService executor, String name, long checkInterval)
  {
    if (trafficShapingHandler == null) {
      throw new IllegalArgumentException("TrafficShapingHandler must not be null");
    }
    this.trafficShapingHandler = trafficShapingHandler;
    this.executor = executor;
    this.name = name;
    
    this.lastCumulativeTime = System.currentTimeMillis();
    this.writingTime = milliSecondFromNano();
    this.readingTime = this.writingTime;
    this.lastWritingTime = this.writingTime;
    this.lastReadingTime = this.writingTime;
    configure(checkInterval);
  }
  
  public void configure(long newcheckInterval)
  {
    long newInterval = newcheckInterval / 10L * 10L;
    if (this.checkInterval.getAndSet(newInterval) != newInterval) {
      if (newInterval <= 0L)
      {
        stop();
        
        this.lastTime.set(milliSecondFromNano());
      }
      else
      {
        start();
      }
    }
  }
  
  void bytesRecvFlowControl(long recv)
  {
    this.currentReadBytes.addAndGet(recv);
    this.cumulativeReadBytes.addAndGet(recv);
  }
  
  void bytesWriteFlowControl(long write)
  {
    this.currentWrittenBytes.addAndGet(write);
    this.cumulativeWrittenBytes.addAndGet(write);
  }
  
  void bytesRealWriteFlowControl(long write)
  {
    this.realWrittenBytes.addAndGet(write);
  }
  
  public long checkInterval()
  {
    return this.checkInterval.get();
  }
  
  public long lastReadThroughput()
  {
    return this.lastReadThroughput;
  }
  
  public long lastWriteThroughput()
  {
    return this.lastWriteThroughput;
  }
  
  public long lastReadBytes()
  {
    return this.lastReadBytes;
  }
  
  public long lastWrittenBytes()
  {
    return this.lastWrittenBytes;
  }
  
  public long currentReadBytes()
  {
    return this.currentReadBytes.get();
  }
  
  public long currentWrittenBytes()
  {
    return this.currentWrittenBytes.get();
  }
  
  public long lastTime()
  {
    return this.lastTime.get();
  }
  
  public long cumulativeWrittenBytes()
  {
    return this.cumulativeWrittenBytes.get();
  }
  
  public long cumulativeReadBytes()
  {
    return this.cumulativeReadBytes.get();
  }
  
  public long lastCumulativeTime()
  {
    return this.lastCumulativeTime;
  }
  
  public AtomicLong getRealWrittenBytes()
  {
    return this.realWrittenBytes;
  }
  
  public long getRealWriteThroughput()
  {
    return this.realWriteThroughput;
  }
  
  public void resetCumulativeTime()
  {
    this.lastCumulativeTime = System.currentTimeMillis();
    this.cumulativeReadBytes.set(0L);
    this.cumulativeWrittenBytes.set(0L);
  }
  
  public String name()
  {
    return this.name;
  }
  
  @Deprecated
  public long readTimeToWait(long size, long limitTraffic, long maxTime)
  {
    return readTimeToWait(size, limitTraffic, maxTime, milliSecondFromNano());
  }
  
  public long readTimeToWait(long size, long limitTraffic, long maxTime, long now)
  {
    bytesRecvFlowControl(size);
    if ((size == 0L) || (limitTraffic == 0L)) {
      return 0L;
    }
    long lastTimeCheck = this.lastTime.get();
    long sum = this.currentReadBytes.get();
    long localReadingTime = this.readingTime;
    long lastRB = this.lastReadBytes;
    long interval = now - lastTimeCheck;
    long pastDelay = Math.max(this.lastReadingTime - lastTimeCheck, 0L);
    if (interval > 10L)
    {
      long time = sum * 1000L / limitTraffic - interval + pastDelay;
      if (time > 10L)
      {
        if (logger.isDebugEnabled()) {
          logger.debug("Time: " + time + ':' + sum + ':' + interval + ':' + pastDelay);
        }
        if ((time > maxTime) && (now + time - localReadingTime > maxTime)) {
          time = maxTime;
        }
        this.readingTime = Math.max(localReadingTime, now + time);
        return time;
      }
      this.readingTime = Math.max(localReadingTime, now);
      return 0L;
    }
    long lastsum = sum + lastRB;
    long lastinterval = interval + this.checkInterval.get();
    long time = lastsum * 1000L / limitTraffic - lastinterval + pastDelay;
    if (time > 10L)
    {
      if (logger.isDebugEnabled()) {
        logger.debug("Time: " + time + ':' + lastsum + ':' + lastinterval + ':' + pastDelay);
      }
      if ((time > maxTime) && (now + time - localReadingTime > maxTime)) {
        time = maxTime;
      }
      this.readingTime = Math.max(localReadingTime, now + time);
      return time;
    }
    this.readingTime = Math.max(localReadingTime, now);
    return 0L;
  }
  
  @Deprecated
  public long writeTimeToWait(long size, long limitTraffic, long maxTime)
  {
    return writeTimeToWait(size, limitTraffic, maxTime, milliSecondFromNano());
  }
  
  public long writeTimeToWait(long size, long limitTraffic, long maxTime, long now)
  {
    bytesWriteFlowControl(size);
    if ((size == 0L) || (limitTraffic == 0L)) {
      return 0L;
    }
    long lastTimeCheck = this.lastTime.get();
    long sum = this.currentWrittenBytes.get();
    long lastWB = this.lastWrittenBytes;
    long localWritingTime = this.writingTime;
    long pastDelay = Math.max(this.lastWritingTime - lastTimeCheck, 0L);
    long interval = now - lastTimeCheck;
    if (interval > 10L)
    {
      long time = sum * 1000L / limitTraffic - interval + pastDelay;
      if (time > 10L)
      {
        if (logger.isDebugEnabled()) {
          logger.debug("Time: " + time + ':' + sum + ':' + interval + ':' + pastDelay);
        }
        if ((time > maxTime) && (now + time - localWritingTime > maxTime)) {
          time = maxTime;
        }
        this.writingTime = Math.max(localWritingTime, now + time);
        return time;
      }
      this.writingTime = Math.max(localWritingTime, now);
      return 0L;
    }
    long lastsum = sum + lastWB;
    long lastinterval = interval + this.checkInterval.get();
    long time = lastsum * 1000L / limitTraffic - lastinterval + pastDelay;
    if (time > 10L)
    {
      if (logger.isDebugEnabled()) {
        logger.debug("Time: " + time + ':' + lastsum + ':' + lastinterval + ':' + pastDelay);
      }
      if ((time > maxTime) && (now + time - localWritingTime > maxTime)) {
        time = maxTime;
      }
      this.writingTime = Math.max(localWritingTime, now + time);
      return time;
    }
    this.writingTime = Math.max(localWritingTime, now);
    return 0L;
  }
  
  public String toString()
  {
    return 165 + "Monitor " + this.name + " Current Speed Read: " + (this.lastReadThroughput >> 10) + " KB/s, " + "Asked Write: " + (this.lastWriteThroughput >> 10) + " KB/s, " + "Real Write: " + (this.realWriteThroughput >> 10) + " KB/s, " + "Current Read: " + (this.currentReadBytes.get() >> 10) + " KB, " + "Current asked Write: " + (this.currentWrittenBytes.get() >> 10) + " KB, " + "Current real Write: " + (this.realWrittenBytes.get() >> 10) + " KB";
  }
}
