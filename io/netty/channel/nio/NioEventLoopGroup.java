package io.netty.channel.nio;

import io.netty.channel.EventLoop;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ExecutorServiceFactory;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

public class NioEventLoopGroup
  extends MultithreadEventLoopGroup
{
  public NioEventLoopGroup()
  {
    this(0);
  }
  
  public NioEventLoopGroup(int nEventLoops)
  {
    this(nEventLoops, (Executor)null);
  }
  
  public NioEventLoopGroup(int nEventLoops, Executor executor)
  {
    this(nEventLoops, executor, SelectorProvider.provider());
  }
  
  public NioEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory)
  {
    this(nEventLoops, executorServiceFactory, SelectorProvider.provider());
  }
  
  public NioEventLoopGroup(int nEventLoops, Executor executor, SelectorProvider selectorProvider)
  {
    super(nEventLoops, executor, new Object[] { selectorProvider });
  }
  
  public NioEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory, SelectorProvider selectorProvider)
  {
    super(nEventLoops, executorServiceFactory, new Object[] { selectorProvider });
  }
  
  public void setIoRatio(int ioRatio)
  {
    for (EventExecutor e : children()) {
      ((NioEventLoop)e).setIoRatio(ioRatio);
    }
  }
  
  public void rebuildSelectors()
  {
    for (EventExecutor e : children()) {
      ((NioEventLoop)e).rebuildSelector();
    }
  }
  
  protected EventLoop newChild(Executor executor, Object... args)
    throws Exception
  {
    return new NioEventLoop(this, executor, (SelectorProvider)args[0]);
  }
}
