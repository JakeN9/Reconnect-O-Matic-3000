package io.netty.resolver;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.io.Closeable;
import java.net.SocketAddress;

public abstract interface NameResolver<T extends SocketAddress>
  extends Closeable
{
  public abstract boolean isSupported(SocketAddress paramSocketAddress);
  
  public abstract boolean isResolved(SocketAddress paramSocketAddress);
  
  public abstract Future<T> resolve(String paramString, int paramInt);
  
  public abstract Future<T> resolve(String paramString, int paramInt, Promise<T> paramPromise);
  
  public abstract Future<T> resolve(SocketAddress paramSocketAddress);
  
  public abstract Future<T> resolve(SocketAddress paramSocketAddress, Promise<T> paramPromise);
  
  public abstract void close();
}
