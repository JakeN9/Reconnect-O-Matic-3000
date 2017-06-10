package io.netty.util;

public abstract interface ResourceLeak
{
  public abstract void record();
  
  public abstract void record(Object paramObject);
  
  public abstract boolean close();
}
