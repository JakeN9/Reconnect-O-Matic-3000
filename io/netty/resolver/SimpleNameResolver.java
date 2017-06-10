package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.TypeParameterMatcher;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.UnsupportedAddressTypeException;

public abstract class SimpleNameResolver<T extends SocketAddress>
  implements NameResolver<T>
{
  private final EventExecutor executor;
  private final TypeParameterMatcher matcher;
  
  protected SimpleNameResolver(EventExecutor executor)
  {
    if (executor == null) {
      throw new NullPointerException("executor");
    }
    this.executor = executor;
    this.matcher = TypeParameterMatcher.find(this, SimpleNameResolver.class, "T");
  }
  
  protected SimpleNameResolver(EventExecutor executor, Class<? extends T> addressType)
  {
    if (executor == null) {
      throw new NullPointerException("executor");
    }
    this.executor = executor;
    this.matcher = TypeParameterMatcher.get(addressType);
  }
  
  protected EventExecutor executor()
  {
    return this.executor;
  }
  
  public boolean isSupported(SocketAddress address)
  {
    return this.matcher.match(address);
  }
  
  public final boolean isResolved(SocketAddress address)
  {
    if (!isSupported(address)) {
      throw new UnsupportedAddressTypeException();
    }
    T castAddress = address;
    return doIsResolved(castAddress);
  }
  
  protected abstract boolean doIsResolved(T paramT);
  
  public final Future<T> resolve(String inetHost, int inetPort)
  {
    if (inetHost == null) {
      throw new NullPointerException("inetHost");
    }
    return resolve(InetSocketAddress.createUnresolved(inetHost, inetPort));
  }
  
  public Future<T> resolve(String inetHost, int inetPort, Promise<T> promise)
  {
    if (inetHost == null) {
      throw new NullPointerException("inetHost");
    }
    return resolve(InetSocketAddress.createUnresolved(inetHost, inetPort), promise);
  }
  
  public final Future<T> resolve(SocketAddress address)
  {
    if (address == null) {
      throw new NullPointerException("unresolvedAddress");
    }
    if (!isSupported(address)) {
      return executor().newFailedFuture(new UnsupportedAddressTypeException());
    }
    if (isResolved(address))
    {
      T cast = address;
      return this.executor.newSucceededFuture(cast);
    }
    try
    {
      T cast = address;
      Promise<T> promise = executor().newPromise();
      doResolve(cast, promise);
      return promise;
    }
    catch (Exception e)
    {
      return executor().newFailedFuture(e);
    }
  }
  
  public final Future<T> resolve(SocketAddress address, Promise<T> promise)
  {
    if (address == null) {
      throw new NullPointerException("unresolvedAddress");
    }
    if (promise == null) {
      throw new NullPointerException("promise");
    }
    if (!isSupported(address)) {
      return promise.setFailure(new UnsupportedAddressTypeException());
    }
    if (isResolved(address))
    {
      T cast = address;
      return promise.setSuccess(cast);
    }
    try
    {
      T cast = address;
      doResolve(cast, promise);
      return promise;
    }
    catch (Exception e)
    {
      return promise.setFailure(e);
    }
  }
  
  protected abstract void doResolve(T paramT, Promise<T> paramPromise)
    throws Exception;
  
  public void close() {}
}
