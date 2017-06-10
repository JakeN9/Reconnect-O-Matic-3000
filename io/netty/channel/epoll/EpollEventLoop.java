package io.netty.channel.epoll;

import io.netty.channel.Channel.Unsafe;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.collection.IntObjectMap.Entry;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

final class EpollEventLoop
  extends SingleThreadEventLoop
{
  private static final InternalLogger logger;
  private static final AtomicIntegerFieldUpdater<EpollEventLoop> WAKEN_UP_UPDATER;
  private final int epollFd;
  private final int eventFd;
  
  static
  {
    logger = InternalLoggerFactory.getInstance(EpollEventLoop.class);
    
    AtomicIntegerFieldUpdater<EpollEventLoop> updater = PlatformDependent.newAtomicIntegerFieldUpdater(EpollEventLoop.class, "wakenUp");
    if (updater == null) {
      updater = AtomicIntegerFieldUpdater.newUpdater(EpollEventLoop.class, "wakenUp");
    }
    WAKEN_UP_UPDATER = updater;
  }
  
  private final IntObjectMap<AbstractEpollChannel> channels = new IntObjectHashMap(4096);
  private final boolean allowGrowing;
  private final EpollEventArray events;
  private volatile int wakenUp;
  private volatile int ioRatio = 50;
  
  EpollEventLoop(EventLoopGroup parent, Executor executor, int maxEvents)
  {
    super(parent, executor, false);
    if (maxEvents == 0)
    {
      this.allowGrowing = true;
      this.events = new EpollEventArray(4096);
    }
    else
    {
      this.allowGrowing = false;
      this.events = new EpollEventArray(maxEvents);
    }
    boolean success = false;
    int epollFd = -1;
    int eventFd = -1;
    try
    {
      this.epollFd = (epollFd = Native.epollCreate());
      this.eventFd = (eventFd = Native.eventFd());
      Native.epollCtlAdd(epollFd, eventFd, Native.EPOLLIN);
      success = true; return;
    }
    finally
    {
      if (!success)
      {
        if (epollFd != -1) {
          try
          {
            Native.close(epollFd);
          }
          catch (Exception e) {}
        }
        if (eventFd != -1) {
          try
          {
            Native.close(eventFd);
          }
          catch (Exception e) {}
        }
      }
    }
  }
  
  protected void wakeup(boolean inEventLoop)
  {
    if ((!inEventLoop) && (WAKEN_UP_UPDATER.compareAndSet(this, 0, 1))) {
      Native.eventFdWrite(this.eventFd, 1L);
    }
  }
  
  void add(AbstractEpollChannel ch)
  {
    assert (inEventLoop());
    int fd = ch.fd().intValue();
    Native.epollCtlAdd(this.epollFd, fd, ch.flags);
    this.channels.put(fd, ch);
  }
  
  void modify(AbstractEpollChannel ch)
  {
    assert (inEventLoop());
    Native.epollCtlMod(this.epollFd, ch.fd().intValue(), ch.flags);
  }
  
  void remove(AbstractEpollChannel ch)
  {
    assert (inEventLoop());
    if (ch.isOpen())
    {
      int fd = ch.fd().intValue();
      if (this.channels.remove(fd) != null) {
        Native.epollCtlDel(this.epollFd, ch.fd().intValue());
      }
    }
  }
  
  protected Queue<Runnable> newTaskQueue()
  {
    return PlatformDependent.newMpscQueue();
  }
  
  public int getIoRatio()
  {
    return this.ioRatio;
  }
  
  public void setIoRatio(int ioRatio)
  {
    if ((ioRatio <= 0) || (ioRatio > 100)) {
      throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
    }
    this.ioRatio = ioRatio;
  }
  
  private int epollWait(boolean oldWakenUp)
    throws IOException
  {
    int selectCnt = 0;
    long currentTimeNanos = System.nanoTime();
    long selectDeadLineNanos = currentTimeNanos + delayNanos(currentTimeNanos);
    for (;;)
    {
      long timeoutMillis = (selectDeadLineNanos - currentTimeNanos + 500000L) / 1000000L;
      if (timeoutMillis <= 0L)
      {
        if (selectCnt != 0) {
          break;
        }
        int ready = Native.epollWait(this.epollFd, this.events, 0);
        if (ready > 0) {
          return ready;
        }
        break;
      }
      int selectedKeys = Native.epollWait(this.epollFd, this.events, (int)timeoutMillis);
      selectCnt++;
      if ((selectedKeys != 0) || (oldWakenUp) || (this.wakenUp == 1) || (hasTasks()) || (hasScheduledTasks())) {
        return selectedKeys;
      }
      currentTimeNanos = System.nanoTime();
    }
    return 0;
  }
  
  protected void run()
  {
    boolean oldWakenUp = WAKEN_UP_UPDATER.getAndSet(this, 0) == 1;
    try
    {
      int ready;
      int ready;
      if (hasTasks())
      {
        ready = Native.epollWait(this.epollFd, this.events, 0);
      }
      else
      {
        ready = epollWait(oldWakenUp);
        if (this.wakenUp == 1) {
          Native.eventFdWrite(this.eventFd, 1L);
        }
      }
      int ioRatio = this.ioRatio;
      if (ioRatio == 100)
      {
        if (ready > 0) {
          processReady(this.events, ready);
        }
        runAllTasks();
      }
      else
      {
        long ioStartTime = System.nanoTime();
        if (ready > 0) {
          processReady(this.events, ready);
        }
        long ioTime = System.nanoTime() - ioStartTime;
        runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
      }
      if ((this.allowGrowing) && (ready == this.events.length())) {
        this.events.increase();
      }
      if (isShuttingDown())
      {
        closeAll();
        if (confirmShutdown())
        {
          cleanupAndTerminate(true);
          return;
        }
      }
    }
    catch (Throwable t)
    {
      logger.warn("Unexpected exception in the selector loop.", t);
      try
      {
        Thread.sleep(1000L);
      }
      catch (InterruptedException e) {}
    }
    scheduleExecution();
  }
  
  private void closeAll()
  {
    try
    {
      Native.epollWait(this.epollFd, this.events, 0);
    }
    catch (IOException ignore) {}
    Collection<AbstractEpollChannel> array = new ArrayList(this.channels.size());
    for (IntObjectMap.Entry<AbstractEpollChannel> entry : this.channels.entries()) {
      array.add(entry.value());
    }
    for (AbstractEpollChannel ch : array) {
      ch.unsafe().close(ch.unsafe().voidPromise());
    }
  }
  
  private void processReady(EpollEventArray events, int ready)
  {
    for (int i = 0; i < ready; i++)
    {
      int fd = events.fd(i);
      if (fd == this.eventFd)
      {
        Native.eventFdRead(this.eventFd);
      }
      else
      {
        long ev = events.events(i);
        
        AbstractEpollChannel ch = (AbstractEpollChannel)this.channels.get(fd);
        if ((ch != null) && (ch.isOpen()))
        {
          boolean close = (ev & Native.EPOLLRDHUP) != 0L;
          boolean read = (ev & Native.EPOLLIN) != 0L;
          boolean write = (ev & Native.EPOLLOUT) != 0L;
          
          AbstractEpollChannel.AbstractEpollUnsafe unsafe = (AbstractEpollChannel.AbstractEpollUnsafe)ch.unsafe();
          if (close) {
            unsafe.epollRdHupReady();
          }
          if ((write) && (ch.isOpen())) {
            unsafe.epollOutReady();
          }
          if ((read) && (ch.isOpen())) {
            unsafe.epollInReady();
          }
        }
        else
        {
          Native.epollCtlDel(this.epollFd, fd);
        }
      }
    }
  }
  
  protected void cleanup()
  {
    try
    {
      try
      {
        Native.close(this.epollFd);
      }
      catch (IOException e)
      {
        logger.warn("Failed to close the epoll fd.", e);
      }
      try
      {
        Native.close(this.eventFd);
      }
      catch (IOException e)
      {
        logger.warn("Failed to close the event fd.", e);
      }
    }
    finally
    {
      this.events.free();
    }
  }
}
