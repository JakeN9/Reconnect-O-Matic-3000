package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.channel.unix.FileDescriptor;
import java.net.SocketAddress;

public final class EpollDomainSocketChannel
  extends AbstractEpollStreamChannel
  implements DomainSocketChannel
{
  private final EpollDomainSocketChannelConfig config = new EpollDomainSocketChannelConfig(this);
  private volatile DomainSocketAddress local;
  private volatile DomainSocketAddress remote;
  
  public EpollDomainSocketChannel()
  {
    super(Native.socketDomainFd());
  }
  
  public EpollDomainSocketChannel(Channel parent, FileDescriptor fd)
  {
    super(parent, fd.intValue());
  }
  
  public EpollDomainSocketChannel(FileDescriptor fd)
  {
    super(fd);
  }
  
  EpollDomainSocketChannel(Channel parent, int fd)
  {
    super(parent, fd);
  }
  
  protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe()
  {
    return new EpollDomainUnsafe(null);
  }
  
  protected DomainSocketAddress localAddress0()
  {
    return this.local;
  }
  
  protected DomainSocketAddress remoteAddress0()
  {
    return this.remote;
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {
    Native.bind(fd().intValue(), localAddress);
    this.local = ((DomainSocketAddress)localAddress);
  }
  
  public EpollDomainSocketChannelConfig config()
  {
    return this.config;
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    if (super.doConnect(remoteAddress, localAddress))
    {
      this.local = ((DomainSocketAddress)localAddress);
      this.remote = ((DomainSocketAddress)remoteAddress);
      return true;
    }
    return false;
  }
  
  public DomainSocketAddress remoteAddress()
  {
    return (DomainSocketAddress)super.remoteAddress();
  }
  
  public DomainSocketAddress localAddress()
  {
    return (DomainSocketAddress)super.localAddress();
  }
  
  protected boolean doWriteSingle(ChannelOutboundBuffer in, int writeSpinCount)
    throws Exception
  {
    Object msg = in.current();
    if (((msg instanceof FileDescriptor)) && (Native.sendFd(fd().intValue(), ((FileDescriptor)msg).intValue()) > 0))
    {
      in.remove();
      return true;
    }
    return super.doWriteSingle(in, writeSpinCount);
  }
  
  protected Object filterOutboundMessage(Object msg)
  {
    if ((msg instanceof FileDescriptor)) {
      return msg;
    }
    return super.filterOutboundMessage(msg);
  }
  
  private final class EpollDomainUnsafe
    extends AbstractEpollStreamChannel.EpollStreamUnsafe
  {
    private EpollDomainUnsafe()
    {
      super();
    }
    
    void epollInReady()
    {
      switch (EpollDomainSocketChannel.1.$SwitchMap$io$netty$channel$unix$DomainSocketReadMode[EpollDomainSocketChannel.this.config().getReadMode().ordinal()])
      {
      case 1: 
        super.epollInReady();
        break;
      case 2: 
        epollInReadFd();
        break;
      default: 
        throw new Error();
      }
    }
    
    private void epollInReadFd()
    {
      boolean edgeTriggered = EpollDomainSocketChannel.this.isFlagSet(Native.EPOLLET);
      ChannelConfig config = EpollDomainSocketChannel.this.config();
      if ((!this.readPending) && (!edgeTriggered) && (!config.isAutoRead()))
      {
        clearEpollIn0();
        return;
      }
      ChannelPipeline pipeline = EpollDomainSocketChannel.this.pipeline();
      try
      {
        int maxMessagesPerRead = edgeTriggered ? Integer.MAX_VALUE : config.getMaxMessagesPerRead();
        
        int messages = 0;
        label72:
        int socketFd = Native.recvFd(EpollDomainSocketChannel.this.fd().intValue());
        if (socketFd != 0)
        {
          if (socketFd == -1)
          {
            close(voidPromise()); return;
          }
          this.readPending = false;
          try
          {
            pipeline.fireChannelRead(new FileDescriptor(socketFd));
            if ((edgeTriggered) || (config.isAutoRead())) {}
          }
          catch (Throwable t)
          {
            pipeline.fireChannelReadComplete();
            pipeline.fireExceptionCaught(t);
            if ((edgeTriggered) || (config.isAutoRead())) {}
          }
          finally
          {
            if ((edgeTriggered) || (config.isAutoRead()))
            {
              throw ((Throwable)localObject1);
              
              messages++;
              if (messages < maxMessagesPerRead) {
                break label72;
              }
            }
          }
        }
        pipeline.fireChannelReadComplete();
      }
      catch (Throwable t)
      {
        pipeline.fireChannelReadComplete();
        pipeline.fireExceptionCaught(t);
        
        EpollDomainSocketChannel.this.eventLoop().execute(new Runnable()
        {
          public void run()
          {
            EpollDomainSocketChannel.EpollDomainUnsafe.this.epollInReady();
          }
        });
      }
      finally
      {
        if ((!this.readPending) && (!config.isAutoRead())) {
          clearEpollIn0();
        }
      }
    }
  }
}
