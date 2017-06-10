package io.netty.resolver.dns;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsQueryHeader;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResource;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsType;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.OneTimeTask;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.ThreadLocalRandom;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

final class DnsQueryContext
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DnsQueryContext.class);
  private final DnsNameResolver parent;
  private final Promise<DnsResponse> promise;
  private final int id;
  private final DnsQuestion question;
  private final DnsResource optResource;
  private final Iterator<InetSocketAddress> nameServerAddresses;
  private final boolean recursionDesired;
  private final int maxTries;
  private int remainingTries;
  private volatile ScheduledFuture<?> timeoutFuture;
  private StringBuilder trace;
  
  DnsQueryContext(DnsNameResolver parent, Iterable<InetSocketAddress> nameServerAddresses, DnsQuestion question, Promise<DnsResponse> promise)
  {
    this.parent = parent;
    this.promise = promise;
    this.question = question;
    
    this.id = allocateId();
    this.recursionDesired = parent.isRecursionDesired();
    this.maxTries = parent.maxTriesPerQuery();
    this.remainingTries = this.maxTries;
    this.optResource = new DnsResource("", DnsType.OPT, parent.maxPayloadSizeClass(), 0L, Unpooled.EMPTY_BUFFER);
    
    this.nameServerAddresses = nameServerAddresses.iterator();
  }
  
  private int allocateId()
  {
    int id = ThreadLocalRandom.current().nextInt(this.parent.promises.length());
    int maxTries = this.parent.promises.length() << 1;
    int tries = 0;
    do
    {
      if (this.parent.promises.compareAndSet(id, null, this)) {
        return id;
      }
      id = id + 1 & 0xFFFF;
      
      tries++;
    } while (tries < maxTries);
    throw new IllegalStateException("query ID space exhausted: " + this.question);
  }
  
  Promise<DnsResponse> promise()
  {
    return this.promise;
  }
  
  DnsQuestion question()
  {
    return this.question;
  }
  
  ScheduledFuture<?> timeoutFuture()
  {
    return this.timeoutFuture;
  }
  
  void query()
  {
    DnsQuestion question = this.question;
    if ((this.remainingTries <= 0) || (!this.nameServerAddresses.hasNext()))
    {
      this.parent.promises.set(this.id, null);
      
      int tries = this.maxTries - this.remainingTries;
      UnknownHostException cause;
      UnknownHostException cause;
      if (tries > 1) {
        cause = new UnknownHostException("failed to resolve " + question + " after " + tries + " attempts:" + this.trace);
      } else {
        cause = new UnknownHostException("failed to resolve " + question + ':' + this.trace);
      }
      cache(question, cause);
      this.promise.tryFailure(cause);
      return;
    }
    this.remainingTries -= 1;
    
    InetSocketAddress nameServerAddr = (InetSocketAddress)this.nameServerAddresses.next();
    DnsQuery query = new DnsQuery(this.id, nameServerAddr);
    query.addQuestion(question);
    query.header().setRecursionDesired(this.recursionDesired);
    query.addAdditionalResource(this.optResource);
    if (logger.isDebugEnabled()) {
      logger.debug("{} WRITE: [{}: {}], {}", new Object[] { this.parent.ch, Integer.valueOf(this.id), nameServerAddr, question });
    }
    sendQuery(query, nameServerAddr);
  }
  
  private void sendQuery(final DnsQuery query, final InetSocketAddress nameServerAddr)
  {
    if (this.parent.bindFuture.isDone()) {
      writeQuery(query, nameServerAddr);
    } else {
      this.parent.bindFuture.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future)
          throws Exception
        {
          if (future.isSuccess()) {
            DnsQueryContext.this.writeQuery(query, nameServerAddr);
          } else {
            DnsQueryContext.this.promise.tryFailure(future.cause());
          }
        }
      });
    }
  }
  
  private void writeQuery(DnsQuery query, final InetSocketAddress nameServerAddr)
  {
    final ChannelFuture writeFuture = this.parent.ch.writeAndFlush(query);
    if (writeFuture.isDone()) {
      onQueryWriteCompletion(writeFuture, nameServerAddr);
    } else {
      writeFuture.addListener(new ChannelFutureListener()
      {
        public void operationComplete(ChannelFuture future)
          throws Exception
        {
          DnsQueryContext.this.onQueryWriteCompletion(writeFuture, nameServerAddr);
        }
      });
    }
  }
  
  private void onQueryWriteCompletion(ChannelFuture writeFuture, final InetSocketAddress nameServerAddr)
  {
    if (!writeFuture.isSuccess())
    {
      retry(nameServerAddr, "failed to send a query: " + writeFuture.cause());
      return;
    }
    final long queryTimeoutMillis = this.parent.queryTimeoutMillis();
    if (queryTimeoutMillis > 0L) {
      this.timeoutFuture = this.parent.ch.eventLoop().schedule(new OneTimeTask()
      {
        public void run()
        {
          if (DnsQueryContext.this.promise.isDone()) {
            return;
          }
          DnsQueryContext.this.retry(nameServerAddr, "query timed out after " + queryTimeoutMillis + " milliseconds");
        }
      }, queryTimeoutMillis, TimeUnit.MILLISECONDS);
    }
  }
  
  void retry(InetSocketAddress nameServerAddr, String message)
  {
    if (this.promise.isCancelled()) {
      return;
    }
    if (this.trace == null) {
      this.trace = new StringBuilder(128);
    }
    this.trace.append(StringUtil.NEWLINE);
    this.trace.append("\tfrom ");
    this.trace.append(nameServerAddr);
    this.trace.append(": ");
    this.trace.append(message);
    query();
  }
  
  private void cache(DnsQuestion question, Throwable cause)
  {
    int negativeTtl = this.parent.negativeTtl();
    if (negativeTtl == 0) {
      return;
    }
    this.parent.cache(question, new DnsNameResolver.DnsCacheEntry(cause), negativeTtl);
  }
}
