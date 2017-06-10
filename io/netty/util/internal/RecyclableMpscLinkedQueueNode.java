package io.netty.util.internal;

import io.netty.util.Recycler.Handle;

public abstract class RecyclableMpscLinkedQueueNode<T>
  extends MpscLinkedQueueNode<T>
{
  private final Recycler.Handle handle;
  
  protected RecyclableMpscLinkedQueueNode(Recycler.Handle<? extends RecyclableMpscLinkedQueueNode<T>> handle)
  {
    if (handle == null) {
      throw new NullPointerException("handle");
    }
    this.handle = handle;
  }
  
  final void unlink()
  {
    super.unlink();
    this.handle.recycle(this);
  }
}
