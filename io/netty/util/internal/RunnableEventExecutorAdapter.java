package io.netty.util.internal;

import io.netty.util.concurrent.EventExecutor;

public abstract interface RunnableEventExecutorAdapter
  extends Runnable
{
  public abstract EventExecutor executor();
  
  public abstract Runnable unwrap();
}
