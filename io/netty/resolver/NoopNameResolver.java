package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.SocketAddress;

public class NoopNameResolver
  extends SimpleNameResolver<SocketAddress>
{
  public NoopNameResolver(EventExecutor executor)
  {
    super(executor);
  }
  
  protected boolean doIsResolved(SocketAddress address)
  {
    return true;
  }
  
  protected void doResolve(SocketAddress unresolvedAddress, Promise<SocketAddress> promise)
    throws Exception
  {
    promise.setSuccess(unresolvedAddress);
  }
}
