package io.netty.channel.epoll;

import io.netty.channel.EventLoop;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ExecutorServiceFactory;
import java.util.concurrent.Executor;

public final class EpollEventLoopGroup
  extends MultithreadEventLoopGroup
{
  public EpollEventLoopGroup()
  {
    this(0);
  }
  
  public EpollEventLoopGroup(int nEventLoops)
  {
    this(nEventLoops, (Executor)null);
  }
  
  public EpollEventLoopGroup(int nEventLoops, Executor executor)
  {
    this(nEventLoops, executor, 0);
  }
  
  public EpollEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory)
  {
    this(nEventLoops, executorServiceFactory, 0);
  }
  
  @Deprecated
  public EpollEventLoopGroup(int nEventLoops, Executor executor, int maxEventsAtOnce)
  {
    super(nEventLoops, executor, new Object[] { Integer.valueOf(maxEventsAtOnce) });
  }
  
  @Deprecated
  public EpollEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory, int maxEventsAtOnce)
  {
    super(nEventLoops, executorServiceFactory, new Object[] { Integer.valueOf(maxEventsAtOnce) });
  }
  
  public void setIoRatio(int ioRatio)
  {
    for (EventExecutor e : children()) {
      ((EpollEventLoop)e).setIoRatio(ioRatio);
    }
  }
  
  protected EventLoop newChild(Executor executor, Object... args)
    throws Exception
  {
    return new EpollEventLoop(this, executor, ((Integer)args[0]).intValue());
  }
}
