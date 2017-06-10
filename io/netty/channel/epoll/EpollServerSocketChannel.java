package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.unix.FileDescriptor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class EpollServerSocketChannel
  extends AbstractEpollServerChannel
  implements ServerSocketChannel
{
  private final EpollServerSocketChannelConfig config;
  private volatile InetSocketAddress local;
  
  public EpollServerSocketChannel()
  {
    super(Native.socketStreamFd());
    this.config = new EpollServerSocketChannelConfig(this);
  }
  
  public EpollServerSocketChannel(FileDescriptor fd)
  {
    super(fd);
    this.config = new EpollServerSocketChannelConfig(this);
    
    this.local = Native.localAddress(fd.intValue());
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof EpollEventLoop;
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {
    InetSocketAddress addr = (InetSocketAddress)localAddress;
    checkResolvable(addr);
    int fd = fd().intValue();
    Native.bind(fd, addr);
    this.local = Native.localAddress(fd);
    Native.listen(fd, this.config.getBacklog());
    this.active = true;
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public EpollServerSocketChannelConfig config()
  {
    return this.config;
  }
  
  protected InetSocketAddress localAddress0()
  {
    return this.local;
  }
  
  protected Channel newChildChannel(int fd, byte[] address, int offset, int len)
    throws Exception
  {
    return new EpollSocketChannel(this, fd, Native.address(address, offset, len));
  }
}
