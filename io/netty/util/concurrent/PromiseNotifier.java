package io.netty.util.concurrent;

public class PromiseNotifier<V, F extends Future<V>>
  implements GenericFutureListener<F>
{
  private final Promise<? super V>[] promises;
  
  @SafeVarargs
  public PromiseNotifier(Promise<? super V>... promises)
  {
    if (promises == null) {
      throw new NullPointerException("promises");
    }
    for (Promise<? super V> promise : promises) {
      if (promise == null) {
        throw new IllegalArgumentException("promises contains null Promise");
      }
    }
    this.promises = ((Promise[])promises.clone());
  }
  
  public void operationComplete(F future)
    throws Exception
  {
    if (future.isSuccess())
    {
      V result = future.get();
      for (Promise<? super V> p : this.promises) {
        p.setSuccess(result);
      }
      return;
    }
    Throwable cause = future.cause();
    for (Promise<? super V> p : this.promises) {
      p.setFailure(cause);
    }
  }
}
