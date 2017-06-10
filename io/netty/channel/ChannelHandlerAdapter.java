package io.netty.channel;

import io.netty.util.internal.InternalThreadLocalMap;
import java.net.SocketAddress;
import java.util.Map;

public class ChannelHandlerAdapter
  implements ChannelHandler
{
  boolean added;
  
  public boolean isSharable()
  {
    Class<?> clazz = getClass();
    Map<Class<?>, Boolean> cache = InternalThreadLocalMap.get().handlerSharableCache();
    Boolean sharable = (Boolean)cache.get(clazz);
    if (sharable == null)
    {
      sharable = Boolean.valueOf(clazz.isAnnotationPresent(ChannelHandler.Sharable.class));
      cache.put(clazz, sharable);
    }
    return sharable.booleanValue();
  }
  
  @ChannelHandler.Skip
  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {}
  
  @ChannelHandler.Skip
  public void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {}
  
  @ChannelHandler.Skip
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    ctx.fireExceptionCaught(cause);
  }
  
  @ChannelHandler.Skip
  public void channelRegistered(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelRegistered();
  }
  
  @ChannelHandler.Skip
  public void channelUnregistered(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelUnregistered();
  }
  
  @ChannelHandler.Skip
  public void channelActive(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelActive();
  }
  
  @ChannelHandler.Skip
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelInactive();
  }
  
  @ChannelHandler.Skip
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    ctx.fireChannelRead(msg);
  }
  
  @ChannelHandler.Skip
  public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelReadComplete();
  }
  
  @ChannelHandler.Skip
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
    throws Exception
  {
    ctx.fireUserEventTriggered(evt);
  }
  
  @ChannelHandler.Skip
  public void channelWritabilityChanged(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.fireChannelWritabilityChanged();
  }
  
  @ChannelHandler.Skip
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.bind(localAddress, promise);
  }
  
  @ChannelHandler.Skip
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    ctx.connect(remoteAddress, localAddress, promise);
  }
  
  @ChannelHandler.Skip
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    ctx.disconnect(promise);
  }
  
  @ChannelHandler.Skip
  public void close(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    ctx.close(promise);
  }
  
  @ChannelHandler.Skip
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    ctx.deregister(promise);
  }
  
  @ChannelHandler.Skip
  public void read(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.read();
  }
  
  @ChannelHandler.Skip
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    ctx.write(msg, promise);
  }
  
  @ChannelHandler.Skip
  public void flush(ChannelHandlerContext ctx)
    throws Exception
  {
    ctx.flush();
  }
}
