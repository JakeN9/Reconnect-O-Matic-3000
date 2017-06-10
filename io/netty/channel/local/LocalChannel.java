package io.netty.channel.local;

import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.InternalThreadLocalMap;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

public class LocalChannel
  extends AbstractChannel
{
  private static enum State
  {
    OPEN,  BOUND,  CONNECTED,  CLOSED;
    
    private State() {}
  }
  
  private static final ChannelMetadata METADATA = new ChannelMetadata(false);
  private static final int MAX_READER_STACK_DEPTH = 8;
  private final ChannelConfig config = new DefaultChannelConfig(this);
  private final Queue<Object> inboundBuffer = new ArrayDeque();
  private final Runnable readTask = new Runnable()
  {
    public void run()
    {
      ChannelPipeline pipeline = LocalChannel.this.pipeline();
      for (;;)
      {
        Object m = LocalChannel.this.inboundBuffer.poll();
        if (m == null) {
          break;
        }
        pipeline.fireChannelRead(m);
      }
      pipeline.fireChannelReadComplete();
    }
  };
  private final Runnable shutdownHook = new Runnable()
  {
    public void run()
    {
      LocalChannel.this.unsafe().close(LocalChannel.this.unsafe().voidPromise());
    }
  };
  private volatile State state;
  private volatile LocalChannel peer;
  private volatile LocalAddress localAddress;
  private volatile LocalAddress remoteAddress;
  private volatile ChannelPromise connectPromise;
  private volatile boolean readInProgress;
  private volatile boolean registerInProgress;
  
  public LocalChannel()
  {
    super(null);
  }
  
  LocalChannel(LocalServerChannel parent, LocalChannel peer)
  {
    super(parent);
    this.peer = peer;
    this.localAddress = parent.localAddress();
    this.remoteAddress = peer.localAddress();
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public ChannelConfig config()
  {
    return this.config;
  }
  
  public LocalServerChannel parent()
  {
    return (LocalServerChannel)super.parent();
  }
  
  public LocalAddress localAddress()
  {
    return (LocalAddress)super.localAddress();
  }
  
  public LocalAddress remoteAddress()
  {
    return (LocalAddress)super.remoteAddress();
  }
  
  public boolean isOpen()
  {
    return this.state != State.CLOSED;
  }
  
  public boolean isActive()
  {
    return this.state == State.CONNECTED;
  }
  
  protected AbstractChannel.AbstractUnsafe newUnsafe()
  {
    return new LocalUnsafe(null);
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof SingleThreadEventLoop;
  }
  
  protected SocketAddress localAddress0()
  {
    return this.localAddress;
  }
  
  protected SocketAddress remoteAddress0()
  {
    return this.remoteAddress;
  }
  
  protected void doRegister()
    throws Exception
  {
    if ((this.peer != null) && (parent() != null))
    {
      final LocalChannel peer = this.peer;
      this.registerInProgress = true;
      this.state = State.CONNECTED;
      
      peer.remoteAddress = (parent() == null ? null : parent().localAddress());
      peer.state = State.CONNECTED;
      
      peer.eventLoop().execute(new Runnable()
      {
        public void run()
        {
          LocalChannel.this.registerInProgress = false;
          peer.pipeline().fireChannelActive();
          peer.connectPromise.setSuccess();
        }
      });
    }
    ((SingleThreadEventLoop)eventLoop().unwrap()).addShutdownHook(this.shutdownHook);
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {
    this.localAddress = LocalChannelRegistry.register(this, this.localAddress, localAddress);
    
    this.state = State.BOUND;
  }
  
  protected void doDisconnect()
    throws Exception
  {
    doClose();
  }
  
  protected void doClose()
    throws Exception
  {
    if (this.state != State.CLOSED)
    {
      if (this.localAddress != null)
      {
        if (parent() == null) {
          LocalChannelRegistry.unregister(this.localAddress);
        }
        this.localAddress = null;
      }
      this.state = State.CLOSED;
    }
    final LocalChannel peer = this.peer;
    if ((peer != null) && (peer.isActive()))
    {
      EventLoop eventLoop = peer.eventLoop();
      if ((eventLoop.inEventLoop()) && (!this.registerInProgress)) {
        peer.unsafe().close(unsafe().voidPromise());
      } else {
        peer.eventLoop().execute(new Runnable()
        {
          public void run()
          {
            peer.unsafe().close(LocalChannel.this.unsafe().voidPromise());
          }
        });
      }
      this.peer = null;
    }
  }
  
  protected void doDeregister()
    throws Exception
  {
    ((SingleThreadEventLoop)eventLoop().unwrap()).removeShutdownHook(this.shutdownHook);
  }
  
  protected void doBeginRead()
    throws Exception
  {
    if (this.readInProgress) {
      return;
    }
    ChannelPipeline pipeline = pipeline();
    Queue<Object> inboundBuffer = this.inboundBuffer;
    if (inboundBuffer.isEmpty())
    {
      this.readInProgress = true;
      return;
    }
    InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
    Integer stackDepth = Integer.valueOf(threadLocals.localChannelReaderStackDepth());
    if (stackDepth.intValue() < 8)
    {
      threadLocals.setLocalChannelReaderStackDepth(stackDepth.intValue() + 1);
      try
      {
        for (;;)
        {
          Object received = inboundBuffer.poll();
          if (received == null) {
            break;
          }
          pipeline.fireChannelRead(received);
        }
        pipeline.fireChannelReadComplete();
      }
      finally
      {
        threadLocals.setLocalChannelReaderStackDepth(stackDepth.intValue());
      }
    }
    else
    {
      eventLoop().execute(this.readTask);
    }
  }
  
  protected void doWrite(ChannelOutboundBuffer in)
    throws Exception
  {
    switch (this.state)
    {
    case OPEN: 
    case BOUND: 
      throw new NotYetConnectedException();
    case CLOSED: 
      throw new ClosedChannelException();
    }
    final LocalChannel peer = this.peer;
    final ChannelPipeline peerPipeline = peer.pipeline();
    EventLoop peerLoop = peer.eventLoop();
    if (peerLoop == eventLoop())
    {
      for (;;)
      {
        Object msg = in.current();
        if (msg == null) {
          break;
        }
        peer.inboundBuffer.add(msg);
        ReferenceCountUtil.retain(msg);
        in.remove();
      }
      finishPeerRead(peer, peerPipeline);
    }
    else
    {
      final Object[] msgsCopy = new Object[in.size()];
      for (int i = 0; i < msgsCopy.length; i++)
      {
        msgsCopy[i] = ReferenceCountUtil.retain(in.current());
        in.remove();
      }
      peerLoop.execute(new Runnable()
      {
        public void run()
        {
          Collections.addAll(peer.inboundBuffer, msgsCopy);
          LocalChannel.finishPeerRead(peer, peerPipeline);
        }
      });
    }
  }
  
  private static void finishPeerRead(LocalChannel peer, ChannelPipeline peerPipeline)
  {
    if (peer.readInProgress)
    {
      peer.readInProgress = false;
      for (;;)
      {
        Object received = peer.inboundBuffer.poll();
        if (received == null) {
          break;
        }
        peerPipeline.fireChannelRead(received);
      }
      peerPipeline.fireChannelReadComplete();
    }
  }
  
  private class LocalUnsafe
    extends AbstractChannel.AbstractUnsafe
  {
    private LocalUnsafe()
    {
      super();
    }
    
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
        return;
      }
      if (LocalChannel.this.state == LocalChannel.State.CONNECTED)
      {
        Exception cause = new AlreadyConnectedException();
        safeSetFailure(promise, cause);
        LocalChannel.this.pipeline().fireExceptionCaught(cause);
        return;
      }
      if (LocalChannel.this.connectPromise != null) {
        throw new ConnectionPendingException();
      }
      LocalChannel.this.connectPromise = promise;
      if (LocalChannel.this.state != LocalChannel.State.BOUND) {
        if (localAddress == null) {
          localAddress = new LocalAddress(LocalChannel.this);
        }
      }
      if (localAddress != null) {
        try
        {
          LocalChannel.this.doBind(localAddress);
        }
        catch (Throwable t)
        {
          safeSetFailure(promise, t);
          close(voidPromise());
          return;
        }
      }
      Channel boundChannel = LocalChannelRegistry.get(remoteAddress);
      if (!(boundChannel instanceof LocalServerChannel))
      {
        Exception cause = new ChannelException("connection refused");
        safeSetFailure(promise, cause);
        close(voidPromise());
        return;
      }
      LocalServerChannel serverChannel = (LocalServerChannel)boundChannel;
      LocalChannel.this.peer = serverChannel.serve(LocalChannel.this);
    }
  }
}
