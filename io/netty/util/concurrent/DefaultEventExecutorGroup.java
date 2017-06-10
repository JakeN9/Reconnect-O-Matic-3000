package io.netty.util.concurrent;

import java.util.concurrent.Executor;

public class DefaultEventExecutorGroup
  extends MultithreadEventExecutorGroup
{
  public DefaultEventExecutorGroup(int nEventExecutors)
  {
    this(nEventExecutors, (Executor)null);
  }
  
  public DefaultEventExecutorGroup(int nEventExecutors, Executor executor)
  {
    super(nEventExecutors, executor, new Object[0]);
  }
  
  public DefaultEventExecutorGroup(int nEventExecutors, ExecutorServiceFactory executorServiceFactory)
  {
    super(nEventExecutors, executorServiceFactory, new Object[0]);
  }
  
  protected EventExecutor newChild(Executor executor, Object... args)
    throws Exception
  {
    return new DefaultEventExecutor(this, executor);
  }
}
