package io.netty.channel.embedded;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerInvoker;
import io.netty.channel.ChannelHandlerInvokerUtil;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.AbstractScheduledEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

final class EmbeddedEventLoop
  extends AbstractScheduledEventExecutor
  implements ChannelHandlerInvoker, EventLoop
{
  private final Queue<Runnable> tasks = new ArrayDeque(2);
  
  public EventLoop unwrap()
  {
    return this;
  }
  
  public EventLoopGroup parent()
  {
    return (EventLoopGroup)super.parent();
  }
  
  public EventLoop next()
  {
    return (EventLoop)super.next();
  }
  
  public void execute(Runnable command)
  {
    if (command == null) {
      throw new NullPointerException("command");
    }
    this.tasks.add(command);
  }
  
  void runTasks()
  {
    for (;;)
    {
      Runnable task = (Runnable)this.tasks.poll();
      if (task == null) {
        break;
      }
      task.run();
    }
  }
  
  long runScheduledTasks()
  {
    long time = AbstractScheduledEventExecutor.nanoTime();
    for (;;)
    {
      Runnable task = pollScheduledTask(time);
      if (task == null) {
        return nextScheduledTaskNano();
      }
      task.run();
    }
  }
  
  long nextScheduledTask()
  {
    return nextScheduledTaskNano();
  }
  
  protected void cancelScheduledTasks()
  {
    super.cancelScheduledTasks();
  }
  
  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
  {
    throw new UnsupportedOperationException();
  }
  
  public Future<?> terminationFuture()
  {
    throw new UnsupportedOperationException();
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
  
  public ChannelFuture register(Channel channel)
  {
    return register(channel, new DefaultChannelPromise(channel, this));
  }
  
  public ChannelFuture register(Channel channel, ChannelPromise promise)
  {
    channel.unsafe().register(this, promise);
    return promise;
  }
  
  public boolean inEventLoop()
  {
    return true;
  }
  
  public boolean inEventLoop(Thread thread)
  {
    return true;
  }
  
  public ChannelHandlerInvoker asInvoker()
  {
    return this;
  }
  
  public EventExecutor executor()
  {
    return this;
  }
  
  public void invokeChannelRegistered(ChannelHandlerContext ctx)
  {
    ChannelHandlerInvokerUtil.invokeChannelRegisteredNow(ctx);
  }
  
  public void invokeChannelUnregistered(ChannelHandlerContext ctx)
  {
    ChannelHandlerInvokerUtil.invokeChannelUnregisteredNow(ctx);
  }
  
  public void invokeChannelActive(ChannelHandlerContext ctx)
  {
    ChannelHandlerInvokerUtil.invokeChannelActiveNow(ctx);
  }
  
  public void invokeChannelInactive(ChannelHandlerContext ctx)
  {
    ChannelHandlerInvokerUtil.invokeChannelInactiveNow(ctx);
  }
  
  public void invokeExceptionCaught(ChannelHandlerContext ctx, Throwable cause)
  {
    ChannelHandlerInvokerUtil.invokeExceptionCaughtNow(ctx, cause);
  }
  
  public void invokeUserEventTriggered(ChannelHandlerContext ctx, Object event)
  {
    ChannelHandlerInvokerUtil.invokeUserEventTriggeredNow(ctx, event);
  }
  
  public void invokeChannelRead(ChannelHandlerContext ctx, Object msg)
  {
    ChannelHandlerInvokerUtil.invokeChannelReadNow(ctx, msg);
  }
  
  public void invokeChannelReadComplete(ChannelHandlerContext ctx)
  {
    ChannelHandlerInvokerUtil.invokeChannelReadCompleteNow(ctx);
  }
  
  public void invokeChannelWritabilityChanged(ChannelHandlerContext ctx)
  {
    ChannelHandlerInvokerUtil.invokeChannelWritabilityChangedNow(ctx);
  }
  
  public void invokeBind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
  {
    ChannelHandlerInvokerUtil.invokeBindNow(ctx, localAddress, promise);
  }
  
  public void invokeConnect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
  {
    ChannelHandlerInvokerUtil.invokeConnectNow(ctx, remoteAddress, localAddress, promise);
  }
  
  public void invokeDisconnect(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    ChannelHandlerInvokerUtil.invokeDisconnectNow(ctx, promise);
  }
  
  public void invokeClose(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    ChannelHandlerInvokerUtil.invokeCloseNow(ctx, promise);
  }
  
  public void invokeDeregister(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    ChannelHandlerInvokerUtil.invokeDeregisterNow(ctx, promise);
  }
  
  public void invokeRead(ChannelHandlerContext ctx)
  {
    ChannelHandlerInvokerUtil.invokeReadNow(ctx);
  }
  
  public void invokeWrite(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
  {
    ChannelHandlerInvokerUtil.invokeWriteNow(ctx, msg, promise);
  }
  
  public void invokeFlush(ChannelHandlerContext ctx)
  {
    ChannelHandlerInvokerUtil.invokeFlushNow(ctx);
  }
}
