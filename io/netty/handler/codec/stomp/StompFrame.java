package io.netty.handler.codec.stomp;

public abstract interface StompFrame
  extends StompHeadersSubframe, LastStompContentSubframe
{
  public abstract StompFrame copy();
  
  public abstract StompFrame duplicate();
  
  public abstract StompFrame retain();
  
  public abstract StompFrame retain(int paramInt);
  
  public abstract StompFrame touch();
  
  public abstract StompFrame touch(Object paramObject);
}
