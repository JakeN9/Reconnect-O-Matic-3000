package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import java.net.InetSocketAddress;

public final class DefaultNameResolverGroup
  extends NameResolverGroup<InetSocketAddress>
{
  public static final DefaultNameResolverGroup INSTANCE = new DefaultNameResolverGroup();
  
  protected NameResolver<InetSocketAddress> newResolver(EventExecutor executor)
    throws Exception
  {
    return new DefaultNameResolver(executor);
  }
}
