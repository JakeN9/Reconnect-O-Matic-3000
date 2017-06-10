package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class DefaultNameResolver
  extends SimpleNameResolver<InetSocketAddress>
{
  public DefaultNameResolver(EventExecutor executor)
  {
    super(executor);
  }
  
  protected boolean doIsResolved(InetSocketAddress address)
  {
    return !address.isUnresolved();
  }
  
  protected void doResolve(InetSocketAddress unresolvedAddress, Promise<InetSocketAddress> promise)
    throws Exception
  {
    try
    {
      promise.setSuccess(new InetSocketAddress(InetAddress.getByName(unresolvedAddress.getHostString()), unresolvedAddress.getPort()));
    }
    catch (UnknownHostException e)
    {
      promise.setFailure(e);
    }
  }
}
