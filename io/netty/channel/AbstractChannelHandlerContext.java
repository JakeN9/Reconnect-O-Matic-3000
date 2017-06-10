package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ResourceLeakHint;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.PausableEventExecutor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

abstract class AbstractChannelHandlerContext
  implements ChannelHandlerContext, ResourceLeakHint
{
  static final int MASK_HANDLER_ADDED = 1;
  static final int MASK_HANDLER_REMOVED = 2;
  private static final int MASK_EXCEPTION_CAUGHT = 4;
  private static final int MASK_CHANNEL_REGISTERED = 8;
  private static final int MASK_CHANNEL_UNREGISTERED = 16;
  private static final int MASK_CHANNEL_ACTIVE = 32;
  private static final int MASK_CHANNEL_INACTIVE = 64;
  private static final int MASK_CHANNEL_READ = 128;
  private static final int MASK_CHANNEL_READ_COMPLETE = 256;
  private static final int MASK_CHANNEL_WRITABILITY_CHANGED = 512;
  private static final int MASK_USER_EVENT_TRIGGERED = 1024;
  private static final int MASK_BIND = 2048;
  private static final int MASK_CONNECT = 4096;
  private static final int MASK_DISCONNECT = 8192;
  private static final int MASK_CLOSE = 16384;
  private static final int MASK_DEREGISTER = 32768;
  private static final int MASK_READ = 65536;
  private static final int MASK_WRITE = 131072;
  private static final int MASK_FLUSH = 262144;
  private static final int MASKGROUP_INBOUND = 2044;
  private static final int MASKGROUP_OUTBOUND = 522240;
  private static final FastThreadLocal<WeakHashMap<Class<?>, Integer>> skipFlagsCache = new FastThreadLocal()
  {
    protected WeakHashMap<Class<?>, Integer> initialValue()
      throws Exception
    {
      return new WeakHashMap();
    }
  };
  private static final AtomicReferenceFieldUpdater<AbstractChannelHandlerContext, PausableChannelEventExecutor> WRAPPED_EVENTEXECUTOR_UPDATER;
  volatile AbstractChannelHandlerContext next;
  volatile AbstractChannelHandlerContext prev;
  private final AbstractChannel channel;
  private final DefaultChannelPipeline pipeline;
  private final String name;
  boolean invokedThisChannelRead;
  private volatile boolean invokedNextChannelRead;
  private volatile boolean invokedPrevRead;
  private boolean removed;
  final int skipFlags;
  final ChannelHandlerInvoker invoker;
  private ChannelFuture succeededFuture;
  volatile Runnable invokeChannelReadCompleteTask;
  volatile Runnable invokeReadTask;
  volatile Runnable invokeFlushTask;
  volatile Runnable invokeChannelWritableStateChangedTask;
  private volatile PausableChannelEventExecutor wrappedEventLoop;
  
  static
  {
    AtomicReferenceFieldUpdater<AbstractChannelHandlerContext, PausableChannelEventExecutor> updater = PlatformDependent.newAtomicReferenceFieldUpdater(AbstractChannelHandlerContext.class, "wrappedEventLoop");
    if (updater == null) {
      updater = AtomicReferenceFieldUpdater.newUpdater(AbstractChannelHandlerContext.class, PausableChannelEventExecutor.class, "wrappedEventLoop");
    }
    WRAPPED_EVENTEXECUTOR_UPDATER = updater;
  }
  
  static int skipFlags(ChannelHandler handler)
  {
    WeakHashMap<Class<?>, Integer> cache = (WeakHashMap)skipFlagsCache.get();
    Class<? extends ChannelHandler> handlerType = handler.getClass();
    
    Integer flags = (Integer)cache.get(handlerType);
    int flagsVal;
    int flagsVal;
    if (flags != null)
    {
      flagsVal = flags.intValue();
    }
    else
    {
      flagsVal = skipFlags0(handlerType);
      cache.put(handlerType, Integer.valueOf(flagsVal));
    }
    return flagsVal;
  }
  
  static int skipFlags0(Class<? extends ChannelHandler> handlerType)
  {
    int flags = 0;
    try
    {
      if (isSkippable(handlerType, "handlerAdded", new Class[0])) {
        flags |= 0x1;
      }
      if (isSkippable(handlerType, "handlerRemoved", new Class[0])) {
        flags |= 0x2;
      }
      if (isSkippable(handlerType, "exceptionCaught", new Class[] { Throwable.class })) {
        flags |= 0x4;
      }
      if (isSkippable(handlerType, "channelRegistered", new Class[0])) {
        flags |= 0x8;
      }
      if (isSkippable(handlerType, "channelUnregistered", new Class[0])) {
        flags |= 0x10;
      }
      if (isSkippable(handlerType, "channelActive", new Class[0])) {
        flags |= 0x20;
      }
      if (isSkippable(handlerType, "channelInactive", new Class[0])) {
        flags |= 0x40;
      }
      if (isSkippable(handlerType, "channelRead", new Class[] { Object.class })) {
        flags |= 0x80;
      }
      if (isSkippable(handlerType, "channelReadComplete", new Class[0])) {
        flags |= 0x100;
      }
      if (isSkippable(handlerType, "channelWritabilityChanged", new Class[0])) {
        flags |= 0x200;
      }
      if (isSkippable(handlerType, "userEventTriggered", new Class[] { Object.class })) {
        flags |= 0x400;
      }
      if (isSkippable(handlerType, "bind", new Class[] { SocketAddress.class, ChannelPromise.class })) {
        flags |= 0x800;
      }
      if (isSkippable(handlerType, "connect", new Class[] { SocketAddress.class, SocketAddress.class, ChannelPromise.class })) {
        flags |= 0x1000;
      }
      if (isSkippable(handlerType, "disconnect", new Class[] { ChannelPromise.class })) {
        flags |= 0x2000;
      }
      if (isSkippable(handlerType, "close", new Class[] { ChannelPromise.class })) {
        flags |= 0x4000;
      }
      if (isSkippable(handlerType, "deregister", new Class[] { ChannelPromise.class })) {
        flags |= 0x8000;
      }
      if (isSkippable(handlerType, "read", new Class[0])) {
        flags |= 0x10000;
      }
      if (isSkippable(handlerType, "write", new Class[] { Object.class, ChannelPromise.class })) {
        flags |= 0x20000;
      }
      if (isSkippable(handlerType, "flush", new Class[0])) {
        flags |= 0x40000;
      }
    }
    catch (Exception e)
    {
      PlatformDependent.throwException(e);
    }
    return flags;
  }
  
  private static boolean isSkippable(Class<?> handlerType, String methodName, Class<?>... paramTypes)
    throws Exception
  {
    Class[] newParamTypes = new Class[paramTypes.length + 1];
    newParamTypes[0] = ChannelHandlerContext.class;
    System.arraycopy(paramTypes, 0, newParamTypes, 1, paramTypes.length);
    
    return handlerType.getMethod(methodName, newParamTypes).isAnnotationPresent(ChannelHandler.Skip.class);
  }
  
  AbstractChannelHandlerContext(DefaultChannelPipeline pipeline, ChannelHandlerInvoker invoker, String name, int skipFlags)
  {
    if (name == null) {
      throw new NullPointerException("name");
    }
    this.channel = pipeline.channel;
    this.pipeline = pipeline;
    this.name = name;
    this.invoker = invoker;
    this.skipFlags = skipFlags;
  }
  
  public final Channel channel()
  {
    return this.channel;
  }
  
  public ChannelPipeline pipeline()
  {
    return this.pipeline;
  }
  
  public ByteBufAllocator alloc()
  {
    return channel().config().getAllocator();
  }
  
  public final EventExecutor executor()
  {
    if (this.invoker == null) {
      return channel().eventLoop();
    }
    return wrappedEventLoop();
  }
  
  public final ChannelHandlerInvoker invoker()
  {
    if (this.invoker == null) {
      return channel().eventLoop().asInvoker();
    }
    return wrappedEventLoop();
  }
  
  private PausableChannelEventExecutor wrappedEventLoop()
  {
    PausableChannelEventExecutor wrapped = this.wrappedEventLoop;
    if (wrapped == null)
    {
      wrapped = new PausableChannelEventExecutor0(null);
      if (!WRAPPED_EVENTEXECUTOR_UPDATER.compareAndSet(this, null, wrapped)) {
        return this.wrappedEventLoop;
      }
    }
    return wrapped;
  }
  
  public String name()
  {
    return this.name;
  }
  
  public <T> Attribute<T> attr(AttributeKey<T> key)
  {
    return this.channel.attr(key);
  }
  
  public <T> boolean hasAttr(AttributeKey<T> key)
  {
    return this.channel.hasAttr(key);
  }
  
  public ChannelHandlerContext fireChannelRegistered()
  {
    AbstractChannelHandlerContext next = findContextInbound();
    next.invoker().invokeChannelRegistered(next);
    return this;
  }
  
  public ChannelHandlerContext fireChannelUnregistered()
  {
    AbstractChannelHandlerContext next = findContextInbound();
    next.invoker().invokeChannelUnregistered(next);
    return this;
  }
  
  public ChannelHandlerContext fireChannelActive()
  {
    AbstractChannelHandlerContext next = findContextInbound();
    next.invoker().invokeChannelActive(next);
    return this;
  }
  
  public ChannelHandlerContext fireChannelInactive()
  {
    AbstractChannelHandlerContext next = findContextInbound();
    next.invoker().invokeChannelInactive(next);
    return this;
  }
  
  public ChannelHandlerContext fireExceptionCaught(Throwable cause)
  {
    AbstractChannelHandlerContext next = findContextInbound();
    next.invoker().invokeExceptionCaught(next, cause);
    return this;
  }
  
  public ChannelHandlerContext fireUserEventTriggered(Object event)
  {
    AbstractChannelHandlerContext next = findContextInbound();
    next.invoker().invokeUserEventTriggered(next, event);
    return this;
  }
  
  public ChannelHandlerContext fireChannelRead(Object msg)
  {
    AbstractChannelHandlerContext next = findContextInbound();
    ReferenceCountUtil.touch(msg, next);
    this.invokedNextChannelRead = true;
    next.invoker().invokeChannelRead(next, msg);
    return this;
  }
  
  public ChannelHandlerContext fireChannelReadComplete()
  {
    if ((this.invokedNextChannelRead) || (!this.invokedThisChannelRead))
    {
      this.invokedNextChannelRead = false;
      this.invokedPrevRead = false;
      
      AbstractChannelHandlerContext next = findContextInbound();
      next.invoker().invokeChannelReadComplete(next);
      return this;
    }
    if ((this.invokedPrevRead) && (!channel().config().isAutoRead())) {
      read();
    } else {
      this.invokedPrevRead = false;
    }
    return this;
  }
  
  public ChannelHandlerContext fireChannelWritabilityChanged()
  {
    AbstractChannelHandlerContext next = findContextInbound();
    next.invoker().invokeChannelWritabilityChanged(next);
    return this;
  }
  
  public ChannelFuture bind(SocketAddress localAddress)
  {
    return bind(localAddress, newPromise());
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress)
  {
    return connect(remoteAddress, newPromise());
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress)
  {
    return connect(remoteAddress, localAddress, newPromise());
  }
  
  public ChannelFuture disconnect()
  {
    return disconnect(newPromise());
  }
  
  public ChannelFuture close()
  {
    return close(newPromise());
  }
  
  public ChannelFuture deregister()
  {
    return deregister(newPromise());
  }
  
  public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise)
  {
    AbstractChannelHandlerContext next = findContextOutbound();
    next.invoker().invokeBind(next, localAddress, promise);
    return promise;
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise)
  {
    return connect(remoteAddress, null, promise);
  }
  
  public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
  {
    AbstractChannelHandlerContext next = findContextOutbound();
    next.invoker().invokeConnect(next, remoteAddress, localAddress, promise);
    return promise;
  }
  
  public ChannelFuture disconnect(ChannelPromise promise)
  {
    if (!channel().metadata().hasDisconnect()) {
      return close(promise);
    }
    AbstractChannelHandlerContext next = findContextOutbound();
    next.invoker().invokeDisconnect(next, promise);
    return promise;
  }
  
  public ChannelFuture close(ChannelPromise promise)
  {
    AbstractChannelHandlerContext next = findContextOutbound();
    next.invoker().invokeClose(next, promise);
    return promise;
  }
  
  public ChannelFuture deregister(ChannelPromise promise)
  {
    AbstractChannelHandlerContext next = findContextOutbound();
    next.invoker().invokeDeregister(next, promise);
    return promise;
  }
  
  public ChannelHandlerContext read()
  {
    AbstractChannelHandlerContext next = findContextOutbound();
    this.invokedPrevRead = true;
    next.invoker().invokeRead(next);
    return this;
  }
  
  public ChannelFuture write(Object msg)
  {
    return write(msg, newPromise());
  }
  
  public ChannelFuture write(Object msg, ChannelPromise promise)
  {
    AbstractChannelHandlerContext next = findContextOutbound();
    ReferenceCountUtil.touch(msg, next);
    next.invoker().invokeWrite(next, msg, promise);
    return promise;
  }
  
  public ChannelHandlerContext flush()
  {
    AbstractChannelHandlerContext next = findContextOutbound();
    next.invoker().invokeFlush(next);
    return this;
  }
  
  public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise)
  {
    AbstractChannelHandlerContext next = findContextOutbound();
    ReferenceCountUtil.touch(msg, next);
    next.invoker().invokeWrite(next, msg, promise);
    next = findContextOutbound();
    next.invoker().invokeFlush(next);
    return promise;
  }
  
  public ChannelFuture writeAndFlush(Object msg)
  {
    return writeAndFlush(msg, newPromise());
  }
  
  public ChannelPromise newPromise()
  {
    return new DefaultChannelPromise(channel(), executor());
  }
  
  public ChannelProgressivePromise newProgressivePromise()
  {
    return new DefaultChannelProgressivePromise(channel(), executor());
  }
  
  public ChannelFuture newSucceededFuture()
  {
    ChannelFuture succeededFuture = this.succeededFuture;
    if (succeededFuture == null) {
      this.succeededFuture = (succeededFuture = new SucceededChannelFuture(channel(), executor()));
    }
    return succeededFuture;
  }
  
  public ChannelFuture newFailedFuture(Throwable cause)
  {
    return new FailedChannelFuture(channel(), executor(), cause);
  }
  
  private AbstractChannelHandlerContext findContextInbound()
  {
    AbstractChannelHandlerContext ctx = this;
    do
    {
      ctx = ctx.next;
    } while ((ctx.skipFlags & 0x7FC) == 2044);
    return ctx;
  }
  
  private AbstractChannelHandlerContext findContextOutbound()
  {
    AbstractChannelHandlerContext ctx = this;
    do
    {
      ctx = ctx.prev;
    } while ((ctx.skipFlags & 0x7F800) == 522240);
    return ctx;
  }
  
  public ChannelPromise voidPromise()
  {
    return this.channel.voidPromise();
  }
  
  void setRemoved()
  {
    this.removed = true;
  }
  
  public boolean isRemoved()
  {
    return this.removed;
  }
  
  public String toHintString()
  {
    return '\'' + this.name + "' will handle the message from this point.";
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(ChannelHandlerContext.class) + '(' + this.name + ", " + this.channel + ')';
  }
  
  private final class PausableChannelEventExecutor0
    extends PausableChannelEventExecutor
  {
    private PausableChannelEventExecutor0() {}
    
    public void rejectNewTasks()
    {
      ((PausableEventExecutor)channel().eventLoop()).rejectNewTasks();
    }
    
    public void acceptNewTasks()
    {
      ((PausableEventExecutor)channel().eventLoop()).acceptNewTasks();
    }
    
    public boolean isAcceptingNewTasks()
    {
      return ((PausableEventExecutor)channel().eventLoop()).isAcceptingNewTasks();
    }
    
    public Channel channel()
    {
      return AbstractChannelHandlerContext.this.channel();
    }
    
    public EventExecutor unwrap()
    {
      return unwrapInvoker().executor();
    }
    
    public ChannelHandlerInvoker unwrapInvoker()
    {
      return AbstractChannelHandlerContext.this.invoker;
    }
  }
}
