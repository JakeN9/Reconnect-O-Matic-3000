package io.netty.util.internal;

import io.netty.util.concurrent.EventExecutor;
import java.util.concurrent.Callable;

public abstract interface CallableEventExecutorAdapter<V>
  extends Callable<V>
{
  public abstract EventExecutor executor();
  
  public abstract Callable<V> unwrap();
}
