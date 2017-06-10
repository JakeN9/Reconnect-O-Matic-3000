package io.netty.util.concurrent;

import java.util.concurrent.ExecutorService;

public abstract interface ExecutorServiceFactory
{
  public abstract ExecutorService newExecutorService(int paramInt);
}
