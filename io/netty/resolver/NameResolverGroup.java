package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

public abstract class NameResolverGroup<T extends SocketAddress>
  implements Closeable
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(NameResolverGroup.class);
  private final Map<EventExecutor, NameResolver<T>> resolvers = new IdentityHashMap();
  
  public NameResolver<T> getResolver(EventExecutor executor)
  {
    if (executor == null) {
      throw new NullPointerException("executor");
    }
    if (executor.isShuttingDown()) {
      throw new IllegalStateException("executor not accepting a task");
    }
    return getResolver0(executor.unwrap());
  }
  
  private NameResolver<T> getResolver0(final EventExecutor executor)
  {
    NameResolver<T> r;
    synchronized (this.resolvers)
    {
      r = (NameResolver)this.resolvers.get(executor);
      if (r == null)
      {
        final NameResolver<T> newResolver;
        try
        {
          newResolver = newResolver(executor);
        }
        catch (Exception e)
        {
          throw new IllegalStateException("failed to create a new resolver", e);
        }
        this.resolvers.put(executor, newResolver);
        executor.terminationFuture().addListener(new FutureListener()
        {
          public void operationComplete(Future<Object> future)
            throws Exception
          {
            NameResolverGroup.this.resolvers.remove(executor);
            newResolver.close();
          }
        });
        r = newResolver;
      }
    }
    return r;
  }
  
  protected abstract NameResolver<T> newResolver(EventExecutor paramEventExecutor)
    throws Exception;
  
  public void close()
  {
    NameResolver<T>[] rArray;
    synchronized (this.resolvers)
    {
      rArray = (NameResolver[])this.resolvers.values().toArray(new NameResolver[this.resolvers.size()]);
      this.resolvers.clear();
    }
    for (NameResolver<T> r : rArray) {
      try
      {
        r.close();
      }
      catch (Throwable t)
      {
        logger.warn("Failed to close a resolver:", t);
      }
    }
  }
}
