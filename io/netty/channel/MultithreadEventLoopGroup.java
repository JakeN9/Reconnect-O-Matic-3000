package io.netty.channel;

import io.netty.util.concurrent.ExecutorServiceFactory;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.Executor;

public abstract class MultithreadEventLoopGroup
  extends MultithreadEventExecutorGroup
  implements EventLoopGroup
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultithreadEventLoopGroup.class);
  private static final int DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2));
  
  static
  {
    if (logger.isDebugEnabled()) {
      logger.debug("-Dio.netty.eventLoopThreads: {}", Integer.valueOf(DEFAULT_EVENT_LOOP_THREADS));
    }
  }
  
  protected MultithreadEventLoopGroup(int nEventLoops, Executor executor, Object... args)
  {
    super(nEventLoops == 0 ? DEFAULT_EVENT_LOOP_THREADS : nEventLoops, executor, args);
  }
  
  protected MultithreadEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory, Object... args)
  {
    super(nEventLoops == 0 ? DEFAULT_EVENT_LOOP_THREADS : nEventLoops, executorServiceFactory, args);
  }
  
  public EventLoop next()
  {
    return (EventLoop)super.next();
  }
  
  public ChannelFuture register(Channel channel)
  {
    return next().register(channel);
  }
  
  public ChannelFuture register(Channel channel, ChannelPromise promise)
  {
    return next().register(channel, promise);
  }
  
  protected abstract EventLoop newChild(Executor paramExecutor, Object... paramVarArgs)
    throws Exception;
}
