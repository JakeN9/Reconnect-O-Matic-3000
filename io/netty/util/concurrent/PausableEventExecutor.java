package io.netty.util.concurrent;

public abstract interface PausableEventExecutor
  extends EventExecutor, WrappedEventExecutor
{
  public abstract void rejectNewTasks();
  
  public abstract void acceptNewTasks();
  
  public abstract boolean isAcceptingNewTasks();
}
