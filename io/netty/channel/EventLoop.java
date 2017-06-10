package io.netty.channel;

import io.netty.util.concurrent.EventExecutor;

public abstract interface EventLoop
  extends EventExecutor, EventLoopGroup
{
  public abstract EventLoopGroup parent();
  
  public abstract EventLoop unwrap();
  
  public abstract ChannelHandlerInvoker asInvoker();
}
