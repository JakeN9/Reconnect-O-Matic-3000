package io.netty.channel.unix;

public abstract interface DomainSocketChannel
  extends UnixChannel
{
  public abstract DomainSocketAddress remoteAddress();
  
  public abstract DomainSocketAddress localAddress();
  
  public abstract DomainSocketChannelConfig config();
}
