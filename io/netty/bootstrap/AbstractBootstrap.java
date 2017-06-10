package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.StringUtil;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel>
  implements Cloneable
{
  private volatile EventLoopGroup group;
  private volatile ChannelFactory<? extends C> channelFactory;
  private volatile SocketAddress localAddress;
  private final Map<ChannelOption<?>, Object> options = new LinkedHashMap();
  private final Map<AttributeKey<?>, Object> attrs = new LinkedHashMap();
  private volatile ChannelHandler handler;
  
  AbstractBootstrap() {}
  
  AbstractBootstrap(AbstractBootstrap<B, C> bootstrap)
  {
    this.group = bootstrap.group;
    this.channelFactory = bootstrap.channelFactory;
    this.handler = bootstrap.handler;
    this.localAddress = bootstrap.localAddress;
    synchronized (bootstrap.options)
    {
      this.options.putAll(bootstrap.options);
    }
    synchronized (bootstrap.attrs)
    {
      this.attrs.putAll(bootstrap.attrs);
    }
  }
  
  public B group(EventLoopGroup group)
  {
    if (group == null) {
      throw new NullPointerException("group");
    }
    if (this.group != null) {
      throw new IllegalStateException("group set already");
    }
    this.group = group;
    return this;
  }
  
  public B channel(Class<? extends C> channelClass)
  {
    if (channelClass == null) {
      throw new NullPointerException("channelClass");
    }
    return channelFactory(new ReflectiveChannelFactory(channelClass));
  }
  
  @Deprecated
  public B channelFactory(ChannelFactory<? extends C> channelFactory)
  {
    if (channelFactory == null) {
      throw new NullPointerException("channelFactory");
    }
    if (this.channelFactory != null) {
      throw new IllegalStateException("channelFactory set already");
    }
    this.channelFactory = channelFactory;
    return this;
  }
  
  public B channelFactory(io.netty.channel.ChannelFactory<? extends C> channelFactory)
  {
    return channelFactory(channelFactory);
  }
  
  public B localAddress(SocketAddress localAddress)
  {
    this.localAddress = localAddress;
    return this;
  }
  
  public B localAddress(int inetPort)
  {
    return localAddress(new InetSocketAddress(inetPort));
  }
  
  public B localAddress(String inetHost, int inetPort)
  {
    return localAddress(new InetSocketAddress(inetHost, inetPort));
  }
  
  public B localAddress(InetAddress inetHost, int inetPort)
  {
    return localAddress(new InetSocketAddress(inetHost, inetPort));
  }
  
  public <T> B option(ChannelOption<T> option, T value)
  {
    if (option == null) {
      throw new NullPointerException("option");
    }
    if (value == null) {
      synchronized (this.options)
      {
        this.options.remove(option);
      }
    } else {
      synchronized (this.options)
      {
        this.options.put(option, value);
      }
    }
    return this;
  }
  
  public <T> B attr(AttributeKey<T> key, T value)
  {
    if (key == null) {
      throw new NullPointerException("key");
    }
    if (value == null) {
      synchronized (this.attrs)
      {
        this.attrs.remove(key);
      }
    } else {
      synchronized (this.attrs)
      {
        this.attrs.put(key, value);
      }
    }
    return this;
  }
  
  public B validate()
  {
    if (this.group == null) {
      throw new IllegalStateException("group not set");
    }
    if (this.channelFactory == null) {
      throw new IllegalStateException("channel or channelFactory not set");
    }
    return this;
  }
  
  public abstract B clone();
  
  public ChannelFuture register()
  {
    validate();
    return initAndRegister();
  }
  
  public ChannelFuture bind()
  {
    validate();
    SocketAddress localAddress = this.localAddress;
    if (localAddress == null) {
      throw new IllegalStateException("localAddress not set");
    }
    return doBind(localAddress);
  }
  
  public ChannelFuture bind(int inetPort)
  {
    return bind(new InetSocketAddress(inetPort));
  }
  
  public ChannelFuture bind(String inetHost, int inetPort)
  {
    return bind(new InetSocketAddress(inetHost, inetPort));
  }
  
  public ChannelFuture bind(InetAddress inetHost, int inetPort)
  {
    return bind(new InetSocketAddress(inetHost, inetPort));
  }
  
  public ChannelFuture bind(SocketAddress localAddress)
  {
    validate();
    if (localAddress == null) {
      throw new NullPointerException("localAddress");
    }
    return doBind(localAddress);
  }
  
  private ChannelFuture doBind(final SocketAddress localAddress)
  {
    final ChannelFuture regFuture = initAndRegister();
    final Channel channel = regFuture.channel();
    if (regFuture.cause() != null) {
      return regFuture;
    }
    if (regFuture.isDone())
    {
      ChannelPromise promise = channel.newPromise();
      doBind0(regFuture, channel, localAddress, promise);
      return promise;
    }
    final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel, null);
    regFuture.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future)
        throws Exception
      {
        Throwable cause = future.cause();
        if (cause != null) {
          promise.setFailure(cause);
        } else {
          promise.executor = channel.eventLoop();
        }
        AbstractBootstrap.doBind0(regFuture, channel, localAddress, promise);
      }
    });
    return promise;
  }
  
  final ChannelFuture initAndRegister()
  {
    Channel channel = channelFactory().newChannel();
    try
    {
      init(channel);
    }
    catch (Throwable t)
    {
      channel.unsafe().closeForcibly();
      
      return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
    }
    ChannelFuture regFuture = group().register(channel);
    if (regFuture.cause() != null) {
      if (channel.isRegistered()) {
        channel.close();
      } else {
        channel.unsafe().closeForcibly();
      }
    }
    return regFuture;
  }
  
  abstract void init(Channel paramChannel)
    throws Exception;
  
  private static void doBind0(ChannelFuture regFuture, final Channel channel, final SocketAddress localAddress, final ChannelPromise promise)
  {
    channel.eventLoop().execute(new Runnable()
    {
      public void run()
      {
        if (this.val$regFuture.isSuccess()) {
          channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        } else {
          promise.setFailure(this.val$regFuture.cause());
        }
      }
    });
  }
  
  public B handler(ChannelHandler handler)
  {
    if (handler == null) {
      throw new NullPointerException("handler");
    }
    this.handler = handler;
    return this;
  }
  
  final SocketAddress localAddress()
  {
    return this.localAddress;
  }
  
  final ChannelFactory<? extends C> channelFactory()
  {
    return this.channelFactory;
  }
  
  final ChannelHandler handler()
  {
    return this.handler;
  }
  
  public EventLoopGroup group()
  {
    return this.group;
  }
  
  final Map<ChannelOption<?>, Object> options()
  {
    return this.options;
  }
  
  final Map<AttributeKey<?>, Object> attrs()
  {
    return this.attrs;
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder().append(StringUtil.simpleClassName(this)).append('(');
    if (this.group != null) {
      buf.append("group: ").append(StringUtil.simpleClassName(this.group)).append(", ");
    }
    if (this.channelFactory != null) {
      buf.append("channelFactory: ").append(this.channelFactory).append(", ");
    }
    if (this.localAddress != null) {
      buf.append("localAddress: ").append(this.localAddress).append(", ");
    }
    synchronized (this.options)
    {
      if (!this.options.isEmpty()) {
        buf.append("options: ").append(this.options).append(", ");
      }
    }
    synchronized (this.attrs)
    {
      if (!this.attrs.isEmpty()) {
        buf.append("attrs: ").append(this.attrs).append(", ");
      }
    }
    if (this.handler != null) {
      buf.append("handler: ").append(this.handler).append(", ");
    }
    if (buf.charAt(buf.length() - 1) == '(')
    {
      buf.append(')');
    }
    else
    {
      buf.setCharAt(buf.length() - 2, ')');
      buf.setLength(buf.length() - 1);
    }
    return buf.toString();
  }
  
  private static final class PendingRegistrationPromise
    extends DefaultChannelPromise
  {
    private volatile EventExecutor executor;
    
    private PendingRegistrationPromise(Channel channel)
    {
      super();
    }
    
    protected EventExecutor executor()
    {
      EventExecutor executor = this.executor;
      if (executor != null) {
        return executor;
      }
      return GlobalEventExecutor.INSTANCE;
    }
  }
}
