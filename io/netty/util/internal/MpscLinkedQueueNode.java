package io.netty.util.internal;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public abstract class MpscLinkedQueueNode<T>
{
  private static final AtomicReferenceFieldUpdater<MpscLinkedQueueNode, MpscLinkedQueueNode> nextUpdater;
  private volatile MpscLinkedQueueNode<T> next;
  
  static
  {
    AtomicReferenceFieldUpdater<MpscLinkedQueueNode, MpscLinkedQueueNode> u = PlatformDependent.newAtomicReferenceFieldUpdater(MpscLinkedQueueNode.class, "next");
    if (u == null) {
      u = AtomicReferenceFieldUpdater.newUpdater(MpscLinkedQueueNode.class, MpscLinkedQueueNode.class, "next");
    }
    nextUpdater = u;
  }
  
  final MpscLinkedQueueNode<T> next()
  {
    return this.next;
  }
  
  final void setNext(MpscLinkedQueueNode<T> newNext)
  {
    nextUpdater.lazySet(this, newNext);
  }
  
  protected T clearMaybe()
  {
    return (T)value();
  }
  
  void unlink()
  {
    setNext(null);
  }
  
  public abstract T value();
}
