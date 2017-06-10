package io.netty.channel.group;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelId;
import io.netty.channel.ServerChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultChannelGroup
  extends AbstractSet<Channel>
  implements ChannelGroup
{
  private static final AtomicInteger nextId = new AtomicInteger();
  private final String name;
  private final EventExecutor executor;
  private final ConcurrentMap<ChannelId, Channel> serverChannels = PlatformDependent.newConcurrentHashMap();
  private final ConcurrentMap<ChannelId, Channel> nonServerChannels = PlatformDependent.newConcurrentHashMap();
  private final ChannelFutureListener remover = new ChannelFutureListener()
  {
    public void operationComplete(ChannelFuture future)
      throws Exception
    {
      DefaultChannelGroup.this.remove(future.channel());
    }
  };
  
  public DefaultChannelGroup(EventExecutor executor)
  {
    this("group-0x" + Integer.toHexString(nextId.incrementAndGet()), executor);
  }
  
  public DefaultChannelGroup(String name, EventExecutor executor)
  {
    if (name == null) {
      throw new NullPointerException("name");
    }
    this.name = name;
    this.executor = executor;
  }
  
  public String name()
  {
    return this.name;
  }
  
  public Channel find(ChannelId id)
  {
    Channel c = (Channel)this.nonServerChannels.get(id);
    if (c != null) {
      return c;
    }
    return (Channel)this.serverChannels.get(id);
  }
  
  public boolean isEmpty()
  {
    return (this.nonServerChannels.isEmpty()) && (this.serverChannels.isEmpty());
  }
  
  public int size()
  {
    return this.nonServerChannels.size() + this.serverChannels.size();
  }
  
  public boolean contains(Object o)
  {
    if ((o instanceof Channel))
    {
      Channel c = (Channel)o;
      if ((o instanceof ServerChannel)) {
        return this.serverChannels.containsValue(c);
      }
      return this.nonServerChannels.containsValue(c);
    }
    return false;
  }
  
  public boolean add(Channel channel)
  {
    ConcurrentMap<ChannelId, Channel> map = (channel instanceof ServerChannel) ? this.serverChannels : this.nonServerChannels;
    
    boolean added = map.putIfAbsent(channel.id(), channel) == null;
    if (added) {
      channel.closeFuture().addListener(this.remover);
    }
    return added;
  }
  
  public boolean remove(Object o)
  {
    Channel c = null;
    if ((o instanceof ChannelId))
    {
      c = (Channel)this.nonServerChannels.remove(o);
      if (c == null) {
        c = (Channel)this.serverChannels.remove(o);
      }
    }
    else if ((o instanceof Channel))
    {
      c = (Channel)o;
      if ((c instanceof ServerChannel)) {
        c = (Channel)this.serverChannels.remove(c.id());
      } else {
        c = (Channel)this.nonServerChannels.remove(c.id());
      }
    }
    if (c == null) {
      return false;
    }
    c.closeFuture().removeListener(this.remover);
    return true;
  }
  
  public void clear()
  {
    this.nonServerChannels.clear();
    this.serverChannels.clear();
  }
  
  public Iterator<Channel> iterator()
  {
    return new CombinedIterator(this.serverChannels.values().iterator(), this.nonServerChannels.values().iterator());
  }
  
  public Object[] toArray()
  {
    Collection<Channel> channels = new ArrayList(size());
    channels.addAll(this.serverChannels.values());
    channels.addAll(this.nonServerChannels.values());
    return channels.toArray();
  }
  
  public <T> T[] toArray(T[] a)
  {
    Collection<Channel> channels = new ArrayList(size());
    channels.addAll(this.serverChannels.values());
    channels.addAll(this.nonServerChannels.values());
    return channels.toArray(a);
  }
  
  public ChannelGroupFuture close()
  {
    return close(ChannelMatchers.all());
  }
  
  public ChannelGroupFuture disconnect()
  {
    return disconnect(ChannelMatchers.all());
  }
  
  public ChannelGroupFuture deregister()
  {
    return deregister(ChannelMatchers.all());
  }
  
  public ChannelGroupFuture write(Object message)
  {
    return write(message, ChannelMatchers.all());
  }
  
  private static Object safeDuplicate(Object message)
  {
    if ((message instanceof ByteBuf)) {
      return ((ByteBuf)message).duplicate().retain();
    }
    if ((message instanceof ByteBufHolder)) {
      return ((ByteBufHolder)message).duplicate().retain();
    }
    return ReferenceCountUtil.retain(message);
  }
  
  public ChannelGroupFuture write(Object message, ChannelMatcher matcher)
  {
    if (message == null) {
      throw new NullPointerException("message");
    }
    if (matcher == null) {
      throw new NullPointerException("matcher");
    }
    Map<Channel, ChannelFuture> futures = new LinkedHashMap(size());
    for (Channel c : this.nonServerChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.write(safeDuplicate(message)));
      }
    }
    ReferenceCountUtil.release(message);
    return new DefaultChannelGroupFuture(this, futures, this.executor);
  }
  
  public ChannelGroup flush()
  {
    return flush(ChannelMatchers.all());
  }
  
  public ChannelGroupFuture writeAndFlush(Object message)
  {
    return writeAndFlush(message, ChannelMatchers.all());
  }
  
  public ChannelGroupFuture disconnect(ChannelMatcher matcher)
  {
    if (matcher == null) {
      throw new NullPointerException("matcher");
    }
    Map<Channel, ChannelFuture> futures = new LinkedHashMap(size());
    for (Channel c : this.serverChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.disconnect());
      }
    }
    for (Channel c : this.nonServerChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.disconnect());
      }
    }
    return new DefaultChannelGroupFuture(this, futures, this.executor);
  }
  
  public ChannelGroupFuture close(ChannelMatcher matcher)
  {
    if (matcher == null) {
      throw new NullPointerException("matcher");
    }
    Map<Channel, ChannelFuture> futures = new LinkedHashMap(size());
    for (Channel c : this.serverChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.close());
      }
    }
    for (Channel c : this.nonServerChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.close());
      }
    }
    return new DefaultChannelGroupFuture(this, futures, this.executor);
  }
  
  public ChannelGroupFuture deregister(ChannelMatcher matcher)
  {
    if (matcher == null) {
      throw new NullPointerException("matcher");
    }
    Map<Channel, ChannelFuture> futures = new LinkedHashMap(size());
    for (Channel c : this.serverChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.deregister());
      }
    }
    for (Channel c : this.nonServerChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.deregister());
      }
    }
    return new DefaultChannelGroupFuture(this, futures, this.executor);
  }
  
  public ChannelGroup flush(ChannelMatcher matcher)
  {
    for (Channel c : this.nonServerChannels.values()) {
      if (matcher.matches(c)) {
        c.flush();
      }
    }
    return this;
  }
  
  public ChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher)
  {
    if (message == null) {
      throw new NullPointerException("message");
    }
    Map<Channel, ChannelFuture> futures = new LinkedHashMap(size());
    for (Channel c : this.nonServerChannels.values()) {
      if (matcher.matches(c)) {
        futures.put(c, c.writeAndFlush(safeDuplicate(message)));
      }
    }
    ReferenceCountUtil.release(message);
    
    return new DefaultChannelGroupFuture(this, futures, this.executor);
  }
  
  public int hashCode()
  {
    return System.identityHashCode(this);
  }
  
  public boolean equals(Object o)
  {
    return this == o;
  }
  
  public int compareTo(ChannelGroup o)
  {
    int v = name().compareTo(o.name());
    if (v != 0) {
      return v;
    }
    return System.identityHashCode(this) - System.identityHashCode(o);
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + "(name: " + name() + ", size: " + size() + ')';
  }
}
