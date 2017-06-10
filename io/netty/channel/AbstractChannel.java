package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.OneTimeTask;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public abstract class AbstractChannel
  extends DefaultAttributeMap
  implements Channel
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractChannel.class);
  static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();
  static final NotYetConnectedException NOT_YET_CONNECTED_EXCEPTION = new NotYetConnectedException();
  private MessageSizeEstimator.Handle estimatorHandle;
  private final Channel parent;
  private final ChannelId id;
  private final Channel.Unsafe unsafe;
  private final DefaultChannelPipeline pipeline;
  
  static
  {
    CLOSED_CHANNEL_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    NOT_YET_CONNECTED_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
  }
  
  private final ChannelFuture succeededFuture = new SucceededChannelFuture(this, null);
  private final VoidChannelPromise voidPromise = new VoidChannelPromise(this, true);
  private final VoidChannelPromise unsafeVoidPromise = new VoidChannelPromise(this, false);
  private final CloseFuture closeFuture = new CloseFuture(this);
  private volatile SocketAddress localAddress;
  private volatile SocketAddress remoteAddress;
  private volatile PausableChannelEventLoop eventLoop;
  private volatile boolean registered;
  private boolean strValActive;
  private String strVal;
  
  protected AbstractChannel(Channel parent)
  {
    this.parent = parent;
    this.id = DefaultChannelId.newInstance();
    this.unsafe = newUnsafe();
    this.pipeline = new DefaultChannelPipeline(this);
  }
  
  protected AbstractChannel(Channel parent, ChannelId id)
  {
    this.parent = parent;
    this.id = id;
    this.unsafe = newUnsafe();
    this.pipeline = new DefaultChannelPipeline(this);
  }
  
  public final ChannelId id()
  {
    return this.id;
  }
  
  public boolean isWritable()
  {
    ChannelOutboundBuffer buf = this.unsafe.outboundBuffer();
    return (buf != null) && (buf.isWritable());
  }
  
  public Channel parent()
  {
    return this.parent;
  }
  
  public ChannelPipeline pipeline()
  {
    return this.pipeline;
  }
  
  public ByteBufAllocator alloc()
  {
    return config().getAllocator();
  }
  
  public final EventLoop eventLoop()
  {
    EventLoop eventLoop = this.eventLoop;
    if (eventLoop == null) {
      throw new IllegalStateException("channel not registered to an event loop");
    }
    return eventLoop;
  }
  
  public SocketAddress localAddress()
  {
    SocketAddress localAddress = this.localAddress;
    if (localAddress == null) {
      try
      {
        this.localAddress = (localAddress = unsafe().localAddress());
      }
      catch (Throwable t)
      {
        return null;
      }
    }
    return localAddress;
  }
  
  protected void invalidateLocalAddress()
  {
    this.localAddress = null;
  }
  
  public SocketAddress remoteAddress()
  {
    SocketAddress remoteAddress = this.remoteAddress;
    if (remoteAddress == null) {
      try
      {
        this.remoteAddress = (remoteAddress = unsafe().remoteAddress());
      }
      catch (Throwable t)
      {
        return null;
      }
    }
    return remoteAddress;
  }
  
  protected void invalidateRemoteAddress()
  {
    this.remoteAddress = null;
  }
  
  public boolean isRegistered()
  {
    return this.registered;
  }
  
  public ChannelFuture bind(SocketAddress localAddress)
  {
    return this.pipeline.bind(localAddress);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress)
  {
    return this.pipeline.connect(remoteAddress);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress)
  {
    return this.pipeline.connect(remoteAddress, localAddress);
  }
  
  public ChannelFuture disconnect()
  {
    return this.pipeline.disconnect();
  }
  
  public ChannelFuture close()
  {
    return this.pipeline.close();
  }
  
  public ChannelFuture deregister()
  {
    this.eventLoop.rejectNewTasks();
    return this.pipeline.deregister();
  }
  
  public Channel flush()
  {
    this.pipeline.flush();
    return this;
  }
  
  public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise)
  {
    return this.pipeline.bind(localAddress, promise);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise)
  {
    return this.pipeline.connect(remoteAddress, promise);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
  {
    return this.pipeline.connect(remoteAddress, localAddress, promise);
  }
  
  public ChannelFuture disconnect(ChannelPromise promise)
  {
    return this.pipeline.disconnect(promise);
  }
  
  public ChannelFuture close(ChannelPromise promise)
  {
    return this.pipeline.close(promise);
  }
  
  public ChannelFuture deregister(ChannelPromise promise)
  {
    this.eventLoop.rejectNewTasks();
    return this.pipeline.deregister(promise);
  }
  
  public Channel read()
  {
    this.pipeline.read();
    return this;
  }
  
  public ChannelFuture write(Object msg)
  {
    return this.pipeline.write(msg);
  }
  
  public ChannelFuture write(Object msg, ChannelPromise promise)
  {
    return this.pipeline.write(msg, promise);
  }
  
  public ChannelFuture writeAndFlush(Object msg)
  {
    return this.pipeline.writeAndFlush(msg);
  }
  
  public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise)
  {
    return this.pipeline.writeAndFlush(msg, promise);
  }
  
  public ChannelPromise newPromise()
  {
    return new DefaultChannelPromise(this);
  }
  
  public ChannelProgressivePromise newProgressivePromise()
  {
    return new DefaultChannelProgressivePromise(this);
  }
  
  public ChannelFuture newSucceededFuture()
  {
    return this.succeededFuture;
  }
  
  public ChannelFuture newFailedFuture(Throwable cause)
  {
    return new FailedChannelFuture(this, null, cause);
  }
  
  public ChannelFuture closeFuture()
  {
    return this.closeFuture;
  }
  
  public Channel.Unsafe unsafe()
  {
    return this.unsafe;
  }
  
  public final int hashCode()
  {
    return this.id.hashCode();
  }
  
  public final boolean equals(Object o)
  {
    return this == o;
  }
  
  public final int compareTo(Channel o)
  {
    if (this == o) {
      return 0;
    }
    return id().compareTo(o.id());
  }
  
  public String toString()
  {
    boolean active = isActive();
    if ((this.strValActive == active) && (this.strVal != null)) {
      return this.strVal;
    }
    SocketAddress remoteAddr = remoteAddress();
    SocketAddress localAddr = localAddress();
    if (remoteAddr != null)
    {
      SocketAddress dstAddr;
      SocketAddress srcAddr;
      SocketAddress dstAddr;
      if (this.parent == null)
      {
        SocketAddress srcAddr = localAddr;
        dstAddr = remoteAddr;
      }
      else
      {
        srcAddr = remoteAddr;
        dstAddr = localAddr;
      }
      StringBuilder buf = new StringBuilder(96).append("[id: 0x").append(this.id.asShortText()).append(", ").append(srcAddr).append(active ? " => " : " :> ").append(dstAddr).append(']');
      
      this.strVal = buf.toString();
    }
    else if (localAddr != null)
    {
      StringBuilder buf = new StringBuilder(64).append("[id: 0x").append(this.id.asShortText()).append(", ").append(localAddr).append(']');
      
      this.strVal = buf.toString();
    }
    else
    {
      StringBuilder buf = new StringBuilder(16).append("[id: 0x").append(this.id.asShortText()).append(']');
      
      this.strVal = buf.toString();
    }
    this.strValActive = active;
    return this.strVal;
  }
  
  public final ChannelPromise voidPromise()
  {
    return this.voidPromise;
  }
  
  final MessageSizeEstimator.Handle estimatorHandle()
  {
    if (this.estimatorHandle == null) {
      this.estimatorHandle = config().getMessageSizeEstimator().newHandle();
    }
    return this.estimatorHandle;
  }
  
  protected abstract class AbstractUnsafe
    implements Channel.Unsafe
  {
    private ChannelOutboundBuffer outboundBuffer = new ChannelOutboundBuffer(AbstractChannel.this);
    private RecvByteBufAllocator.Handle recvHandle;
    private boolean inFlush0;
    private boolean neverRegistered = true;
    
    protected AbstractUnsafe() {}
    
    public RecvByteBufAllocator.Handle recvBufAllocHandle()
    {
      if (this.recvHandle == null) {
        this.recvHandle = AbstractChannel.this.config().getRecvByteBufAllocator().newHandle();
      }
      return this.recvHandle;
    }
    
    public final ChannelHandlerInvoker invoker()
    {
      return ((PausableChannelEventExecutor)AbstractChannel.this.eventLoop().asInvoker()).unwrapInvoker();
    }
    
    public final ChannelOutboundBuffer outboundBuffer()
    {
      return this.outboundBuffer;
    }
    
    public final SocketAddress localAddress()
    {
      return AbstractChannel.this.localAddress0();
    }
    
    public final SocketAddress remoteAddress()
    {
      return AbstractChannel.this.remoteAddress0();
    }
    
    public final void register(EventLoop eventLoop, final ChannelPromise promise)
    {
      if (eventLoop == null) {
        throw new NullPointerException("eventLoop");
      }
      if (promise == null) {
        throw new NullPointerException("promise");
      }
      if (AbstractChannel.this.isRegistered())
      {
        promise.setFailure(new IllegalStateException("registered to an event loop already"));
        return;
      }
      if (!AbstractChannel.this.isCompatible(eventLoop))
      {
        promise.setFailure(new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
        
        return;
      }
      if (AbstractChannel.this.eventLoop == null) {
        AbstractChannel.this.eventLoop = new AbstractChannel.PausableChannelEventLoop(AbstractChannel.this, eventLoop);
      } else {
        AbstractChannel.this.eventLoop.unwrapped = eventLoop;
      }
      if (eventLoop.inEventLoop()) {
        register0(promise);
      } else {
        try
        {
          eventLoop.execute(new OneTimeTask()
          {
            public void run()
            {
              AbstractChannel.AbstractUnsafe.this.register0(promise);
            }
          });
        }
        catch (Throwable t)
        {
          AbstractChannel.logger.warn("Force-closing a channel whose registration task was not accepted by an event loop: {}", AbstractChannel.this, t);
          
          closeForcibly();
          AbstractChannel.this.closeFuture.setClosed();
          safeSetFailure(promise, t);
        }
      }
    }
    
    private void register0(ChannelPromise promise)
    {
      try
      {
        if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
          return;
        }
        boolean firstRegistration = this.neverRegistered;
        AbstractChannel.this.doRegister();
        this.neverRegistered = false;
        AbstractChannel.this.registered = true;
        AbstractChannel.this.eventLoop.acceptNewTasks();
        safeSetSuccess(promise);
        AbstractChannel.this.pipeline.fireChannelRegistered();
        if ((firstRegistration) && (AbstractChannel.this.isActive())) {
          AbstractChannel.this.pipeline.fireChannelActive();
        }
      }
      catch (Throwable t)
      {
        closeForcibly();
        AbstractChannel.this.closeFuture.setClosed();
        safeSetFailure(promise, t);
      }
    }
    
    public final void bind(SocketAddress localAddress, ChannelPromise promise)
    {
      if ((!promise.setUncancellable()) || (!ensureOpen(promise))) {
        return;
      }
      if ((Boolean.TRUE.equals(AbstractChannel.this.config().getOption(ChannelOption.SO_BROADCAST))) && ((localAddress instanceof InetSocketAddress)) && (!((InetSocketAddress)localAddress).getAddress().isAnyLocalAddress()) && (!PlatformDependent.isWindows()) && (!PlatformDependent.isRoot())) {
        AbstractChannel.logger.warn("A non-root user can't receive a broadcast packet if the socket is not bound to a wildcard address; binding to a non-wildcard address (" + localAddress + ") anyway as requested.");
      }
      boolean wasActive = AbstractChannel.this.isActive();
      try
      {
        AbstractChannel.this.doBind(localAddress);
      }
      catch (Throwable t)
      {
        safeSetFailure(promise, t);
        closeIfClosed();
        return;
      }
      if ((!wasActive) && (AbstractChannel.this.isActive())) {
        invokeLater(new OneTimeTask()
        {
          public void run()
          {
            AbstractChannel.this.pipeline.fireChannelActive();
          }
        });
      }
      safeSetSuccess(promise);
    }
    
    public final void disconnect(ChannelPromise promise)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      boolean wasActive = AbstractChannel.this.isActive();
      try
      {
        AbstractChannel.this.doDisconnect();
      }
      catch (Throwable t)
      {
        safeSetFailure(promise, t);
        closeIfClosed();
        return;
      }
      if ((wasActive) && (!AbstractChannel.this.isActive())) {
        invokeLater(new OneTimeTask()
        {
          public void run()
          {
            AbstractChannel.this.pipeline.fireChannelInactive();
          }
        });
      }
      safeSetSuccess(promise);
      closeIfClosed();
    }
    
    public final void close(final ChannelPromise promise)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      if (this.inFlush0)
      {
        invokeLater(new OneTimeTask()
        {
          public void run()
          {
            AbstractChannel.AbstractUnsafe.this.close(promise);
          }
        });
        return;
      }
      if (this.outboundBuffer == null)
      {
        AbstractChannel.this.closeFuture.addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future)
            throws Exception
          {
            promise.setSuccess();
          }
        });
        return;
      }
      if (AbstractChannel.this.closeFuture.isDone())
      {
        safeSetSuccess(promise);
        return;
      }
      final boolean wasActive = AbstractChannel.this.isActive();
      final ChannelOutboundBuffer buffer = this.outboundBuffer;
      this.outboundBuffer = null;
      Executor closeExecutor = closeExecutor();
      if (closeExecutor != null)
      {
        closeExecutor.execute(new OneTimeTask()
        {
          public void run()
          {
            Throwable cause = null;
            try
            {
              AbstractChannel.this.doClose();
            }
            catch (Throwable t)
            {
              cause = t;
            }
            final Throwable error = cause;
            
            AbstractChannel.AbstractUnsafe.this.invokeLater(new OneTimeTask()
            {
              public void run()
              {
                AbstractChannel.AbstractUnsafe.this.closeAndDeregister(AbstractChannel.AbstractUnsafe.6.this.val$buffer, AbstractChannel.AbstractUnsafe.6.this.val$wasActive, AbstractChannel.AbstractUnsafe.6.this.val$promise, error);
              }
            });
          }
        });
      }
      else
      {
        Throwable error = null;
        try
        {
          AbstractChannel.this.doClose();
        }
        catch (Throwable t)
        {
          error = t;
        }
        closeAndDeregister(buffer, wasActive, promise, error);
      }
    }
    
    private void closeAndDeregister(ChannelOutboundBuffer outboundBuffer, boolean wasActive, ChannelPromise promise, Throwable error)
    {
      try
      {
        outboundBuffer.failFlushed(AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
        outboundBuffer.close(AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
      }
      finally
      {
        if ((wasActive) && (!AbstractChannel.this.isActive())) {
          invokeLater(new OneTimeTask()
          {
            public void run()
            {
              AbstractChannel.this.pipeline.fireChannelInactive();
              AbstractChannel.AbstractUnsafe.this.deregister(AbstractChannel.AbstractUnsafe.this.voidPromise());
            }
          });
        } else {
          invokeLater(new OneTimeTask()
          {
            public void run()
            {
              AbstractChannel.AbstractUnsafe.this.deregister(AbstractChannel.AbstractUnsafe.this.voidPromise());
            }
          });
        }
        AbstractChannel.this.closeFuture.setClosed();
        if (error != null) {
          safeSetFailure(promise, error);
        } else {
          safeSetSuccess(promise);
        }
      }
    }
    
    public final void closeForcibly()
    {
      try
      {
        AbstractChannel.this.doClose();
      }
      catch (Exception e)
      {
        AbstractChannel.logger.warn("Failed to close a channel.", e);
      }
    }
    
    public final void deregister(ChannelPromise promise)
    {
      if (!promise.setUncancellable()) {
        return;
      }
      if (!AbstractChannel.this.registered)
      {
        safeSetSuccess(promise);
        return;
      }
      try
      {
        AbstractChannel.this.doDeregister();
      }
      catch (Throwable t)
      {
        safeSetFailure(promise, t);
        AbstractChannel.logger.warn("Unexpected exception occurred while deregistering a channel.", t);
      }
      finally
      {
        if (AbstractChannel.this.registered)
        {
          AbstractChannel.this.registered = false;
          safeSetSuccess(promise);
          AbstractChannel.this.pipeline.fireChannelUnregistered();
        }
        else
        {
          safeSetSuccess(promise);
        }
      }
    }
    
    public final void beginRead()
    {
      if (!AbstractChannel.this.isActive()) {
        return;
      }
      try
      {
        AbstractChannel.this.doBeginRead();
      }
      catch (Exception e)
      {
        invokeLater(new OneTimeTask()
        {
          public void run()
          {
            AbstractChannel.this.pipeline.fireExceptionCaught(e);
          }
        });
        close(voidPromise());
      }
    }
    
    public final void write(Object msg, ChannelPromise promise)
    {
      ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
      if (outboundBuffer == null)
      {
        safeSetFailure(promise, AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
        
        ReferenceCountUtil.release(msg); return;
      }
      int size;
      try
      {
        msg = AbstractChannel.this.filterOutboundMessage(msg);
        size = AbstractChannel.this.estimatorHandle().size(msg);
        if (size < 0) {
          size = 0;
        }
      }
      catch (Throwable t)
      {
        safeSetFailure(promise, t);
        ReferenceCountUtil.release(msg);
        return;
      }
      outboundBuffer.addMessage(msg, size, promise);
    }
    
    public final void flush()
    {
      ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
      if (outboundBuffer == null) {
        return;
      }
      outboundBuffer.addFlush();
      flush0();
    }
    
    protected void flush0()
    {
      if (this.inFlush0) {
        return;
      }
      ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
      if ((outboundBuffer == null) || (outboundBuffer.isEmpty())) {
        return;
      }
      this.inFlush0 = true;
      if (!AbstractChannel.this.isActive())
      {
        try
        {
          if (AbstractChannel.this.isOpen()) {
            outboundBuffer.failFlushed(AbstractChannel.NOT_YET_CONNECTED_EXCEPTION);
          } else {
            outboundBuffer.failFlushed(AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
          }
        }
        finally
        {
          this.inFlush0 = false;
        }
        return;
      }
      try
      {
        AbstractChannel.this.doWrite(outboundBuffer);
      }
      catch (Throwable t)
      {
        outboundBuffer.failFlushed(t);
      }
      finally
      {
        this.inFlush0 = false;
      }
    }
    
    public final ChannelPromise voidPromise()
    {
      return AbstractChannel.this.unsafeVoidPromise;
    }
    
    protected final boolean ensureOpen(ChannelPromise promise)
    {
      if (AbstractChannel.this.isOpen()) {
        return true;
      }
      safeSetFailure(promise, AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
      return false;
    }
    
    protected final void safeSetSuccess(ChannelPromise promise)
    {
      if ((!(promise instanceof VoidChannelPromise)) && (!promise.trySuccess())) {
        AbstractChannel.logger.warn("Failed to mark a promise as success because it is done already: {}", promise);
      }
    }
    
    protected final void safeSetFailure(ChannelPromise promise, Throwable cause)
    {
      if ((!(promise instanceof VoidChannelPromise)) && (!promise.tryFailure(cause))) {
        AbstractChannel.logger.warn("Failed to mark a promise as failure because it's done already: {}", promise, cause);
      }
    }
    
    protected final void closeIfClosed()
    {
      if (AbstractChannel.this.isOpen()) {
        return;
      }
      close(voidPromise());
    }
    
    private void invokeLater(Runnable task)
    {
      try
      {
        AbstractChannel.this.eventLoop().unwrap().execute(task);
      }
      catch (RejectedExecutionException e)
      {
        AbstractChannel.logger.warn("Can't invoke task later as EventLoop rejected it", e);
      }
    }
    
    protected final Throwable annotateConnectException(Throwable cause, SocketAddress remoteAddress)
    {
      if ((cause instanceof ConnectException))
      {
        Throwable newT = new ConnectException(cause.getMessage() + ": " + remoteAddress);
        newT.setStackTrace(cause.getStackTrace());
        cause = newT;
      }
      else if ((cause instanceof NoRouteToHostException))
      {
        Throwable newT = new NoRouteToHostException(cause.getMessage() + ": " + remoteAddress);
        newT.setStackTrace(cause.getStackTrace());
        cause = newT;
      }
      else if ((cause instanceof SocketException))
      {
        Throwable newT = new SocketException(cause.getMessage() + ": " + remoteAddress);
        newT.setStackTrace(cause.getStackTrace());
        cause = newT;
      }
      return cause;
    }
    
    protected Executor closeExecutor()
    {
      return null;
    }
  }
  
  protected Object filterOutboundMessage(Object msg)
    throws Exception
  {
    return msg;
  }
  
  protected abstract AbstractUnsafe newUnsafe();
  
  protected abstract boolean isCompatible(EventLoop paramEventLoop);
  
  protected abstract SocketAddress localAddress0();
  
  protected abstract SocketAddress remoteAddress0();
  
  protected void doRegister()
    throws Exception
  {}
  
  protected abstract void doBind(SocketAddress paramSocketAddress)
    throws Exception;
  
  protected abstract void doDisconnect()
    throws Exception;
  
  protected abstract void doClose()
    throws Exception;
  
  protected void doDeregister()
    throws Exception
  {}
  
  protected abstract void doBeginRead()
    throws Exception;
  
  protected abstract void doWrite(ChannelOutboundBuffer paramChannelOutboundBuffer)
    throws Exception;
  
  static final class CloseFuture
    extends DefaultChannelPromise
  {
    CloseFuture(AbstractChannel ch)
    {
      super();
    }
    
    public ChannelPromise setSuccess()
    {
      throw new IllegalStateException();
    }
    
    public ChannelPromise setFailure(Throwable cause)
    {
      throw new IllegalStateException();
    }
    
    public boolean trySuccess()
    {
      throw new IllegalStateException();
    }
    
    public boolean tryFailure(Throwable cause)
    {
      throw new IllegalStateException();
    }
    
    boolean setClosed()
    {
      return super.trySuccess();
    }
  }
  
  private final class PausableChannelEventLoop
    extends PausableChannelEventExecutor
    implements EventLoop
  {
    volatile boolean isAcceptingNewTasks = true;
    volatile EventLoop unwrapped;
    
    PausableChannelEventLoop(EventLoop unwrapped)
    {
      this.unwrapped = unwrapped;
    }
    
    public void rejectNewTasks()
    {
      this.isAcceptingNewTasks = false;
    }
    
    public void acceptNewTasks()
    {
      this.isAcceptingNewTasks = true;
    }
    
    public boolean isAcceptingNewTasks()
    {
      return this.isAcceptingNewTasks;
    }
    
    public EventLoopGroup parent()
    {
      return unwrap().parent();
    }
    
    public EventLoop next()
    {
      return unwrap().next();
    }
    
    public EventLoop unwrap()
    {
      return this.unwrapped;
    }
    
    public ChannelHandlerInvoker asInvoker()
    {
      return this;
    }
    
    public ChannelFuture register(Channel channel)
    {
      return unwrap().register(channel);
    }
    
    public ChannelFuture register(Channel channel, ChannelPromise promise)
    {
      return unwrap().register(channel, promise);
    }
    
    Channel channel()
    {
      return AbstractChannel.this;
    }
    
    ChannelHandlerInvoker unwrapInvoker()
    {
      return this.unwrapped.asInvoker();
    }
  }
}
