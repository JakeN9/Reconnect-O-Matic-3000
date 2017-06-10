package io.netty.handler.codec.http2;

public abstract interface Http2StreamRemovalPolicy
{
  public abstract void setAction(Action paramAction);
  
  public abstract void markForRemoval(Http2Stream paramHttp2Stream);
  
  public static abstract interface Action
  {
    public abstract void removeStream(Http2Stream paramHttp2Stream);
  }
}
