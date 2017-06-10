package io.netty.channel;

import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import java.net.SocketAddress;

public final class ChannelHandlerInvokerUtil
{
  public static void invokeChannelRegisteredNow(ChannelHandlerContext ctx)
  {
    try
    {
      ctx.handler().channelRegistered(ctx);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static void invokeChannelUnregisteredNow(ChannelHandlerContext ctx)
  {
    try
    {
      ctx.handler().channelUnregistered(ctx);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static void invokeChannelActiveNow(ChannelHandlerContext ctx)
  {
    try
    {
      ctx.handler().channelActive(ctx);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static void invokeChannelInactiveNow(ChannelHandlerContext ctx)
  {
    try
    {
      ctx.handler().channelInactive(ctx);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static void invokeExceptionCaughtNow(ChannelHandlerContext ctx, Throwable cause)
  {
    try
    {
      ctx.handler().exceptionCaught(ctx, cause);
    }
    catch (Throwable t)
    {
      if (DefaultChannelPipeline.logger.isWarnEnabled())
      {
        DefaultChannelPipeline.logger.warn("An exception was thrown by a user handler's exceptionCaught() method:", t);
        DefaultChannelPipeline.logger.warn(".. and the cause of the exceptionCaught() was:", cause);
      }
    }
  }
  
  public static void invokeUserEventTriggeredNow(ChannelHandlerContext ctx, Object event)
  {
    try
    {
      ctx.handler().userEventTriggered(ctx, event);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static void invokeChannelReadNow(ChannelHandlerContext ctx, Object msg)
  {
    try
    {
      ((AbstractChannelHandlerContext)ctx).invokedThisChannelRead = true;
      ctx.handler().channelRead(ctx, msg);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static void invokeChannelReadCompleteNow(ChannelHandlerContext ctx)
  {
    try
    {
      ctx.handler().channelReadComplete(ctx);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static void invokeChannelWritabilityChangedNow(ChannelHandlerContext ctx)
  {
    try
    {
      ctx.handler().channelWritabilityChanged(ctx);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static void invokeBindNow(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
  {
    try
    {
      ctx.handler().bind(ctx, localAddress, promise);
    }
    catch (Throwable t)
    {
      notifyOutboundHandlerException(t, promise);
    }
  }
  
  public static void invokeConnectNow(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
  {
    try
    {
      ctx.handler().connect(ctx, remoteAddress, localAddress, promise);
    }
    catch (Throwable t)
    {
      notifyOutboundHandlerException(t, promise);
    }
  }
  
  public static void invokeDisconnectNow(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    try
    {
      ctx.handler().disconnect(ctx, promise);
    }
    catch (Throwable t)
    {
      notifyOutboundHandlerException(t, promise);
    }
  }
  
  public static void invokeCloseNow(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    try
    {
      ctx.handler().close(ctx, promise);
    }
    catch (Throwable t)
    {
      notifyOutboundHandlerException(t, promise);
    }
  }
  
  public static void invokeDeregisterNow(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    try
    {
      ctx.handler().deregister(ctx, promise);
    }
    catch (Throwable t)
    {
      notifyOutboundHandlerException(t, promise);
    }
  }
  
  public static void invokeReadNow(ChannelHandlerContext ctx)
  {
    try
    {
      ctx.handler().read(ctx);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static void invokeWriteNow(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
  {
    try
    {
      ctx.handler().write(ctx, msg, promise);
    }
    catch (Throwable t)
    {
      notifyOutboundHandlerException(t, promise);
    }
  }
  
  public static void invokeFlushNow(ChannelHandlerContext ctx)
  {
    try
    {
      ctx.handler().flush(ctx);
    }
    catch (Throwable t)
    {
      notifyHandlerException(ctx, t);
    }
  }
  
  public static boolean validatePromise(ChannelHandlerContext ctx, ChannelPromise promise, boolean allowVoidPromise)
  {
    if (ctx == null) {
      throw new NullPointerException("ctx");
    }
    if (promise == null) {
      throw new NullPointerException("promise");
    }
    if (promise.isDone())
    {
      if (promise.isCancelled()) {
        return false;
      }
      throw new IllegalArgumentException("promise already done: " + promise);
    }
    if (promise.channel() != ctx.channel()) {
      throw new IllegalArgumentException(String.format("promise.channel does not match: %s (expected: %s)", new Object[] { promise.channel(), ctx.channel() }));
    }
    if (promise.getClass() == DefaultChannelPromise.class) {
      return true;
    }
    if ((!allowVoidPromise) && ((promise instanceof VoidChannelPromise))) {
      throw new IllegalArgumentException(StringUtil.simpleClassName(VoidChannelPromise.class) + " not allowed for this operation");
    }
    if ((promise instanceof AbstractChannel.CloseFuture)) {
      throw new IllegalArgumentException(StringUtil.simpleClassName(AbstractChannel.CloseFuture.class) + " not allowed in a pipeline");
    }
    return true;
  }
  
  private static void notifyHandlerException(ChannelHandlerContext ctx, Throwable cause)
  {
    if (inExceptionCaught(cause))
    {
      if (DefaultChannelPipeline.logger.isWarnEnabled()) {
        DefaultChannelPipeline.logger.warn("An exception was thrown by a user handler while handling an exceptionCaught event", cause);
      }
      return;
    }
    invokeExceptionCaughtNow(ctx, cause);
  }
  
  private static void notifyOutboundHandlerException(Throwable cause, ChannelPromise promise)
  {
    if ((!promise.tryFailure(cause)) && (!(promise instanceof VoidChannelPromise)) && 
      (DefaultChannelPipeline.logger.isWarnEnabled())) {
      DefaultChannelPipeline.logger.warn("Failed to fail the promise because it's done already: {}", promise, cause);
    }
  }
  
  private static boolean inExceptionCaught(Throwable cause)
  {
    do
    {
      StackTraceElement[] trace = cause.getStackTrace();
      if (trace != null) {
        for (StackTraceElement t : trace)
        {
          if (t == null) {
            break;
          }
          if ("exceptionCaught".equals(t.getMethodName())) {
            return true;
          }
        }
      }
      cause = cause.getCause();
    } while (cause != null);
    return false;
  }
}
