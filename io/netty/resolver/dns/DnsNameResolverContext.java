package io.netty.resolver.dns;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.handler.codec.dns.DnsClass;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResource;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsType;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.StringUtil;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class DnsNameResolverContext
{
  private static final int INADDRSZ4 = 4;
  private static final int INADDRSZ6 = 16;
  private static final FutureListener<DnsResponse> RELEASE_RESPONSE = new FutureListener()
  {
    public void operationComplete(Future<DnsResponse> future)
    {
      if (future.isSuccess()) {
        ((DnsResponse)future.getNow()).release();
      }
    }
  };
  private final DnsNameResolver parent;
  private final Promise<InetSocketAddress> promise;
  private final String hostname;
  private final int port;
  private final int maxAllowedQueries;
  private final InternetProtocolFamily[] resolveAddressTypes;
  private final Set<Future<DnsResponse>> queriesInProgress = Collections.newSetFromMap(new IdentityHashMap());
  private List<InetAddress> resolvedAddresses;
  private StringBuilder trace;
  private int allowedQueries;
  private boolean triedCNAME;
  
  DnsNameResolverContext(DnsNameResolver parent, String hostname, int port, Promise<InetSocketAddress> promise)
  {
    this.parent = parent;
    this.promise = promise;
    this.hostname = hostname;
    this.port = port;
    
    this.maxAllowedQueries = parent.maxQueriesPerResolve();
    this.resolveAddressTypes = parent.resolveAddressTypesUnsafe();
    this.allowedQueries = this.maxAllowedQueries;
  }
  
  void resolve()
  {
    for (InternetProtocolFamily f : this.resolveAddressTypes)
    {
      DnsType type;
      switch (f)
      {
      case IPv4: 
        type = DnsType.A;
        break;
      case IPv6: 
        type = DnsType.AAAA;
        break;
      default: 
        throw new Error();
      }
      query(this.parent.nameServerAddresses, new DnsQuestion(this.hostname, type));
    }
  }
  
  private void query(Iterable<InetSocketAddress> nameServerAddresses, final DnsQuestion question)
  {
    if ((this.allowedQueries == 0) || (this.promise.isCancelled())) {
      return;
    }
    this.allowedQueries -= 1;
    
    Future<DnsResponse> f = this.parent.query(nameServerAddresses, question);
    this.queriesInProgress.add(f);
    
    f.addListener(new FutureListener()
    {
      public void operationComplete(Future<DnsResponse> future)
        throws Exception
      {
        DnsNameResolverContext.this.queriesInProgress.remove(future);
        if (DnsNameResolverContext.this.promise.isDone()) {
          return;
        }
        try
        {
          if (future.isSuccess()) {
            DnsNameResolverContext.this.onResponse(question, (DnsResponse)future.getNow());
          } else {
            DnsNameResolverContext.this.addTrace(future.cause());
          }
        }
        finally
        {
          DnsNameResolverContext.this.tryToFinishResolve();
        }
      }
    });
  }
  
  void onResponse(DnsQuestion question, DnsResponse response)
  {
    DnsType type = question.type();
    try
    {
      if ((type == DnsType.A) || (type == DnsType.AAAA)) {
        onResponseAorAAAA(type, question, response);
      } else if (type == DnsType.CNAME) {
        onResponseCNAME(question, response);
      }
    }
    finally
    {
      ReferenceCountUtil.safeRelease(response);
    }
  }
  
  private void onResponseAorAAAA(DnsType qType, DnsQuestion question, DnsResponse response)
  {
    Map<String, String> cnames = buildAliasMap(response);
    
    boolean found = false;
    for (DnsResource r : response.answers())
    {
      DnsType type = r.type();
      if ((type == DnsType.A) || (type == DnsType.AAAA))
      {
        String qName = question.name().toLowerCase(Locale.US);
        String rName = r.name().toLowerCase(Locale.US);
        if (!rName.equals(qName))
        {
          String resolved = qName;
          do
          {
            resolved = (String)cnames.get(resolved);
          } while ((!rName.equals(resolved)) && 
          
            (resolved != null));
          if (resolved == null) {}
        }
        else
        {
          ByteBuf content = r.content();
          int contentLen = content.readableBytes();
          if ((contentLen == 4) || (contentLen == 16))
          {
            byte[] addrBytes = new byte[contentLen];
            content.getBytes(content.readerIndex(), addrBytes);
            try
            {
              InetAddress resolved = InetAddress.getByAddress(this.hostname, addrBytes);
              if (this.resolvedAddresses == null) {
                this.resolvedAddresses = new ArrayList();
              }
              this.resolvedAddresses.add(resolved);
              found = true;
            }
            catch (UnknownHostException e)
            {
              throw new Error(e);
            }
          }
        }
      }
    }
    if (found) {
      return;
    }
    addTrace(response.sender(), "no matching " + qType + " record found");
    if (!cnames.isEmpty()) {
      onResponseCNAME(question, response, cnames, false);
    }
  }
  
  private void onResponseCNAME(DnsQuestion question, DnsResponse response)
  {
    onResponseCNAME(question, response, buildAliasMap(response), true);
  }
  
  private void onResponseCNAME(DnsQuestion question, DnsResponse response, Map<String, String> cnames, boolean trace)
  {
    String name = question.name().toLowerCase(Locale.US);
    String resolved = name;
    boolean found = false;
    for (;;)
    {
      String next = (String)cnames.get(resolved);
      if (next == null) {
        break;
      }
      found = true;
      resolved = next;
    }
    if (found) {
      followCname(response.sender(), name, resolved);
    } else if (trace) {
      addTrace(response.sender(), "no matching CNAME record found");
    }
  }
  
  private static Map<String, String> buildAliasMap(DnsResponse response)
  {
    Map<String, String> cnames = null;
    for (DnsResource r : response.answers())
    {
      DnsType type = r.type();
      if (type == DnsType.CNAME)
      {
        String content = decodeDomainName(r.content());
        if (content != null)
        {
          if (cnames == null) {
            cnames = new HashMap();
          }
          cnames.put(r.name().toLowerCase(Locale.US), content.toLowerCase(Locale.US));
        }
      }
    }
    return cnames != null ? cnames : Collections.emptyMap();
  }
  
  void tryToFinishResolve()
  {
    if (!this.queriesInProgress.isEmpty())
    {
      if (gotPreferredAddress()) {
        finishResolve();
      }
      return;
    }
    if (this.resolvedAddresses == null) {
      if (!this.triedCNAME)
      {
        this.triedCNAME = true;
        query(this.parent.nameServerAddresses, new DnsQuestion(this.hostname, DnsType.CNAME, DnsClass.IN));
        return;
      }
    }
    finishResolve();
  }
  
  private boolean gotPreferredAddress()
  {
    if (this.resolvedAddresses == null) {
      return false;
    }
    int size = this.resolvedAddresses.size();
    switch (this.resolveAddressTypes[0])
    {
    case IPv4: 
      for (int i = 0; i < size; i++) {
        if ((this.resolvedAddresses.get(i) instanceof Inet4Address)) {
          return true;
        }
      }
      break;
    case IPv6: 
      for (int i = 0; i < size; i++) {
        if ((this.resolvedAddresses.get(i) instanceof Inet6Address)) {
          return true;
        }
      }
    }
    return false;
  }
  
  private void finishResolve()
  {
    Iterator<Future<DnsResponse>> i;
    if (!this.queriesInProgress.isEmpty()) {
      for (i = this.queriesInProgress.iterator(); i.hasNext();)
      {
        Future<DnsResponse> f = (Future)i.next();
        i.remove();
        if (!f.cancel(false)) {
          f.addListener(RELEASE_RESPONSE);
        }
      }
    }
    if (this.resolvedAddresses != null) {
      for (InternetProtocolFamily f : this.resolveAddressTypes) {
        switch (f)
        {
        case IPv4: 
          if (finishResolveWithIPv4()) {
            return;
          }
          break;
        case IPv6: 
          if (finishResolveWithIPv6()) {
            return;
          }
          break;
        }
      }
    }
    int tries = this.maxAllowedQueries - this.allowedQueries;
    UnknownHostException cause;
    UnknownHostException cause;
    if (tries > 1) {
      cause = new UnknownHostException("failed to resolve " + this.hostname + " after " + tries + " queries:" + this.trace);
    } else {
      cause = new UnknownHostException("failed to resolve " + this.hostname + ':' + this.trace);
    }
    this.promise.tryFailure(cause);
  }
  
  private boolean finishResolveWithIPv4()
  {
    List<InetAddress> resolvedAddresses = this.resolvedAddresses;
    int size = resolvedAddresses.size();
    for (int i = 0; i < size; i++)
    {
      InetAddress a = (InetAddress)resolvedAddresses.get(i);
      if ((a instanceof Inet4Address))
      {
        this.promise.trySuccess(new InetSocketAddress(a, this.port));
        return true;
      }
    }
    return false;
  }
  
  private boolean finishResolveWithIPv6()
  {
    List<InetAddress> resolvedAddresses = this.resolvedAddresses;
    int size = resolvedAddresses.size();
    for (int i = 0; i < size; i++)
    {
      InetAddress a = (InetAddress)resolvedAddresses.get(i);
      if ((a instanceof Inet6Address))
      {
        this.promise.trySuccess(new InetSocketAddress(a, this.port));
        return true;
      }
    }
    return false;
  }
  
  static String decodeDomainName(ByteBuf buf)
  {
    buf.markReaderIndex();
    try
    {
      int position = -1;
      int checked = 0;
      int length = buf.writerIndex();
      StringBuilder name = new StringBuilder(64);
      for (int len = buf.readUnsignedByte(); (buf.isReadable()) && (len != 0); len = buf.readUnsignedByte())
      {
        boolean pointer = (len & 0xC0) == 192;
        if (pointer)
        {
          if (position == -1) {
            position = buf.readerIndex() + 1;
          }
          buf.readerIndex((len & 0x3F) << 8 | buf.readUnsignedByte());
          
          checked += 2;
          if (checked >= length) {
            return null;
          }
        }
        else
        {
          name.append(buf.toString(buf.readerIndex(), len, CharsetUtil.UTF_8)).append('.');
          buf.skipBytes(len);
        }
      }
      if (position != -1) {
        buf.readerIndex(position);
      }
      if (name.length() == 0) {
        return null;
      }
      return name.substring(0, name.length() - 1);
    }
    finally
    {
      buf.resetReaderIndex();
    }
  }
  
  private void followCname(InetSocketAddress nameServerAddr, String name, String cname)
  {
    if (this.trace == null) {
      this.trace = new StringBuilder(128);
    }
    this.trace.append(StringUtil.NEWLINE);
    this.trace.append("\tfrom ");
    this.trace.append(nameServerAddr);
    this.trace.append(": ");
    this.trace.append(name);
    this.trace.append(" CNAME ");
    this.trace.append(cname);
    
    query(this.parent.nameServerAddresses, new DnsQuestion(cname, DnsType.A, DnsClass.IN));
    query(this.parent.nameServerAddresses, new DnsQuestion(cname, DnsType.AAAA, DnsClass.IN));
  }
  
  private void addTrace(InetSocketAddress nameServerAddr, String msg)
  {
    if (this.trace == null) {
      this.trace = new StringBuilder(128);
    }
    this.trace.append(StringUtil.NEWLINE);
    this.trace.append("\tfrom ");
    this.trace.append(nameServerAddr);
    this.trace.append(": ");
    this.trace.append(msg);
  }
  
  private void addTrace(Throwable cause)
  {
    if (this.trace == null) {
      this.trace = new StringBuilder(128);
    }
    this.trace.append(StringUtil.NEWLINE);
    this.trace.append("Caused by: ");
    this.trace.append(cause);
  }
}
