package io.netty.handler.codec.memcache;

import io.netty.buffer.ByteBufHolder;

public abstract interface MemcacheContent
  extends MemcacheObject, ByteBufHolder
{
  public abstract MemcacheContent copy();
  
  public abstract MemcacheContent duplicate();
  
  public abstract MemcacheContent retain();
  
  public abstract MemcacheContent retain(int paramInt);
  
  public abstract MemcacheContent touch();
  
  public abstract MemcacheContent touch(Object paramObject);
}
