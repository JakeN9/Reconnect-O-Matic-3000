package io.netty.channel;

import io.netty.util.concurrent.DefaultExecutorServiceFactory;
import java.util.concurrent.Executor;

public class DefaultEventLoop
  extends SingleThreadEventLoop
{
  public DefaultEventLoop()
  {
    this((EventLoopGroup)null);
  }
  
  public DefaultEventLoop(Executor executor)
  {
    this(null, executor);
  }
  
  public DefaultEventLoop(EventLoopGroup parent)
  {
    this(parent, new DefaultExecutorServiceFactory(DefaultEventLoop.class).newExecutorService(1));
  }
  
  public DefaultEventLoop(EventLoopGroup parent, Executor executor)
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
