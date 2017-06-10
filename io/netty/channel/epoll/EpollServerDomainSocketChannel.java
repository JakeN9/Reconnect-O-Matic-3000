package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.ServerDomainSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.File;
import java.net.SocketAddress;

public final class EpollServerDomainSocketChannel
  extends AbstractEpollServerChannel
  implements ServerDomainSocketChannel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(EpollServerDomainSocketChannel.class);
  private final EpollServerChannelConfig config = new EpollServerChannelConfig(this);
  private volatile DomainSocketAddress local;
  
  public EpollServerDomainSocketChannel()
  {
    super(Native.socketDomainFd());
  }
  
  public EpollServerDomainSocketChannel(FileDescriptor fd)
  {
    super(fd);
  }
  
  protected Channel newChildChannel(int fd, byte[] addr, int offset, int len)
    throws Exception
  {
    return new EpollDomainSocketChannel(this, fd);
  }
  
  protected DomainSocketAddress localAddress0()
  {
    return this.local;
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {
    int fd = fd().intValue();
    Native.bind(fd, localAddress);
    Native.listen(fd, this.config.getBacklog());
    this.local = ((DomainSocketAddress)localAddress);
  }
  
  protected void doClose()
    throws Exception
  {
    try
    {
      super.doClose();
    }
    finally
    {
      DomainSocketAddress local;
      File socketFile;
      boolean success;
      DomainSocketAddress local = this.local;
      if (local != null)
      {
        File socketFile = new File(local.path());
        boolean success = socketFile.delete();
        if ((!success) && (logger.isDebugEnabled())) {
          logger.debug("Failed to delete a domain socket file: {}", local.path());
        }
      }
    }
  }
  
  public EpollServerChannelConfig config()
  {
    return this.config;
  }
  
  public DomainSocketAddress remoteAddress()
  {
    return (DomainSocketAddress)super.remoteAddress();
  }
  
  public DomainSocketAddress localAddress()
  {
    return (DomainSocketAddress)super.localAddress();
  }
}
