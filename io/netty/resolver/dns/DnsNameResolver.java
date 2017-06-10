package io.netty.resolver.dns;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.handler.codec.dns.DnsClass;
import io.netty.handler.codec.dns.DnsQueryEncoder;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResource;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.DnsResponseDecoder;
import io.netty.handler.codec.dns.DnsResponseHeader;
import io.netty.resolver.SimpleNameResolver;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.OneTimeTask;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.IDN;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class DnsNameResolver
  extends SimpleNameResolver<InetSocketAddress>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DnsNameResolver.class);
  static final InetSocketAddress ANY_LOCAL_ADDR = new InetSocketAddress(0);
  private static final InternetProtocolFamily[] DEFAULT_RESOLVE_ADDRESS_TYPES = new InternetProtocolFamily[2];
  
  static
  {
    if ("true".equalsIgnoreCase(SystemPropertyUtil.get("java.net.preferIPv6Addresses")))
    {
      DEFAULT_RESOLVE_ADDRESS_TYPES[0] = InternetProtocolFamily.IPv6;
      DEFAULT_RESOLVE_ADDRESS_TYPES[1] = InternetProtocolFamily.IPv4;
      logger.debug("-Djava.net.preferIPv6Addresses: true");
    }
    else
    {
      DEFAULT_RESOLVE_ADDRESS_TYPES[0] = InternetProtocolFamily.IPv4;
      DEFAULT_RESOLVE_ADDRESS_TYPES[1] = InternetProtocolFamily.IPv6;
      logger.debug("-Djava.net.preferIPv6Addresses: false");
    }
  }
  
  private static final DnsResponseDecoder DECODER = new DnsResponseDecoder();
  private static final DnsQueryEncoder ENCODER = new DnsQueryEncoder();
  final Iterable<InetSocketAddress> nameServerAddresses;
  final ChannelFuture bindFuture;
  final DatagramChannel ch;
  final AtomicReferenceArray<DnsQueryContext> promises = new AtomicReferenceArray(65536);
  final ConcurrentMap<DnsQuestion, DnsCacheEntry> queryCache = PlatformDependent.newConcurrentHashMap();
  private final DnsResponseHandler responseHandler = new DnsResponseHandler(null);
  private volatile long queryTimeoutMillis = 5000L;
  private volatile int minTtl;
  private volatile int maxTtl = Integer.MAX_VALUE;
  private volatile int negativeTtl;
  private volatile int maxTriesPerQuery = 2;
  private volatile InternetProtocolFamily[] resolveAddressTypes = DEFAULT_RESOLVE_ADDRESS_TYPES;
  private volatile boolean recursionDesired = true;
  private volatile int maxQueriesPerResolve = 8;
  private volatile int maxPayloadSize;
  private volatile DnsClass maxPayloadSizeClass;
  
  public DnsNameResolver(EventLoop eventLoop, Class<? extends DatagramChannel> channelType, InetSocketAddress nameServerAddress)
  {
    this(eventLoop, channelType, ANY_LOCAL_ADDR, nameServerAddress);
  }
  
  public DnsNameResolver(EventLoop eventLoop, Class<? extends DatagramChannel> channelType, InetSocketAddress localAddress, InetSocketAddress nameServerAddress)
  {
    this(eventLoop, new ReflectiveChannelFactory(channelType), localAddress, nameServerAddress);
  }
  
  public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress nameServerAddress)
  {
    this(eventLoop, channelFactory, ANY_LOCAL_ADDR, nameServerAddress);
  }
  
  public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress localAddress, InetSocketAddress nameServerAddress)
  {
    this(eventLoop, channelFactory, localAddress, DnsServerAddresses.singleton(nameServerAddress));
  }
  
  public DnsNameResolver(EventLoop eventLoop, Class<? extends DatagramChannel> channelType, Iterable<InetSocketAddress> nameServerAddresses)
  {
    this(eventLoop, channelType, ANY_LOCAL_ADDR, nameServerAddresses);
  }
  
  public DnsNameResolver(EventLoop eventLoop, Class<? extends DatagramChannel> channelType, InetSocketAddress localAddress, Iterable<InetSocketAddress> nameServerAddresses)
  {
    this(eventLoop, new ReflectiveChannelFactory(channelType), localAddress, nameServerAddresses);
  }
  
  public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, Iterable<InetSocketAddress> nameServerAddresses)
  {
    this(eventLoop, channelFactory, ANY_LOCAL_ADDR, nameServerAddresses);
  }
  
  public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress localAddress, Iterable<InetSocketAddress> nameServerAddresses)
  {
    super(eventLoop);
    if (channelFactory == null) {
      throw new NullPointerException("channelFactory");
    }
    if (nameServerAddresses == null) {
      throw new NullPointerException("nameServerAddresses");
    }
    if (!nameServerAddresses.iterator().hasNext()) {
      throw new NullPointerException("nameServerAddresses is empty");
    }
    if (localAddress == null) {
      throw new NullPointerException("localAddress");
    }
    this.nameServerAddresses = nameServerAddresses;
    this.bindFuture = newChannel(channelFactory, localAddress);
    this.ch = ((DatagramChannel)this.bindFuture.channel());
    
    setMaxPayloadSize(4096);
  }
  
  private ChannelFuture newChannel(ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress localAddress)
  {
    Bootstrap b = new Bootstrap();
    b.group(executor());
    b.channelFactory(channelFactory);
    b.handler(new ChannelInitializer()
    {
      protected void initChannel(DatagramChannel ch)
        throws Exception
      {
        ch.pipeline().addLast(new ChannelHandler[] { DnsNameResolver.DECODER, DnsNameResolver.ENCODER, DnsNameResolver.this.responseHandler });
      }
    });
    ChannelFuture bindFuture = b.bind(localAddress);
    bindFuture.channel().closeFuture().addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future)
        throws Exception
      {
        DnsNameResolver.this.clearCache();
      }
    });
    return bindFuture;
  }
  
  public int minTtl()
  {
    return this.minTtl;
  }
  
  public int maxTtl()
  {
    return this.maxTtl;
  }
  
  public DnsNameResolver setTtl(int minTtl, int maxTtl)
  {
    if (minTtl < 0) {
      throw new IllegalArgumentException("minTtl: " + minTtl + " (expected: >= 0)");
    }
    if (maxTtl < 0) {
      throw new IllegalArgumentException("maxTtl: " + maxTtl + " (expected: >= 0)");
    }
    if (minTtl > maxTtl) {
      throw new IllegalArgumentException("minTtl: " + minTtl + ", maxTtl: " + maxTtl + " (expected: 0 <= minTtl <= maxTtl)");
    }
    this.maxTtl = maxTtl;
    this.minTtl = minTtl;
    
    return this;
  }
  
  public int negativeTtl()
  {
    return this.negativeTtl;
  }
  
  public DnsNameResolver setNegativeTtl(int negativeTtl)
  {
    if (negativeTtl < 0) {
      throw new IllegalArgumentException("negativeTtl: " + negativeTtl + " (expected: >= 0)");
    }
    this.negativeTtl = negativeTtl;
    
    return this;
  }
  
  public long queryTimeoutMillis()
  {
    return this.queryTimeoutMillis;
  }
  
  public DnsNameResolver setQueryTimeoutMillis(long queryTimeoutMillis)
  {
    if (queryTimeoutMillis < 0L) {
      throw new IllegalArgumentException("queryTimeoutMillis: " + queryTimeoutMillis + " (expected: >= 0)");
    }
    this.queryTimeoutMillis = queryTimeoutMillis;
    
    return this;
  }
  
  public int maxTriesPerQuery()
  {
    return this.maxTriesPerQuery;
  }
  
  public DnsNameResolver setMaxTriesPerQuery(int maxTriesPerQuery)
  {
    if (maxTriesPerQuery < 1) {
      throw new IllegalArgumentException("maxTries: " + maxTriesPerQuery + " (expected: > 0)");
    }
    this.maxTriesPerQuery = maxTriesPerQuery;
    
    return this;
  }
  
  public List<InternetProtocolFamily> resolveAddressTypes()
  {
    return Arrays.asList(this.resolveAddressTypes);
  }
  
  InternetProtocolFamily[] resolveAddressTypesUnsafe()
  {
    return this.resolveAddressTypes;
  }
  
  public DnsNameResolver setResolveAddressTypes(InternetProtocolFamily... resolveAddressTypes)
  {
    if (resolveAddressTypes == null) {
      throw new NullPointerException("resolveAddressTypes");
    }
    List<InternetProtocolFamily> list = new ArrayList(InternetProtocolFamily.values().length);
    for (InternetProtocolFamily f : resolveAddressTypes)
    {
      if (f == null) {
        break;
      }
      if (!list.contains(f)) {
        list.add(f);
      }
    }
    if (list.isEmpty()) {
      throw new IllegalArgumentException("no protocol family specified");
    }
    this.resolveAddressTypes = ((InternetProtocolFamily[])list.toArray(new InternetProtocolFamily[list.size()]));
    
    return this;
  }
  
  public DnsNameResolver setResolveAddressTypes(Iterable<InternetProtocolFamily> resolveAddressTypes)
  {
    if (resolveAddressTypes == null) {
      throw new NullPointerException("resolveAddressTypes");
    }
    List<InternetProtocolFamily> list = new ArrayList(InternetProtocolFamily.values().length);
    for (InternetProtocolFamily f : resolveAddressTypes)
    {
      if (f == null) {
        break;
      }
      if (!list.contains(f)) {
        list.add(f);
      }
    }
    if (list.isEmpty()) {
      throw new IllegalArgumentException("no protocol family specified");
    }
    this.resolveAddressTypes = ((InternetProtocolFamily[])list.toArray(new InternetProtocolFamily[list.size()]));
    
    return this;
  }
  
  public boolean isRecursionDesired()
  {
    return this.recursionDesired;
  }
  
  public DnsNameResolver setRecursionDesired(boolean recursionDesired)
  {
    this.recursionDesired = recursionDesired;
    return this;
  }
  
  public int maxQueriesPerResolve()
  {
    return this.maxQueriesPerResolve;
  }
  
  public DnsNameResolver setMaxQueriesPerResolve(int maxQueriesPerResolve)
  {
    if (maxQueriesPerResolve <= 0) {
      throw new IllegalArgumentException("maxQueriesPerResolve: " + maxQueriesPerResolve + " (expected: > 0)");
    }
    this.maxQueriesPerResolve = maxQueriesPerResolve;
    
    return this;
  }
  
  public int maxPayloadSize()
  {
    return this.maxPayloadSize;
  }
  
  public DnsNameResolver setMaxPayloadSize(int maxPayloadSize)
  {
    if (maxPayloadSize <= 0) {
      throw new IllegalArgumentException("maxPayloadSize: " + maxPayloadSize + " (expected: > 0)");
    }
    if (this.maxPayloadSize == maxPayloadSize) {
      return this;
    }
    this.maxPayloadSize = maxPayloadSize;
    this.maxPayloadSizeClass = DnsClass.valueOf(maxPayloadSize);
    this.ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(maxPayloadSize));
    
    return this;
  }
  
  DnsClass maxPayloadSizeClass()
  {
    return this.maxPayloadSizeClass;
  }
  
  public DnsNameResolver clearCache()
  {
    for (Iterator<Map.Entry<DnsQuestion, DnsCacheEntry>> i = this.queryCache.entrySet().iterator(); i.hasNext();)
    {
      Map.Entry<DnsQuestion, DnsCacheEntry> e = (Map.Entry)i.next();
      i.remove();
      ((DnsCacheEntry)e.getValue()).release();
    }
    return this;
  }
  
  public boolean clearCache(DnsQuestion question)
  {
    DnsCacheEntry e = (DnsCacheEntry)this.queryCache.remove(question);
    if (e != null)
    {
      e.release();
      return true;
    }
    return false;
  }
  
  public void close()
  {
    this.ch.close();
  }
  
  protected EventLoop executor()
  {
    return (EventLoop)super.executor();
  }
  
  protected boolean doIsResolved(InetSocketAddress address)
  {
    return !address.isUnresolved();
  }
  
  protected void doResolve(InetSocketAddress unresolvedAddress, Promise<InetSocketAddress> promise)
    throws Exception
  {
    String hostname = IDN.toASCII(hostname(unresolvedAddress));
    int port = unresolvedAddress.getPort();
    
    DnsNameResolverContext ctx = new DnsNameResolverContext(this, hostname, port, promise);
    
    ctx.resolve();
  }
  
  private static String hostname(InetSocketAddress addr)
  {
    if (PlatformDependent.javaVersion() < 7) {
      return addr.getHostName();
    }
    return addr.getHostString();
  }
  
  public Future<DnsResponse> query(DnsQuestion question)
  {
    return query(this.nameServerAddresses, question);
  }
  
  public Future<DnsResponse> query(DnsQuestion question, Promise<DnsResponse> promise)
  {
    return query(this.nameServerAddresses, question, promise);
  }
  
  public Future<DnsResponse> query(Iterable<InetSocketAddress> nameServerAddresses, DnsQuestion question)
  {
    if (nameServerAddresses == null) {
      throw new NullPointerException("nameServerAddresses");
    }
    if (question == null) {
      throw new NullPointerException("question");
    }
    EventLoop eventLoop = this.ch.eventLoop();
    DnsCacheEntry cachedResult = (DnsCacheEntry)this.queryCache.get(question);
    if (cachedResult != null)
    {
      if (cachedResult.response != null) {
        return eventLoop.newSucceededFuture(cachedResult.response.retain());
      }
      return eventLoop.newFailedFuture(cachedResult.cause);
    }
    return query0(nameServerAddresses, question, eventLoop.newPromise());
  }
  
  public Future<DnsResponse> query(Iterable<InetSocketAddress> nameServerAddresses, DnsQuestion question, Promise<DnsResponse> promise)
  {
    if (nameServerAddresses == null) {
      throw new NullPointerException("nameServerAddresses");
    }
    if (question == null) {
      throw new NullPointerException("question");
    }
    if (promise == null) {
      throw new NullPointerException("promise");
    }
    DnsCacheEntry cachedResult = (DnsCacheEntry)this.queryCache.get(question);
    if (cachedResult != null)
    {
      if (cachedResult.response != null) {
        return promise.setSuccess(cachedResult.response.retain());
      }
      return promise.setFailure(cachedResult.cause);
    }
    return query0(nameServerAddresses, question, promise);
  }
  
  private Future<DnsResponse> query0(Iterable<InetSocketAddress> nameServerAddresses, DnsQuestion question, Promise<DnsResponse> promise)
  {
    try
    {
      new DnsQueryContext(this, nameServerAddresses, question, promise).query();
      return promise;
    }
    catch (Exception e)
    {
      return promise.setFailure(e);
    }
  }
  
  void cache(final DnsQuestion question, DnsCacheEntry entry, long delaySeconds)
  {
    DnsCacheEntry oldEntry = (DnsCacheEntry)this.queryCache.put(question, entry);
    if (oldEntry != null) {
      oldEntry.release();
    }
    boolean scheduled = false;
    try
    {
      entry.expirationFuture = this.ch.eventLoop().schedule(new OneTimeTask()
      {
        public void run()
        {
          DnsNameResolver.this.clearCache(question);
        }
      }, delaySeconds, TimeUnit.SECONDS);
      
      scheduled = true;
    }
    finally
    {
      if (!scheduled)
      {
        clearCache(question);
        entry.release();
      }
    }
  }
  
  private final class DnsResponseHandler
    extends ChannelHandlerAdapter
  {
    private DnsResponseHandler() {}
    
    public void channelRead(ChannelHandlerContext ctx, Object msg)
      throws Exception
    {
      try
      {
        DnsResponse res = (DnsResponse)msg;
        int queryId = res.header().id();
        if (DnsNameResolver.logger.isDebugEnabled()) {
          DnsNameResolver.logger.debug("{} RECEIVED: [{}: {}], {}", new Object[] { DnsNameResolver.this.ch, Integer.valueOf(queryId), res.sender(), res });
        }
        DnsQueryContext qCtx = (DnsQueryContext)DnsNameResolver.this.promises.get(queryId);
        if (qCtx == null)
        {
          if (DnsNameResolver.logger.isWarnEnabled()) {
            DnsNameResolver.logger.warn("Received a DNS response with an unknown ID: {}", Integer.valueOf(queryId));
          }
        }
        else
        {
          List<DnsQuestion> questions = res.questions();
          if (questions.size() != 1)
          {
            DnsNameResolver.logger.warn("Received a DNS response with invalid number of questions: {}", res);
          }
          else
          {
            DnsQuestion q = qCtx.question();
            if (!q.equals(questions.get(0)))
            {
              DnsNameResolver.logger.warn("Received a mismatching DNS response: {}", res);
            }
            else
            {
              ScheduledFuture<?> timeoutFuture = qCtx.timeoutFuture();
              if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
              }
              if (res.header().responseCode() == DnsResponseCode.NOERROR)
              {
                cache(q, res);
                DnsNameResolver.this.promises.set(queryId, null);
                
                Promise<DnsResponse> qPromise = qCtx.promise();
                if (qPromise.setUncancellable()) {
                  qPromise.setSuccess(res.retain());
                }
              }
              else
              {
                qCtx.retry(res.sender(), "response code: " + res.header().responseCode() + " with " + res.answers().size() + " answer(s) and " + res.authorityResources().size() + " authority resource(s)");
              }
            }
          }
        }
      }
      finally
      {
        ReferenceCountUtil.safeRelease(msg);
      }
    }
    
    private void cache(DnsQuestion question, DnsResponse res)
    {
      int maxTtl = DnsNameResolver.this.maxTtl();
      if (maxTtl == 0) {
        return;
      }
      long ttl = Long.MAX_VALUE;
      for (DnsResource r : res.answers())
      {
        long rTtl = r.timeToLive();
        if (ttl > rTtl) {
          ttl = rTtl;
        }
      }
      ttl = Math.max(DnsNameResolver.this.minTtl(), Math.min(maxTtl, ttl));
      
      DnsNameResolver.this.cache(question, new DnsNameResolver.DnsCacheEntry(res), ttl);
    }
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception
    {
      DnsNameResolver.logger.warn("Unexpected exception: ", cause);
    }
  }
  
  static final class DnsCacheEntry
  {
    final DnsResponse response;
    final Throwable cause;
    volatile ScheduledFuture<?> expirationFuture;
    
    DnsCacheEntry(DnsResponse response)
    {
      this.response = response.retain();
      this.cause = null;
    }
    
    DnsCacheEntry(Throwable cause)
    {
      this.cause = cause;
      this.response = null;
    }
    
    void release()
    {
      DnsResponse response = this.response;
      if (response != null) {
        ReferenceCountUtil.safeRelease(response);
      }
      ScheduledFuture<?> expirationFuture = this.expirationFuture;
      if (expirationFuture != null) {
        expirationFuture.cancel(false);
      }
    }
  }
}
