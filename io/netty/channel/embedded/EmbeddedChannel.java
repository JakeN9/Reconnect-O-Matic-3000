package io.netty.channel.embedded;

import io.netty.channel.AbstractChannel;
import io.netty.channel.AbstractChannel.AbstractUnsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;

public class EmbeddedChannel
  extends AbstractChannel
{
  private static final SocketAddress LOCAL_ADDRESS = new EmbeddedSocketAddress();
  private static final SocketAddress REMOTE_ADDRESS = new EmbeddedSocketAddress();
  private static final ChannelHandler[] EMPTY_HANDLERS = new ChannelHandler[0];
  
  private static enum State
  {
    OPEN,  ACTIVE,  CLOSED;
    
    private State() {}
  }
  
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(EmbeddedChannel.class);
  private static final ChannelMetadata METADATA = new ChannelMetadata(false);
  private final EmbeddedEventLoop loop = new EmbeddedEventLoop();
  private final ChannelConfig config = new DefaultChannelConfig(this);
  private final Queue<Object> inboundMessages = new ArrayDeque();
  private final Queue<Object> outboundMessages = new ArrayDeque();
  private Throwable lastException;
  private State state;
  
  public EmbeddedChannel()
  {
    this(EMPTY_HANDLERS);
  }
  
  public EmbeddedChannel(ChannelHandler... handlers)
  {
    super(null, EmbeddedChannelId.INSTANCE);
    if (handlers == null) {
      throw new NullPointerException("handlers");
    }
    ChannelPipeline p = pipeline();
    for (ChannelHandler h : handlers)
    {
      if (h == null) {
        break;
      }
      p.addLast(new ChannelHandler[] { h });
    }
    this.loop.register(this);
    p.addLast(new ChannelHandler[] { new LastInboundHandler(null) });
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public ChannelConfig config()
  {
    return this.config;
  }
  
  public boolean isOpen()
  {
    return this.state != State.CLOSED;
  }
  
  public boolean isActive()
  {
    return this.state == State.ACTIVE;
  }
  
  public Queue<Object> inboundMessages()
  {
    return this.inboundMessages;
  }
  
  @Deprecated
  public Queue<Object> lastInboundBuffer()
  {
    return inboundMessages();
  }
  
  public Queue<Object> outboundMessages()
  {
    return this.outboundMessages;
  }
  
  @Deprecated
  public Queue<Object> lastOutboundBuffer()
  {
    return outboundMessages();
  }
  
  public <T> T readInbound()
  {
    return (T)this.inboundMessages.poll();
  }
  
  public <T> T readOutbound()
  {
    return (T)this.outboundMessages.poll();
  }
  
  public boolean writeInbound(Object... msgs)
  {
    ensureOpen();
    if (msgs.length == 0) {
      return !this.inboundMessages.isEmpty();
    }
    ChannelPipeline p = pipeline();
    for (Object m : msgs) {
      p.fireChannelRead(m);
    }
    p.fireChannelReadComplete();
    runPendingTasks();
    checkException();
    return !this.inboundMessages.isEmpty();
  }
  
  public boolean writeOutbound(Object... msgs)
  {
    ensureOpen();
    if (msgs.length == 0) {
      return !this.outboundMessages.isEmpty();
    }
    RecyclableArrayList futures = RecyclableArrayList.newInstance(msgs.length);
    try
    {
      for (Object m : msgs)
      {
        if (m == null) {
          break;
        }
        futures.add(write(m));
      }
      flush();
      
      int size = futures.size();
      for (int i = 0; i < size; i++)
      {
        ChannelFuture future = (ChannelFuture)futures.get(i);
        assert (future.isDone());
        if (future.cause() != null) {
          recordException(future.cause());
        }
      }
      runPendingTasks();
      checkException();
      return !this.outboundMessages.isEmpty() ? 1 : 0;
    }
    finally
    {
      futures.recycle();
    }
  }
  
  public boolean finish()
  {
    close();
    runPendingTasks();
    
    this.loop.cancelScheduledTasks();
    
    checkException();
    
    return (!this.inboundMessages.isEmpty()) || (!this.outboundMessages.isEmpty());
  }
  
  public void runPendingTasks()
  {
    try
    {
      this.loop.runTasks();
    }
    catch (Exception e)
    {
      recordException(e);
    }
    try
    {
      this.loop.runScheduledTasks();
    }
    catch (Exception e)
    {
      recordException(e);
    }
  }
  
  public long runScheduledPendingTasks()
  {
    try
    {
      return this.loop.runScheduledTasks();
    }
    catch (Exception e)
    {
      recordException(e);
    }
    return this.loop.nextScheduledTask();
  }
  
  private void recordException(Throwable cause)
  {
    if (this.lastException == null) {
      this.lastException = cause;
    } else {
      logger.warn("More than one exception was raised. Will report only the first one and log others.", cause);
    }
  }
  
  public void checkException()
  {
    Throwable t = this.lastException;
    if (t == null) {
      return;
    }
    this.lastException = null;
    
    PlatformDependent.throwException(t);
  }
  
  protected final void ensureOpen()
  {
    if (!isOpen())
    {
      recordException(new ClosedChannelException());
      checkException();
    }
  }
  
  protected boolean isCompatible(EventLoop loop)
  {
    return loop instanceof EmbeddedEventLoop;
  }
  
  protected SocketAddress localAddress0()
  {
    return isActive() ? LOCAL_ADDRESS : null;
  }
  
  protected SocketAddress remoteAddress0()
  {
    return isActive() ? REMOTE_ADDRESS : null;
  }
  
  protected void doRegister()
    throws Exception
  {
    this.state = State.ACTIVE;
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {}
  
  protected void doDisconnect()
    throws Exception
  {
    doClose();
  }
  
  protected void doClose()
    throws Exception
  {
    this.state = State.CLOSED;
  }
  
  protected void doBeginRead()
    throws Exception
  {}
  
  protected AbstractChannel.AbstractUnsafe newUnsafe()
  {
    return new DefaultUnsafe(null);
  }
  
  protected void doWrite(ChannelOutboundBuffer in)
    throws Exception
  {
    for (;;)
    {
      Object msg = in.current();
      if (msg == null) {
        break;
      }
      ReferenceCountUtil.retain(msg);
      this.outboundMessages.add(msg);
      in.remove();
    }
  }
  
  private class DefaultUnsafe
    extends AbstractChannel.AbstractUnsafe
  {
    private DefaultUnsafe()
    {
      super();
    }
    
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    {
      safeSetSuccess(promise);
    }
  }
  
  private final class LastInboundHandler
    extends ChannelHandlerAdapter
  {
    private LastInboundHandler() {}
    
    public void channelRead(ChannelHandlerContext ctx, Object msg)
      throws Exception
    {
      EmbeddedChannel.this.inboundMessages.add(msg);
    }
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception
    {
      EmbeddedChannel.this.recordException(cause);
    }
  }
}
