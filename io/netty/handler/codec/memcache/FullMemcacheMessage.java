package io.netty.handler.codec.memcache;

public abstract interface FullMemcacheMessage
  extends MemcacheMessage, LastMemcacheContent
{
  public abstract FullMemcacheMessage copy();
  
  public abstract FullMemcacheMessage retain(int paramInt);
  
  public abstract FullMemcacheMessage retain();
  
  public abstract FullMemcacheMessage touch();
  
  public abstract FullMemcacheMessage touch(Object paramObject);
  
  public abstract FullMemcacheMessage duplicate();
}
