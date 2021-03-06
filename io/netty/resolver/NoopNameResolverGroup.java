package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import java.net.SocketAddress;

public final class NoopNameResolverGroup
  extends NameResolverGroup<SocketAddress>
{
  public static final NoopNameResolverGroup INSTANCE = new NoopNameResolverGroup();
  
  protected NameResolver<SocketAddress> newResolver(EventExecutor executor)
    throws Exception
  {
    return new NoopNameResolver(executor);
  }
}
