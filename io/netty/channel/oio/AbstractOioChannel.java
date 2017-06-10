package io.netty.channel.oio;

import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.ThreadPerChannelEventLoop;
import java.net.SocketAddress;

public abstract class AbstractOioChannel
  extends AbstractChannel
{
  protected static final int SO_TIMEOUT = 1000;
  private volatile boolean readPending;
  private final Runnable readTask = new Runnable()
  {
    public void run()
    {
      if ((!AbstractOioChannel.this.isReadPending()) && (!AbstractOioChannel.this.config().isAutoRead())) {
        return;
      }
      AbstractOioChannel.this.setReadPending(false);
      AbstractOioChannel.this.doRead();
    }
  };
  
  protected AbstractOioChannel(Channel parent)
  {
    super(parent);
  }
  
  protected AbstractChannel.AbstractUnsafe newUnsafe()
  {
    return new DefaultOioUnsafe(null);
  }
  
  private final class DefaultOioUnsafe
    extends AbstractChannel.AbstractUnsafe
  {
    private DefaultOioUnsafe()
    {
      super();
    }
    
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
        return;
      }
      try
      {
        boolean wasActive = AbstractOioChannel.this.isActive();
        AbstractOioChannel.this.doConnect(remoteAddress, localAddress);
        safeSetSuccess(promise);
        if ((!wasActive) && (AbstractOioChannel.this.isActive())) {
          AbstractOioChannel.this.pipeline().fireChannelActive();
        }
      }
      catch (Throwable t)
      {
        safeSetFailure(promise, annotateConnectException(t, remoteAddress));
        closeIfClosed();
      }
    }
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof ThreadPerChannelEventLoop;
  }
  
  protected abstract void doConnect(SocketAddress paramSocketAddress1, SocketAddress paramSocketAddress2)
    throws Exception;
  
  protected void doBeginRead()
    throws Exception
  {
    if (isReadPending()) {
      return;
    }
    setReadPending(true);
    eventLoop().execute(this.readTask);
  }
  
  protected abstract void doRead();
  
  protected boolean isReadPending()
  {
    return this.readPending;
  }
  
  protected void setReadPending(boolean readPending)
  {
    this.readPending = readPending;
  }
}
