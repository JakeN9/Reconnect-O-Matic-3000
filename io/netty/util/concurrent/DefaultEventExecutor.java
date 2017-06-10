package io.netty.util.concurrent;

import java.util.concurrent.Executor;

public final class DefaultEventExecutor
  extends SingleThreadEventExecutor
{
  public DefaultEventExecutor()
  {
    this((EventExecutorGroup)null);
  }
  
  public DefaultEventExecutor(Executor executor)
  {
    this(null, executor);
  }
  
  public DefaultEventExecutor(EventExecutorGroup parent)
  {
    this(parent, new DefaultExecutorServiceFactory(DefaultEventExecutor.class).newExecutorService(1));
  }
  
  public DefaultEventExecutor(EventExecutorGroup parent, Executor executor)
  {
    super(parent, executor, true);
  }
  
  protected void run()
  {
    Runnable task = takeTask();
    if (task != null)
    {
      task.run();
      updateLastExecutionTime();
    }
    if (confirmShutdown()) {
      cleanupAndTerminate(true);
    } else {
      scheduleExecution();
    }
  }
}
