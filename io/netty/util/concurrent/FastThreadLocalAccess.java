package io.netty.util.concurrent;

import io.netty.util.internal.InternalThreadLocalMap;

public abstract interface FastThreadLocalAccess
{
  public abstract InternalThreadLocalMap threadLocalMap();
  
  public abstract void setThreadLocalMap(InternalThreadLocalMap paramInternalThreadLocalMap);
}
