package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.OneTimeTask;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

public final class EpollSocketChannel
  extends AbstractEpollStreamChannel
  implements SocketChannel
{
  private final EpollSocketChannelConfig config;
  private volatile InetSocketAddress local;
  private volatile InetSocketAddress remote;
  
  EpollSocketChannel(Channel parent, int fd, InetSocketAddress remote)
  {
    super(parent, fd);
    this.config = new EpollSocketChannelConfig(this);
    
    this.remote = remote;
    this.local = Native.localAddress(fd);
  }
  
  public EpollSocketChannel()
  {
    super(Native.socketStreamFd());
    this.config = new EpollSocketChannelConfig(this);
  }
  
  public EpollSocketChannel(FileDescriptor fd)
  {
    super(fd);
    this.config = new EpollSocketChannelConfig(this);
    
    this.remote = Native.remoteAddress(fd.intValue());
    this.local = Native.localAddress(fd.intValue());
  }
  
  public EpollTcpInfo tcpInfo()
  {
    return tcpInfo(new EpollTcpInfo());
  }
  
  public EpollTcpInfo tcpInfo(EpollTcpInfo info)
  {
    Native.tcpInfo(fd().intValue(), info);
    return info;
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  protected SocketAddress localAddress0()
  {
    return this.local;
  }
  
  protected SocketAddress remoteAddress0()
  {
    if (this.remote == null)
    {
      InetSocketAddress address = Native.remoteAddress(fd().intValue());
      if (address != null) {
        this.remote = address;
      }
      return address;
    }
    return this.remote;
  }
  
  protected void doBind(SocketAddress local)
    throws Exception
  {
    InetSocketAddress localAddress = (InetSocketAddress)local;
    int fd = fd().intValue();
    Native.bind(fd, localAddress);
    this.local = Native.localAddress(fd);
  }
  
  public EpollSocketChannelConfig config()
  {
    return this.config;
  }
  
  public boolean isInputShutdown()
  {
    return isInputShutdown0();
  }
  
  public boolean isOutputShutdown()
  {
    return isOutputShutdown0();
  }
  
  public ChannelFuture shutdownOutput()
  {
    return shutdownOutput(newPromise());
  }
  
  public ChannelFuture shutdownOutput(final ChannelPromise promise)
  {
    Executor closeExecutor = ((EpollSocketChannelUnsafe)unsafe()).closeExecutor();
    if (closeExecutor != null)
    {
      closeExecutor.execute(new OneTimeTask()
      {
        public void run()
        {
          EpollSocketChannel.this.shutdownOutput0(promise);
        }
      });
    }
    else
    {
      EventLoop loop = eventLoop();
      if (loop.inEventLoop()) {
        shutdownOutput0(promise);
      } else {
        loop.execute(new OneTimeTask()
        {
          public void run()
          {
            EpollSocketChannel.this.shutdownOutput0(promise);
          }
        });
      }
    }
    return promise;
  }
  
  public ServerSocketChannel parent()
  {
    return (ServerSocketChannel)super.parent();
  }
  
  protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe()
  {
    return new EpollSocketChannelUnsafe(null);
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    if (localAddress != null) {
      checkResolvable((InetSocketAddress)localAddress);
    }
    checkResolvable((InetSocketAddress)remoteAddress);
    if (super.doConnect(remoteAddress, localAddress))
    {
      int fd = fd().intValue();
      this.local = Native.localAddress(fd);
      this.remote = ((InetSocketAddress)remoteAddress);
      return true;
    }
    return false;
  }
  
  private final class EpollSocketChannelUnsafe
    extends AbstractEpollStreamChannel.EpollStreamUnsafe
  {
    private EpollSocketChannelUnsafe()
    {
      super();
    }
    
    protected Executor closeExecutor()
    {
      if (EpollSocketChannel.this.config().getSoLinger() > 0) {
        return GlobalEventExecutor.INSTANCE;
      }
      return null;
    }
  }
}
