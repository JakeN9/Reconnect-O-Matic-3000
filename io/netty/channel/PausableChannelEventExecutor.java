package io.netty.channel;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.PausableEventExecutor;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.CallableEventExecutorAdapter;
import io.netty.util.internal.RunnableEventExecutorAdapter;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract class PausableChannelEventExecutor
  implements PausableEventExecutor, ChannelHandlerInvoker
{
  abstract Channel channel();
  
  abstract ChannelHandlerInvoker unwrapInvoker();
  
  public void invokeFlush(ChannelHandlerContext ctx)
  {
    unwrapInvoker().invokeFlush(ctx);
  }
  
  public EventExecutor executor()
  {
    return this;
  }
  
  public void invokeChannelRegistered(ChannelHandlerContext ctx)
  {
    unwrapInvoker().invokeChannelRegistered(ctx);
  }
  
  public void invokeChannelUnregistered(ChannelHandlerContext ctx)
  {
    unwrapInvoker().invokeChannelUnregistered(ctx);
  }
  
  public void invokeChannelActive(ChannelHandlerContext ctx)
  {
    unwrapInvoker().invokeChannelActive(ctx);
  }
  
  public void invokeChannelInactive(ChannelHandlerContext ctx)
  {
    unwrapInvoker().invokeChannelInactive(ctx);
  }
  
  public void invokeExceptionCaught(ChannelHandlerContext ctx, Throwable cause)
  {
    unwrapInvoker().invokeExceptionCaught(ctx, cause);
  }
  
  public void invokeUserEventTriggered(ChannelHandlerContext ctx, Object event)
  {
    unwrapInvoker().invokeUserEventTriggered(ctx, event);
  }
  
  public void invokeChannelRead(ChannelHandlerContext ctx, Object msg)
  {
    unwrapInvoker().invokeChannelRead(ctx, msg);
  }
  
  public void invokeChannelReadComplete(ChannelHandlerContext ctx)
  {
    unwrapInvoker().invokeChannelReadComplete(ctx);
  }
  
  public void invokeChannelWritabilityChanged(ChannelHandlerContext ctx)
  {
    unwrapInvoker().invokeChannelWritabilityChanged(ctx);
  }
  
  public void invokeBind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
  {
    unwrapInvoker().invokeBind(ctx, localAddress, promise);
  }
  
  public void invokeConnect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
  {
    unwrapInvoker().invokeConnect(ctx, remoteAddress, localAddress, promise);
  }
  
  public void invokeDisconnect(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    unwrapInvoker().invokeDisconnect(ctx, promise);
  }
  
  public void invokeClose(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    unwrapInvoker().invokeClose(ctx, promise);
  }
  
  public void invokeDeregister(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    unwrapInvoker().invokeDeregister(ctx, promise);
  }
  
  public void invokeRead(ChannelHandlerContext ctx)
  {
    unwrapInvoker().invokeRead(ctx);
  }
  
  public void invokeWrite(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
  {
    unwrapInvoker().invokeWrite(ctx, msg, promise);
  }
  
  public EventExecutor next()
  {
    return unwrap().next();
  }
  
  public <E extends EventExecutor> Set<E> children()
  {
    return unwrap().children();
  }
  
  public EventExecutorGroup parent()
  {
    return unwrap().parent();
  }
  
  public boolean inEventLoop()
  {
    return unwrap().inEventLoop();
  }
  
  public boolean inEventLoop(Thread thread)
  {
    return unwrap().inEventLoop(thread);
  }
  
  public <V> Promise<V> newPromise()
  {
    return unwrap().newPromise();
  }
  
  public <V> ProgressivePromise<V> newProgressivePromise()
  {
    return unwrap().newProgressivePromise();
  }
  
  public <V> io.netty.util.concurrent.Future<V> newSucceededFuture(V result)
  {
    return unwrap().newSucceededFuture(result);
  }
  
  public <V> io.netty.util.concurrent.Future<V> newFailedFuture(Throwable cause)
  {
    return unwrap().newFailedFuture(cause);
  }
  
  public boolean isShuttingDown()
  {
    return unwrap().isShuttingDown();
  }
  
  public io.netty.util.concurrent.Future<?> shutdownGracefully()
  {
    return unwrap().shutdownGracefully();
  }
  
  public io.netty.util.concurrent.Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
  {
    return unwrap().shutdownGracefully(quietPeriod, timeout, unit);
  }
  
  public io.netty.util.concurrent.Future<?> terminationFuture()
  {
    return unwrap().terminationFuture();
  }
  
  @Deprecated
  public void shutdown()
  {
    unwrap().shutdown();
  }
  
  @Deprecated
  public List<Runnable> shutdownNow()
  {
    return unwrap().shutdownNow();
  }
  
  public io.netty.util.concurrent.Future<?> submit(Runnable task)
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return unwrap().submit(task);
  }
  
  public <T> io.netty.util.concurrent.Future<T> submit(Runnable task, T result)
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return unwrap().submit(task, result);
  }
  
  public <T> io.netty.util.concurrent.Future<T> submit(Callable<T> task)
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return unwrap().submit(task);
  }
  
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return unwrap().schedule(new ChannelRunnableEventExecutor(channel(), command), delay, unit);
  }
  
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return unwrap().schedule(new ChannelCallableEventExecutor(channel(), callable), delay, unit);
  }
  
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return unwrap().scheduleAtFixedRate(new ChannelRunnableEventExecutor(channel(), command), initialDelay, period, unit);
  }
  
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return unwrap().scheduleWithFixedDelay(new ChannelRunnableEventExecutor(channel(), command), initialDelay, delay, unit);
  }
  
  public boolean isShutdown()
  {
    return unwrap().isShutdown();
  }
  
  public boolean isTerminated()
  {
    return unwrap().isTerminated();
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    return unwrap().awaitTermination(timeout, unit);
  }
  
  public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
    throws InterruptedException
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return unwrap().invokeAll(tasks);
  }
  
  public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    throws InterruptedException
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return unwrap().invokeAll(tasks, timeout, unit);
  }
  
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
    throws InterruptedException, ExecutionException
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return (T)unwrap().invokeAny(tasks);
  }
  
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    return (T)unwrap().invokeAny(tasks, timeout, unit);
  }
  
  public void execute(Runnable command)
  {
    if (!isAcceptingNewTasks()) {
      throw new RejectedExecutionException();
    }
    unwrap().execute(command);
  }
  
  public void close()
    throws Exception
  {
    unwrap().close();
  }
  
  private static final class ChannelCallableEventExecutor<V>
    implements CallableEventExecutorAdapter<V>
  {
    final Channel channel;
    final Callable<V> callable;
    
    ChannelCallableEventExecutor(Channel channel, Callable<V> callable)
    {
      this.channel = channel;
      this.callable = callable;
    }
    
    public EventExecutor executor()
    {
      return this.channel.eventLoop();
    }
    
    public Callable unwrap()
    {
      return this.callable;
    }
    
    public V call()
      throws Exception
    {
      return (V)this.callable.call();
    }
  }
  
  private static final class ChannelRunnableEventExecutor
    implements RunnableEventExecutorAdapter
  {
    final Channel channel;
    final Runnable runnable;
    
    ChannelRunnableEventExecutor(Channel channel, Runnable runnable)
    {
      this.channel = channel;
      this.runnable = runnable;
    }
    
    public EventExecutor executor()
    {
      return this.channel.eventLoop();
    }
    
    public Runnable unwrap()
    {
      return this.runnable;
    }
    
    public void run()
    {
      this.runnable.run();
    }
  }
}
