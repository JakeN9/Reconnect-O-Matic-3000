package io.netty.handler.codec.http2;

import java.util.Collection;

public abstract interface Http2Stream
{
  public abstract int id();
  
  public abstract State state();
  
  public abstract Http2Stream open(boolean paramBoolean)
    throws Http2Exception;
  
  public abstract Http2Stream close();
  
  public abstract Http2Stream closeLocalSide();
  
  public abstract Http2Stream closeRemoteSide();
  
  public abstract boolean isResetSent();
  
  public abstract Http2Stream resetSent();
  
  public abstract boolean remoteSideOpen();
  
  public abstract boolean localSideOpen();
  
  public abstract Object setProperty(Object paramObject1, Object paramObject2);
  
  public abstract <V> V getProperty(Object paramObject);
  
  public abstract <V> V removeProperty(Object paramObject);
  
  public abstract Http2Stream setPriority(int paramInt, short paramShort, boolean paramBoolean)
    throws Http2Exception;
  
  public abstract boolean isRoot();
  
  public abstract boolean isLeaf();
  
  public abstract short weight();
  
  public abstract int totalChildWeights();
  
  public abstract Http2Stream parent();
  
  public abstract boolean isDescendantOf(Http2Stream paramHttp2Stream);
  
  public abstract int numChildren();
  
  public abstract boolean hasChild(int paramInt);
  
  public abstract Http2Stream child(int paramInt);
  
  public abstract Collection<? extends Http2Stream> children();
  
  public static enum State
  {
    IDLE,  RESERVED_LOCAL,  RESERVED_REMOTE,  OPEN,  HALF_CLOSED_LOCAL,  HALF_CLOSED_REMOTE,  CLOSED;
    
    private State() {}
  }
}
