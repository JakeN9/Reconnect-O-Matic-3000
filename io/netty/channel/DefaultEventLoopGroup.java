package io.netty.channel;

import io.netty.util.concurrent.ExecutorServiceFactory;
import java.util.concurrent.Executor;

public class DefaultEventLoopGroup
  extends MultithreadEventLoopGroup
{
  public DefaultEventLoopGroup()
  {
    this(0);
  }
  
  public DefaultEventLoopGroup(int nEventLoops)
  {
    this(nEventLoops, (Executor)null);
  }
  
  public DefaultEventLoopGroup(int nEventLoops, Executor executor)
  {
    super(nEventLoops, executor, new Object[0]);
  }
  
  public DefaultEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory)
  {
    super(nEventLoops, executorServiceFactory, new Object[0]);
  }
  
  protected EventLoop newChild(Executor executor, Object... args)
    throws Exception
  {
    return new DefaultEventLoop(this, executor);
  }
}
