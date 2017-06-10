package io.netty.util.concurrent;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public abstract class SingleThreadEventExecutor
  extends AbstractScheduledEventExecutor
{
  private static final InternalLogger logger;
  private static final int ST_NOT_STARTED = 1;
  private static final int ST_STARTED = 2;
  private static final int ST_SHUTTING_DOWN = 3;
  private static final int ST_SHUTDOWN = 4;
  private static final int ST_TERMINATED = 5;
  private static final Runnable WAKEUP_TASK;
  private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER;
  private static final AtomicReferenceFieldUpdater<SingleThreadEventExecutor, Thread> THREAD_UPDATER;
  private final Queue<Runnable> taskQueue;
  private volatile Thread thread;
  private final Executor executor;
  
  static
  {
    logger = InternalLoggerFactory.getInstance(SingleThreadEventExecutor.class);
    
    WAKEUP_TASK = new Runnable()
    {
      public void run() {}
    };
    AtomicIntegerFieldUpdater<SingleThreadEventExecutor> updater = PlatformDependent.newAtomicIntegerFieldUpdater(SingleThreadEventExecutor.class, "state");
    if (updater == null) {
      updater = AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");
    }
    STATE_UPDATER = updater;
    
    AtomicReferenceFieldUpdater<SingleThreadEventExecutor, Thread> refUpdater = PlatformDependent.newAtomicReferenceFieldUpdater(SingleThreadEventExecutor.class, "thread");
    if (refUpdater == null) {
      refUpdater = AtomicReferenceFieldUpdater.newUpdater(SingleThreadEventExecutor.class, Thread.class, "thread");
    }
    THREAD_UPDATER = refUpdater;
  }
  
  private final Semaphore threadLock = new Semaphore(0);
  private final Set<Runnable> shutdownHooks = new LinkedHashSet();
  private final boolean addTaskWakesUp;
  private long lastExecutionTime;
  private volatile int state = 1;
  private volatile long gracefulShutdownQuietPeriod;
  private volatile long gracefulShutdownTimeout;
  private long gracefulShutdownStartTime;
  private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);
  private boolean firstRun = true;
  private final Runnable asRunnable = new Runnable()
  {
    public void run()
    {
      SingleThreadEventExecutor.this.updateThread(Thread.currentThread());
      if (SingleThreadEventExecutor.this.firstRun)
      {
        SingleThreadEventExecutor.this.firstRun = false;
        SingleThreadEventExecutor.this.updateLastExecutionTime();
      }
      try
      {
        SingleThreadEventExecutor.this.run();
      }
      catch (Throwable t)
      {
        SingleThreadEventExecutor.logger.warn("Unexpected exception from an event executor: ", t);
        SingleThreadEventExecutor.this.cleanupAndTerminate(false);
      }
    }
  };
  
  protected SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor, boolean addTaskWakesUp)
  {
    super(parent);
    if (executor == null) {
      throw new NullPointerException("executor");
    }
    this.addTaskWakesUp = addTaskWakesUp;
    this.executor = executor;
    this.taskQueue = newTaskQueue();
  }
  
  protected Queue<Runnable> newTaskQueue()
  {
    return new LinkedBlockingQueue();
  }
  
  protected Runnable pollTask()
  {
    assert (inEventLoop());
    Runnable task;
    do
    {
      task = (Runnable)this.taskQueue.poll();
    } while (task == WAKEUP_TASK);
    return task;
  }
  
  protected Runnable takeTask()
  {
    assert (inEventLoop());
    if (!(this.taskQueue instanceof BlockingQueue)) {
      throw new UnsupportedOperationException();
    }
    BlockingQueue<Runnable> taskQueue = (BlockingQueue)this.taskQueue;
    for (;;)
    {
      ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
      if (scheduledTask == null)
      {
        Runnable task = null;
        try
        {
          task = (Runnable)taskQueue.take();
          if (task == WAKEUP_TASK) {
            task = null;
          }
        }
        catch (InterruptedException e) {}
        return task;
      }
      long delayNanos = scheduledTask.delayNanos();
      Runnable task = null;
      if (delayNanos > 0L) {
        try
        {
          task = (Runnable)taskQueue.poll(delayNanos, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e)
        {
          return null;
        }
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
  
  protected Runnable peekTask()
  {
    assert (inEventLoop());
    return (Runnable)this.taskQueue.peek();
  }
  
  protected boolean hasTasks()
  {
    assert (inEventLoop());
    return !this.taskQueue.isEmpty();
  }
  
  public final int pendingTasks()
  {
    return this.taskQueue.size();
  }
  
  protected void addTask(Runnable task)
  {
    if (task == null) {
      throw new NullPointerException("task");
    }
    if (isShutdown()) {
      reject();
    }
    this.taskQueue.add(task);
  }
  
  protected boolean removeTask(Runnable task)
  {
    if (task == null) {
      throw new NullPointerException("task");
    }
    return this.taskQueue.remove(task);
  }
  
  protected boolean runAllTasks()
  {
    fetchFromScheduledTaskQueue();
    Runnable task = pollTask();
    if (task == null) {
      return false;
    }
    do
    {
      try
      {
        task.run();
      }
      catch (Throwable t)
      {
        logger.warn("A task raised an exception.", t);
      }
      task = pollTask();
    } while (task != null);
    this.lastExecutionTime = ScheduledFutureTask.nanoTime();
    return true;
  }
  
  protected boolean runAllTasks(long timeoutNanos)
  {
    fetchFromScheduledTaskQueue();
    Runnable task = pollTask();
    if (task == null) {
      return false;
    }
    long deadline = ScheduledFutureTask.nanoTime() + timeoutNanos;
    long runTasks = 0L;
    do
    {
      try
      {
        task.run();
      }
      catch (Throwable t)
      {
        logger.warn("A task raised an exception.", t);
      }
      runTasks += 1L;
      if ((runTasks & 0x3F) == 0L)
      {
        long lastExecutionTime = ScheduledFutureTask.nanoTime();
        if (lastExecutionTime >= deadline) {
          break;
        }
      }
      task = pollTask();
    } while (task != null);
    long lastExecutionTime = ScheduledFutureTask.nanoTime();
    
    this.lastExecutionTime = lastExecutionTime;
    return true;
  }
  
  protected long delayNanos(long currentTimeNanos)
  {
    ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
    if (scheduledTask == null) {
      return SCHEDULE_PURGE_INTERVAL;
    }
    return scheduledTask.delayNanos(currentTimeNanos);
  }
  
  protected void updateLastExecutionTime()
  {
    this.lastExecutionTime = ScheduledFutureTask.nanoTime();
  }
  
  protected void wakeup(boolean inEventLoop)
  {
    if ((!inEventLoop) || (STATE_UPDATER.get(this) == 3)) {
      this.taskQueue.add(WAKEUP_TASK);
    }
  }
  
  public boolean inEventLoop(Thread thread)
  {
    return thread == this.thread;
  }
  
  public void addShutdownHook(final Runnable task)
  {
    if (inEventLoop()) {
      this.shutdownHooks.add(task);
    } else {
      execute(new Runnable()
      {
        public void run()
        {
          SingleThreadEventExecutor.this.shutdownHooks.add(task);
        }
      });
    }
  }
  
  public void removeShutdownHook(final Runnable task)
  {
    if (inEventLoop()) {
      this.shutdownHooks.remove(task);
    } else {
      execute(new Runnable()
      {
        public void run()
        {
          SingleThreadEventExecutor.this.shutdownHooks.remove(task);
        }
      });
    }
  }
  
  private boolean runShutdownHooks()
  {
    boolean ran = false;
    while (!this.shutdownHooks.isEmpty())
    {
      List<Runnable> copy = new ArrayList(this.shutdownHooks);
      this.shutdownHooks.clear();
      for (Runnable task : copy) {
        try
        {
          task.run();
        }
        catch (Throwable t)
        {
          logger.warn("Shutdown hook raised an exception.", t);
        }
        finally
        {
          ran = true;
        }
      }
    }
    if (ran) {
      this.lastExecutionTime = ScheduledFutureTask.nanoTime();
    }
    return ran;
  }
  
  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
  {
    if (quietPeriod < 0L) {
      throw new IllegalArgumentException("quietPeriod: " + quietPeriod + " (expected >= 0)");
    }
    if (timeout < quietPeriod) {
      throw new IllegalArgumentException("timeout: " + timeout + " (expected >= quietPeriod (" + quietPeriod + "))");
    }
    if (unit == null) {
      throw new NullPointerException("unit");
    }
    if (isShuttingDown()) {
      return terminationFuture();
    }
    boolean inEventLoop = inEventLoop();
    boolean wakeup;
    int oldState;
    for (;;)
    {
      if (isShuttingDown()) {
        return terminationFuture();
      }
      wakeup = true;
      oldState = STATE_UPDATER.get(this);
      int newState;
      int newState;
      if (inEventLoop) {
        newState = 3;
      } else {
        switch (oldState)
        {
        case 1: 
        case 2: 
          newState = 3;
          break;
        default: 
          newState = oldState;
          wakeup = false;
        }
      }
      if (STATE_UPDATER.compareAndSet(this, oldState, newState)) {
        break;
      }
    }
    this.gracefulShutdownQuietPeriod = unit.toNanos(quietPeriod);
    this.gracefulShutdownTimeout = unit.toNanos(timeout);
    if (oldState == 1) {
      scheduleExecution();
    }
    if (wakeup) {
      wakeup(inEventLoop);
    }
    return terminationFuture();
  }
  
  public Future<?> terminationFuture()
  {
    return this.terminationFuture;
  }
  
  @Deprecated
  public void shutdown()
  {
    if (isShutdown()) {
      return;
    }
    boolean inEventLoop = inEventLoop();
    boolean wakeup;
    int oldState;
    for (;;)
    {
      if (isShuttingDown()) {
        return;
      }
      wakeup = true;
      oldState = STATE_UPDATER.get(this);
      int newState;
      int newState;
      if (inEventLoop) {
        newState = 4;
      } else {
        switch (oldState)
        {
        case 1: 
        case 2: 
        case 3: 
          newState = 4;
          break;
        default: 
          newState = oldState;
          wakeup = false;
        }
      }
      if (STATE_UPDATER.compareAndSet(this, oldState, newState)) {
        break;
      }
    }
    if (oldState == 1) {
      scheduleExecution();
    }
    if (wakeup) {
      wakeup(inEventLoop);
    }
  }
  
  public boolean isShuttingDown()
  {
    return STATE_UPDATER.get(this) >= 3;
  }
  
  public boolean isShutdown()
  {
    return STATE_UPDATER.get(this) >= 4;
  }
  
  public boolean isTerminated()
  {
    return STATE_UPDATER.get(this) == 5;
  }
  
  protected boolean confirmShutdown()
  {
    if (!isShuttingDown()) {
      return false;
    }
    if (!inEventLoop()) {
      throw new IllegalStateException("must be invoked from an event loop");
    }
    cancelScheduledTasks();
    if (this.gracefulShutdownStartTime == 0L) {
      this.gracefulShutdownStartTime = ScheduledFutureTask.nanoTime();
    }
    if ((runAllTasks()) || (runShutdownHooks()))
    {
      if (isShutdown()) {
        return true;
      }
      wakeup(true);
      return false;
    }
    long nanoTime = ScheduledFutureTask.nanoTime();
    if ((isShutdown()) || (nanoTime - this.gracefulShutdownStartTime > this.gracefulShutdownTimeout)) {
      return true;
    }
    if (nanoTime - this.lastExecutionTime <= this.gracefulShutdownQuietPeriod)
    {
      wakeup(true);
      try
      {
        Thread.sleep(100L);
      }
      catch (InterruptedException e) {}
      return false;
    }
    return true;
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    if (unit == null) {
      throw new NullPointerException("unit");
    }
    if (inEventLoop()) {
      throw new IllegalStateException("cannot await termination of the current thread");
    }
    if (this.threadLock.tryAcquire(timeout, unit)) {
      this.threadLock.release();
    }
    return isTerminated();
  }
  
  public void execute(Runnable task)
  {
    if (task == null) {
      throw new NullPointerException("task");
    }
    boolean inEventLoop = inEventLoop();
    if (inEventLoop)
    {
      addTask(task);
    }
    else
    {
      startExecution();
      addTask(task);
      if ((isShutdown()) && (removeTask(task))) {
        reject();
      }
    }
    if ((!this.addTaskWakesUp) && (wakesUpForTask(task))) {
      wakeup(inEventLoop);
    }
  }
  
  protected boolean wakesUpForTask(Runnable task)
  {
    return true;
  }
  
  protected static void reject()
  {
    throw new RejectedExecutionException("event executor terminated");
  }
  
  private static final long SCHEDULE_PURGE_INTERVAL = TimeUnit.SECONDS.toNanos(1L);
  
  protected void cleanupAndTerminate(boolean success)
  {
    for (;;)
    {
      int oldState = STATE_UPDATER.get(this);
      if ((oldState >= 3) || (STATE_UPDATER.compareAndSet(this, oldState, 3))) {
        break;
      }
    }
    if ((success) && (this.gracefulShutdownStartTime == 0L)) {
      logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; " + SingleThreadEventExecutor.class.getSimpleName() + ".confirmShutdown() must be called " + "before run() implementation terminates.");
    }
    try
    {
      for (;;)
      {
        if (confirmShutdown()) {
          break;
        }
      }
    }
    finally
    {
      try
      {
        cleanup();
      }
      finally
      {
        STATE_UPDATER.set(this, 5);
        this.threadLock.release();
        if (!this.taskQueue.isEmpty()) {
          logger.warn("An event executor terminated with non-empty task queue (" + this.taskQueue.size() + ')');
        }
        this.firstRun = true;
        this.terminationFuture.setSuccess(null);
      }
    }
  }
  
  private void startExecution()
  {
    if ((STATE_UPDATER.get(this) == 1) && 
      (STATE_UPDATER.compareAndSet(this, 1, 2)))
    {
      schedule(new ScheduledFutureTask(this, Executors.callable(new PurgeTask(null), null), ScheduledFutureTask.deadlineNanos(SCHEDULE_PURGE_INTERVAL), -SCHEDULE_PURGE_INTERVAL));
      
      scheduleExecution();
    }
  }
  
  protected final void scheduleExecution()
  {
    updateThread(null);
    this.executor.execute(this.asRunnable);
  }
  
  private void updateThread(Thread t)
  {
    THREAD_UPDATER.lazySet(this, t);
  }
  
  protected abstract void run();
  
  protected void cleanup() {}
  
  private final class PurgeTask
    implements Runnable
  {
    private PurgeTask() {}
    
    public void run()
    {
      SingleThreadEventExecutor.this.purgeCancelledScheduledTasks();
    }
  }
}
