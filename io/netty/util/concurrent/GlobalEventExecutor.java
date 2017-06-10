package io.netty.util.concurrent;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GlobalEventExecutor
  extends AbstractScheduledEventExecutor
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(GlobalEventExecutor.class);
  private static final long SCHEDULE_PURGE_INTERVAL = TimeUnit.SECONDS.toNanos(1L);
  public static final GlobalEventExecutor INSTANCE = new GlobalEventExecutor();
  final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue();
  final ScheduledFutureTask<Void> purgeTask = new ScheduledFutureTask(this, Executors.callable(new PurgeTask(null), null), ScheduledFutureTask.deadlineNanos(SCHEDULE_PURGE_INTERVAL), -SCHEDULE_PURGE_INTERVAL);
  private final ThreadFactory threadFactory = new DefaultThreadFactory(getClass());
  private final TaskRunner taskRunner = new TaskRunner();
  private final AtomicBoolean started = new AtomicBoolean();
  volatile Thread thread;
  private final Future<?> terminationFuture = new FailedFuture(this, new UnsupportedOperationException());
  
  private GlobalEventExecutor()
  {
    scheduledTaskQueue().add(this.purgeTask);
  }
  
  Runnable takeTask()
  {
    BlockingQueue<Runnable> taskQueue = this.taskQueue;
    for (;;)
    {
      ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
      if (scheduledTask == null)
      {
        Runnable task = null;
        try
        {
          task = (Runnable)taskQueue.take();
        }
        catch (InterruptedException e) {}
        return task;
      }
      long delayNanos = scheduledTask.delayNanos();
      Runnable task;
      if (delayNanos > 0L) {
        try
        {
          task = (Runnable)taskQueue.poll(delayNanos, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e)
        {
          return null;
        }
      } else {
        task = (Runnable)taskQueue.poll();
      }
      if (task == null)
      {
        fetchFromScheduledTaskQueue();
        task = (Runnable)taskQueue.poll();
      }
      if (task != null) {
        return task;
      }
    }
  }
  
  private void fetchFromScheduledTaskQueue()
  {
    if (hasScheduledTasks())
    {
      long nanoTime = AbstractScheduledEventExecutor.nanoTime();
      for (;;)
      {
        Runnable scheduledTask = pollScheduledTask(nanoTime);
        if (scheduledTask == null) {
          break;
        }
        this.taskQueue.add(scheduledTask);
      }
    }
  }
  
  public int pendingTasks()
  {
    return this.taskQueue.size();
  }
  
  private void addTask(Runnable task)
  {
    if (task == null) {
      throw new NullPointerException("task");
    }
    this.taskQueue.add(task);
  }
  
  public boolean inEventLoop(Thread thread)
  {
    return thread == this.thread;
  }
  
  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
  {
    return terminationFuture();
  }
  
  public Future<?> terminationFuture()
  {
    return this.terminationFuture;
  }
  
  @Deprecated
  public void shutdown()
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean isShuttingDown()
  {
    return false;
  }
  
  public boolean isShutdown()
  {
    return false;
  }
  
  public boolean isTerminated()
  {
    return false;
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit)
  {
    return false;
  }
  
  public boolean awaitInactivity(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    if (unit == null) {
      throw new NullPointerException("unit");
    }
    Thread thread = this.thread;
    if (thread == null) {
      throw new IllegalStateException("thread was not started");
    }
    thread.join(unit.toMillis(timeout));
    return !thread.isAlive();
  }
  
  public void execute(Runnable task)
  {
    if (task == null) {
      throw new NullPointerException("task");
    }
    addTask(task);
    if (!inEventLoop()) {
      startThread();
    }
  }
  
  private void startThread()
  {
    if (this.started.compareAndSet(false, true))
    {
      Thread t = this.threadFactory.newThread(this.taskRunner);
      t.start();
      this.thread = t;
    }
  }
  
  final class TaskRunner
    implements Runnable
  {
    TaskRunner() {}
    
    public void run()
    {
      for (;;)
      {
        Runnable task = GlobalEventExecutor.this.takeTask();
        if (task != null)
        {
          try
          {
            task.run();
          }
          catch (Throwable t)
          {
            GlobalEventExecutor.logger.warn("Unexpected exception from the global event executor: ", t);
          }
          if (task != GlobalEventExecutor.this.purgeTask) {}
        }
        else
        {
          Queue<ScheduledFutureTask<?>> scheduledTaskQueue = GlobalEventExecutor.this.scheduledTaskQueue;
          if ((GlobalEventExecutor.this.taskQueue.isEmpty()) && ((scheduledTaskQueue == null) || (scheduledTaskQueue.size() == 1)))
          {
            boolean stopped = GlobalEventExecutor.this.started.compareAndSet(true, false);
            assert (stopped);
            if ((GlobalEventExecutor.this.taskQueue.isEmpty()) && ((scheduledTaskQueue == null) || (scheduledTaskQueue.size() == 1))) {
              break;
            }
            if (!GlobalEventExecutor.this.started.compareAndSet(false, true)) {
              break;
            }
          }
        }
      }
    }
  }
  
  private final class PurgeTask
    implements Runnable
  {
    private PurgeTask() {}
    
    public void run()
    {
      GlobalEventExecutor.this.purgeCancelledScheduledTasks();
    }
  }
}
