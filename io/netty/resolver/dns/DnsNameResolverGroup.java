package io.netty.resolver.dns;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoop;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.socket.DatagramChannel;
import io.netty.resolver.NameResolver;
import io.netty.resolver.NameResolverGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.StringUtil;
import java.net.InetSocketAddress;

public final class DnsNameResolverGroup
  extends NameResolverGroup<InetSocketAddress>
{
  private final ChannelFactory<? extends DatagramChannel> channelFactory;
  private final InetSocketAddress localAddress;
  private final Iterable<InetSocketAddress> nameServerAddresses;
  
  public DnsNameResolverGroup(Class<? extends DatagramChannel> channelType, InetSocketAddress nameServerAddress)
  {
    this(channelType, DnsNameResolver.ANY_LOCAL_ADDR, nameServerAddress);
  }
  
  public DnsNameResolverGroup(Class<? extends DatagramChannel> channelType, InetSocketAddress localAddress, InetSocketAddress nameServerAddress)
  {
    this(new ReflectiveChannelFactory(channelType), localAddress, nameServerAddress);
  }
  
  public DnsNameResolverGroup(ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress nameServerAddress)
  {
    this(channelFactory, DnsNameResolver.ANY_LOCAL_ADDR, nameServerAddress);
  }
  
  public DnsNameResolverGroup(ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress localAddress, InetSocketAddress nameServerAddress)
  {
    this(channelFactory, localAddress, DnsServerAddresses.singleton(nameServerAddress));
  }
  
  public DnsNameResolverGroup(Class<? extends DatagramChannel> channelType, Iterable<InetSocketAddress> nameServerAddresses)
  {
    this(channelType, DnsNameResolver.ANY_LOCAL_ADDR, nameServerAddresses);
  }
  
  public DnsNameResolverGroup(Class<? extends DatagramChannel> channelType, InetSocketAddress localAddress, Iterable<InetSocketAddress> nameServerAddresses)
  {
    this(new ReflectiveChannelFactory(channelType), localAddress, nameServerAddresses);
  }
  
  public DnsNameResolverGroup(ChannelFactory<? extends DatagramChannel> channelFactory, Iterable<InetSocketAddress> nameServerAddresses)
  {
    this(channelFactory, DnsNameResolver.ANY_LOCAL_ADDR, nameServerAddresses);
  }
  
  public DnsNameResolverGroup(ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress localAddress, Iterable<InetSocketAddress> nameServerAddresses)
  {
    this.channelFactory = channelFactory;
    this.localAddress = localAddress;
    this.nameServerAddresses = nameServerAddresses;
  }
  
  protected NameResolver<InetSocketAddress> newResolver(EventExecutor executor)
    throws Exception
  {
    if (!(executor instanceof EventLoop)) {
      throw new IllegalStateException("unsupported executor type: " + StringUtil.simpleClassName(executor) + " (expected: " + StringUtil.simpleClassName(EventLoop.class));
    }
    return new DnsNameResolver((EventLoop)executor, this.channelFactory, this.localAddress, this.nameServerAddresses);
  }
}
