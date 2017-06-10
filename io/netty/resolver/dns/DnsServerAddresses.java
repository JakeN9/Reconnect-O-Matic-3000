package io.netty.resolver.dns;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ThreadLocalRandom;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public final class DnsServerAddresses
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DnsServerAddresses.class);
  private static final List<InetSocketAddress> DEFAULT_NAME_SERVER_LIST;
  private static final InetSocketAddress[] DEFAULT_NAME_SERVER_ARRAY;
  
  static
  {
    int DNS_PORT = 53;
    List<InetSocketAddress> defaultNameServers = new ArrayList(2);
    try
    {
      Class<?> configClass = Class.forName("sun.net.dns.ResolverConfiguration");
      Method open = configClass.getMethod("open", new Class[0]);
      Method nameservers = configClass.getMethod("nameservers", new Class[0]);
      Object instance = open.invoke(null, new Object[0]);
      
      List<String> list = (List)nameservers.invoke(instance, new Object[0]);
      int size = list.size();
      for (int i = 0; i < size; i++)
      {
        String dnsAddr = (String)list.get(i);
        if (dnsAddr != null) {
          defaultNameServers.add(new InetSocketAddress(InetAddress.getByName(dnsAddr), 53));
        }
      }
    }
    catch (Exception ignore) {}
    if (!defaultNameServers.isEmpty())
    {
      if (logger.isDebugEnabled()) {
        logger.debug("Default DNS servers: {} (sun.net.dns.ResolverConfiguration)", defaultNameServers);
      }
    }
    else
    {
      Collections.addAll(defaultNameServers, new InetSocketAddress[] { new InetSocketAddress("8.8.8.8", 53), new InetSocketAddress("8.8.4.4", 53) });
      if (logger.isWarnEnabled()) {
        logger.warn("Default DNS servers: {} (Google Public DNS as a fallback)", defaultNameServers);
      }
    }
    DEFAULT_NAME_SERVER_LIST = Collections.unmodifiableList(defaultNameServers);
    DEFAULT_NAME_SERVER_ARRAY = (InetSocketAddress[])defaultNameServers.toArray(new InetSocketAddress[defaultNameServers.size()]);
  }
  
  public static List<InetSocketAddress> defaultAddresses()
  {
    return DEFAULT_NAME_SERVER_LIST;
  }
  
  public static Iterable<InetSocketAddress> sequential(Iterable<? extends InetSocketAddress> addresses)
  {
    return sequential0(sanitize(addresses));
  }
  
  public static Iterable<InetSocketAddress> sequential(InetSocketAddress... addresses)
  {
    return sequential0(sanitize(addresses));
  }
  
  private static Iterable<InetSocketAddress> sequential0(InetSocketAddress[] addresses)
  {
    new Iterable()
    {
      public Iterator<InetSocketAddress> iterator()
      {
        return new DnsServerAddresses.SequentialAddressIterator(this.val$addresses, 0);
      }
    };
  }
  
  public static Iterable<InetSocketAddress> shuffled(Iterable<? extends InetSocketAddress> addresses)
  {
    return shuffled0(sanitize(addresses));
  }
  
  public static Iterable<InetSocketAddress> shuffled(InetSocketAddress... addresses)
  {
    return shuffled0(sanitize(addresses));
  }
  
  private static Iterable<InetSocketAddress> shuffled0(InetSocketAddress[] addresses)
  {
    if (addresses.length == 1) {
      return singleton(addresses[0]);
    }
    new Iterable()
    {
      public Iterator<InetSocketAddress> iterator()
      {
        return new DnsServerAddresses.ShuffledAddressIterator(this.val$addresses);
      }
    };
  }
  
  public static Iterable<InetSocketAddress> rotational(Iterable<? extends InetSocketAddress> addresses)
  {
    return rotational0(sanitize(addresses));
  }
  
  public static Iterable<InetSocketAddress> rotational(InetSocketAddress... addresses)
  {
    return rotational0(sanitize(addresses));
  }
  
  private static Iterable<InetSocketAddress> rotational0(InetSocketAddress[] addresses)
  {
    return new RotationalAddresses(addresses);
  }
  
  public static Iterable<InetSocketAddress> singleton(InetSocketAddress address)
  {
    if (address == null) {
      throw new NullPointerException("address");
    }
    if (address.isUnresolved()) {
      throw new IllegalArgumentException("cannot use an unresolved DNS server address: " + address);
    }
    new Iterable()
    {
      private final Iterator<InetSocketAddress> iterator = new Iterator()
      {
        public boolean hasNext()
        {
          return true;
        }
        
        public InetSocketAddress next()
        {
          return DnsServerAddresses.3.this.val$address;
        }
        
        public void remove()
        {
          throw new UnsupportedOperationException();
        }
      };
      
      public Iterator<InetSocketAddress> iterator()
      {
        return this.iterator;
      }
    };
  }
  
  private static InetSocketAddress[] sanitize(Iterable<? extends InetSocketAddress> addresses)
  {
    if (addresses == null) {
      throw new NullPointerException("addresses");
    }
    List<InetSocketAddress> list;
    List<InetSocketAddress> list;
    if ((addresses instanceof Collection)) {
      list = new ArrayList(((Collection)addresses).size());
    } else {
      list = new ArrayList(4);
    }
    for (InetSocketAddress a : addresses)
    {
      if (a == null) {
        break;
      }
      if (a.isUnresolved()) {
        throw new IllegalArgumentException("cannot use an unresolved DNS server address: " + a);
      }
      list.add(a);
    }
    if (list.isEmpty()) {
      return DEFAULT_NAME_SERVER_ARRAY;
    }
    return (InetSocketAddress[])list.toArray(new InetSocketAddress[list.size()]);
  }
  
  private static InetSocketAddress[] sanitize(InetSocketAddress[] addresses)
  {
    if (addresses == null) {
      throw new NullPointerException("addresses");
    }
    List<InetSocketAddress> list = new ArrayList(addresses.length);
    for (InetSocketAddress a : addresses)
    {
      if (a == null) {
        break;
      }
      if (a.isUnresolved()) {
        throw new IllegalArgumentException("cannot use an unresolved DNS server address: " + a);
      }
      list.add(a);
    }
    if (list.isEmpty()) {
      return DEFAULT_NAME_SERVER_ARRAY;
    }
    return (InetSocketAddress[])list.toArray(new InetSocketAddress[list.size()]);
  }
  
  private static final class SequentialAddressIterator
    implements Iterator<InetSocketAddress>
  {
    private final InetSocketAddress[] addresses;
    private int i;
    
    SequentialAddressIterator(InetSocketAddress[] addresses, int startIdx)
    {
      this.addresses = addresses;
      this.i = startIdx;
    }
    
    public boolean hasNext()
    {
      return true;
    }
    
    public InetSocketAddress next()
    {
      int i = this.i;
      InetSocketAddress next = this.addresses[i];
      i++;
      if (i < this.addresses.length) {
        this.i = i;
      } else {
        this.i = 0;
      }
      return next;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  private static final class ShuffledAddressIterator
    implements Iterator<InetSocketAddress>
  {
    private final InetSocketAddress[] addresses;
    private int i;
    
    ShuffledAddressIterator(InetSocketAddress[] addresses)
    {
      this.addresses = ((InetSocketAddress[])addresses.clone());
      
      shuffle();
    }
    
    private void shuffle()
    {
      InetSocketAddress[] addresses = this.addresses;
      Random r = ThreadLocalRandom.current();
      for (int i = addresses.length - 1; i >= 0; i--)
      {
        InetSocketAddress tmp = addresses[i];
        int j = r.nextInt(i + 1);
        addresses[i] = addresses[j];
        addresses[j] = tmp;
      }
    }
    
    public boolean hasNext()
    {
      return true;
    }
    
    public InetSocketAddress next()
    {
      int i = this.i;
      InetSocketAddress next = this.addresses[i];
      i++;
      if (i < this.addresses.length)
      {
        this.i = i;
      }
      else
      {
        this.i = 0;
        shuffle();
      }
      return next;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  private static final class RotationalAddresses
    implements Iterable<InetSocketAddress>
  {
    private static final AtomicIntegerFieldUpdater<RotationalAddresses> startIdxUpdater;
    private final InetSocketAddress[] addresses;
    private volatile int startIdx;
    
    static
    {
      AtomicIntegerFieldUpdater<RotationalAddresses> updater = PlatformDependent.newAtomicIntegerFieldUpdater(RotationalAddresses.class, "startIdx");
      if (updater == null) {
        updater = AtomicIntegerFieldUpdater.newUpdater(RotationalAddresses.class, "startIdx");
      }
      startIdxUpdater = updater;
    }
    
    RotationalAddresses(InetSocketAddress[] addresses)
    {
      this.addresses = addresses;
    }
    
    public Iterator<InetSocketAddress> iterator()
    {
      for (;;)
      {
        int curStartIdx = this.startIdx;
        int nextStartIdx = curStartIdx + 1;
        if (nextStartIdx >= this.addresses.length) {
          nextStartIdx = 0;
        }
        if (startIdxUpdater.compareAndSet(this, curStartIdx, nextStartIdx)) {
          return new DnsServerAddresses.SequentialAddressIterator(this.addresses, curStartIdx);
        }
      }
    }
  }
}
