package io.netty.channel.socket.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.FileRegion;
import io.netty.channel.nio.AbstractNioByteChannel;
import io.netty.channel.nio.AbstractNioByteChannel.NioByteUnsafe;
import io.netty.channel.nio.AbstractNioChannel.AbstractNioUnsafe;
import io.netty.channel.socket.DefaultSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.OneTimeTask;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

public class NioSocketChannel
  extends AbstractNioByteChannel
  implements io.netty.channel.socket.SocketChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(false);
  private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();
  private final SocketChannelConfig config;
  
  private static java.nio.channels.SocketChannel newSocket(SelectorProvider provider)
  {
    try
    {
      return provider.openSocketChannel();
    }
    catch (IOException e)
    {
      throw new ChannelException("Failed to open a socket.", e);
    }
  }
  
  public NioSocketChannel()
  {
    this(DEFAULT_SELECTOR_PROVIDER);
  }
  
  public NioSocketChannel(SelectorProvider provider)
  {
    this(newSocket(provider));
  }
  
  public NioSocketChannel(java.nio.channels.SocketChannel socket)
  {
    this(null, socket);
  }
  
  public NioSocketChannel(Channel parent, java.nio.channels.SocketChannel socket)
  {
    super(parent, socket);
    this.config = new NioSocketChannelConfig(this, socket.socket(), null);
  }
  
  public ServerSocketChannel parent()
  {
    return (ServerSocketChannel)super.parent();
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public SocketChannelConfig config()
  {
    return this.config;
  }
  
  protected java.nio.channels.SocketChannel javaChannel()
  {
    return (java.nio.channels.SocketChannel)super.javaChannel();
  }
  
  public boolean isActive()
  {
    java.nio.channels.SocketChannel ch = javaChannel();
    return (ch.isOpen()) && (ch.isConnected());
  }
  
  public boolean isInputShutdown()
  {
    return super.isInputShutdown();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  public boolean isOutputShutdown()
  {
    return (javaChannel().socket().isOutputShutdown()) || (!isActive());
  }
  
  public ChannelFuture shutdownOutput()
  {
    return shutdownOutput(newPromise());
  }
  
  public ChannelFuture shutdownOutput(final ChannelPromise promise)
  {
    Executor closeExecutor = ((NioSocketChannelUnsafe)unsafe()).closeExecutor();
    if (closeExecutor != null)
    {
      closeExecutor.execute(new OneTimeTask()
      {
        public void run()
        {
          NioSocketChannel.this.shutdownOutput0(promise);
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
            NioSocketChannel.this.shutdownOutput0(promise);
          }
        });
      }
    }
    return promise;
  }
  
  private void shutdownOutput0(ChannelPromise promise)
  {
    try
    {
      javaChannel().socket().shutdownOutput();
      promise.setSuccess();
    }
    catch (Throwable t)
    {
      promise.setFailure(t);
    }
  }
  
  protected SocketAddress localAddress0()
  {
    return javaChannel().socket().getLocalSocketAddress();
  }
  
  protected SocketAddress remoteAddress0()
  {
    return javaChannel().socket().getRemoteSocketAddress();
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {
    javaChannel().socket().bind(localAddress);
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    if (localAddress != null) {
      javaChannel().socket().bind(localAddress);
    }
    boolean success = false;
    try
    {
      boolean connected = javaChannel().connect(remoteAddress);
      if (!connected) {
        selectionKey().interestOps(8);
      }
      success = true;
      return connected;
    }
    finally
    {
      if (!success) {
        doClose();
      }
    }
  }
  
  protected void doFinishConnect()
    throws Exception
  {
    if (!javaChannel().finishConnect()) {
      throw new Error();
    }
  }
  
  protected void doDisconnect()
    throws Exception
  {
    doClose();
  }
  
  protected void doClose()
    throws Exception
  {
    javaChannel().close();
  }
  
  protected int doReadBytes(ByteBuf byteBuf)
    throws Exception
  {
    return byteBuf.writeBytes(javaChannel(), byteBuf.writableBytes());
  }
  
  protected int doWriteBytes(ByteBuf buf)
    throws Exception
  {
    int expectedWrittenBytes = buf.readableBytes();
    return buf.readBytes(javaChannel(), expectedWrittenBytes);
  }
  
  protected long doWriteFileRegion(FileRegion region)
    throws Exception
  {
    long position = region.transfered();
    return region.transferTo(javaChannel(), position);
  }
  
  protected void doWrite(ChannelOutboundBuffer in)
    throws Exception
  {
    for (;;)
    {
      int size = in.size();
      if (size == 0)
      {
        clearOpWrite();
        break;
      }
      long writtenBytes = 0L;
      boolean done = false;
      boolean setOpWrite = false;
      
      ByteBuffer[] nioBuffers = in.nioBuffers();
      int nioBufferCnt = in.nioBufferCount();
      long expectedWrittenBytes = in.nioBufferSize();
      java.nio.channels.SocketChannel ch = javaChannel();
      switch (nioBufferCnt)
      {
      case 0: 
        super.doWrite(in);
        return;
      case 1: 
        ByteBuffer nioBuffer = nioBuffers[0];
        for (int i = config().getWriteSpinCount() - 1; i >= 0; i--)
        {
          int localWrittenBytes = ch.write(nioBuffer);
          if (localWrittenBytes == 0)
          {
            setOpWrite = true;
            break;
          }
          expectedWrittenBytes -= localWrittenBytes;
          writtenBytes += localWrittenBytes;
          if (expectedWrittenBytes == 0L)
          {
            done = true;
            break;
          }
        }
        break;
      default: 
        for (int i = config().getWriteSpinCount() - 1; i >= 0; i--)
        {
          long localWrittenBytes = ch.write(nioBuffers, 0, nioBufferCnt);
          if (localWrittenBytes == 0L)
          {
            setOpWrite = true;
            break;
          }
          expectedWrittenBytes -= localWrittenBytes;
          writtenBytes += localWrittenBytes;
          if (expectedWrittenBytes == 0L)
          {
            done = true;
            break;
          }
        }
      }
      in.removeBytes(writtenBytes);
      if (!done)
      {
        incompleteWrite(setOpWrite);
        break;
      }
    }
  }
  
  protected AbstractNioChannel.AbstractNioUnsafe newUnsafe()
  {
    return new NioSocketChannelUnsafe(null);
  }
  
  private final class NioSocketChannelUnsafe
    extends AbstractNioByteChannel.NioByteUnsafe
  {
    private NioSocketChannelUnsafe()
    {
      super();
    }
    
    protected Executor closeExecutor()
    {
      if (NioSocketChannel.this.config().getSoLinger() > 0) {
        return GlobalEventExecutor.INSTANCE;
      }
      return null;
    }
  }
  
  private final class NioSocketChannelConfig
    extends DefaultSocketChannelConfig
  {
    private NioSocketChannelConfig(NioSocketChannel channel, Socket javaSocket)
    {
      super(javaSocket);
    }
    
    protected void autoReadCleared()
    {
      NioSocketChannel.this.setReadPending(false);
    }
  }
}
