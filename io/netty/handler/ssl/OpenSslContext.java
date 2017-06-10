package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.SSLContext;

public abstract class OpenSslContext
  extends SslContext
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(OpenSslContext.class);
  private static final List<String> DEFAULT_CIPHERS;
  private static final AtomicIntegerFieldUpdater<OpenSslContext> DESTROY_UPDATER;
  protected static final int VERIFY_DEPTH = 10;
  private final long aprPool;
  private volatile int aprPoolDestroyed;
  private final List<String> ciphers = new ArrayList();
  private final List<String> unmodifiableCiphers = Collections.unmodifiableList(this.ciphers);
  private final long sessionCacheSize;
  private final long sessionTimeout;
  private final OpenSslApplicationProtocolNegotiator apn;
  protected final long ctx;
  private final int mode;
  
  static
  {
    List<String> ciphers = new ArrayList();
    
    Collections.addAll(ciphers, new String[] { "ECDHE-RSA-AES128-GCM-SHA256", "ECDHE-RSA-AES128-SHA", "ECDHE-RSA-AES256-SHA", "AES128-GCM-SHA256", "AES128-SHA", "AES256-SHA", "DES-CBC3-SHA", "RC4-SHA" });
    
    DEFAULT_CIPHERS = Collections.unmodifiableList(ciphers);
    if (logger.isDebugEnabled()) {
      logger.debug("Default cipher suite (OpenSSL): " + ciphers);
    }
    AtomicIntegerFieldUpdater<OpenSslContext> updater = PlatformDependent.newAtomicIntegerFieldUpdater(OpenSslContext.class, "aprPoolDestroyed");
    if (updater == null) {
      updater = AtomicIntegerFieldUpdater.newUpdater(OpenSslContext.class, "aprPoolDestroyed");
    }
    DESTROY_UPDATER = updater;
  }
  
  OpenSslContext(Iterable<String> ciphers, ApplicationProtocolConfig apnCfg, long sessionCacheSize, long sessionTimeout, int mode)
    throws SSLException
  {
    this(ciphers, toNegotiator(apnCfg, mode == 1), sessionCacheSize, sessionTimeout, mode);
  }
  
  OpenSslContext(Iterable<String> ciphers, OpenSslApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout, int mode)
    throws SSLException
  {
    OpenSsl.ensureAvailability();
    if ((mode != 1) && (mode != 0)) {
      throw new IllegalArgumentException("mode most be either SSL.SSL_MODE_SERVER or SSL.SSL_MODE_CLIENT");
    }
    this.mode = mode;
    if (ciphers == null) {
      ciphers = DEFAULT_CIPHERS;
    }
    for (String c : ciphers)
    {
      if (c == null) {
        break;
      }
      String converted = CipherSuiteConverter.toOpenSsl(c);
      if (converted != null) {
        c = converted;
      }
      this.ciphers.add(c);
    }
    this.apn = ((OpenSslApplicationProtocolNegotiator)ObjectUtil.checkNotNull(apn, "apn"));
    
    this.aprPool = Pool.create(0L);
    
    boolean success = false;
    try
    {
      synchronized (OpenSslContext.class)
      {
        try
        {
          this.ctx = SSLContext.make(this.aprPool, 28, mode);
        }
        catch (Exception e)
        {
          throw new SSLException("failed to create an SSL_CTX", e);
        }
        SSLContext.setOptions(this.ctx, 4095);
        SSLContext.setOptions(this.ctx, 16777216);
        SSLContext.setOptions(this.ctx, 33554432);
        SSLContext.setOptions(this.ctx, 4194304);
        SSLContext.setOptions(this.ctx, 524288);
        SSLContext.setOptions(this.ctx, 1048576);
        SSLContext.setOptions(this.ctx, 65536);
        try
        {
          SSLContext.setCipherSuite(this.ctx, CipherSuiteConverter.toOpenSsl(this.ciphers));
        }
        catch (SSLException e)
        {
          throw e;
        }
        catch (Exception e)
        {
          throw new SSLException("failed to set cipher suite: " + this.ciphers, e);
        }
        List<String> nextProtoList = apn.protocols();
        if (!nextProtoList.isEmpty())
        {
          StringBuilder nextProtocolBuf = new StringBuilder();
          for (String p : nextProtoList)
          {
            nextProtocolBuf.append(p);
            nextProtocolBuf.append(',');
          }
          nextProtocolBuf.setLength(nextProtocolBuf.length() - 1);
          
          SSLContext.setNextProtos(this.ctx, nextProtocolBuf.toString());
        }
        if (sessionCacheSize > 0L)
        {
          this.sessionCacheSize = sessionCacheSize;
          SSLContext.setSessionCacheSize(this.ctx, sessionCacheSize);
        }
        else
        {
          this.sessionCacheSize = (sessionCacheSize = SSLContext.setSessionCacheSize(this.ctx, 20480L));
          
          SSLContext.setSessionCacheSize(this.ctx, sessionCacheSize);
        }
        if (sessionTimeout > 0L)
        {
          this.sessionTimeout = sessionTimeout;
          SSLContext.setSessionCacheTimeout(this.ctx, sessionTimeout);
        }
        else
        {
          this.sessionTimeout = (sessionTimeout = SSLContext.setSessionCacheTimeout(this.ctx, 300L));
          
          SSLContext.setSessionCacheTimeout(this.ctx, sessionTimeout);
        }
      }
      success = true;
    }
    finally
    {
      if (!success) {
        destroyPools();
      }
    }
  }
  
  public final List<String> cipherSuites()
  {
    return this.unmodifiableCiphers;
  }
  
  public final long sessionCacheSize()
  {
    return this.sessionCacheSize;
  }
  
  public final long sessionTimeout()
  {
    return this.sessionTimeout;
  }
  
  public ApplicationProtocolNegotiator applicationProtocolNegotiator()
  {
    return this.apn;
  }
  
  public final boolean isClient()
  {
    return this.mode == 0;
  }
  
  public final SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort)
  {
    throw new UnsupportedOperationException();
  }
  
  public final SSLEngine newEngine(ByteBufAllocator alloc)
  {
    List<String> protos = applicationProtocolNegotiator().protocols();
    if (protos.isEmpty()) {
      return new OpenSslEngine(this.ctx, alloc, null, isClient(), sessionContext());
    }
    return new OpenSslEngine(this.ctx, alloc, (String)protos.get(protos.size() - 1), isClient(), sessionContext());
  }
  
  public final long context()
  {
    return this.ctx;
  }
  
  @Deprecated
  public final OpenSslSessionStats stats()
  {
    return sessionContext().stats();
  }
  
  protected final void finalize()
    throws Throwable
  {
    super.finalize();
    synchronized (OpenSslContext.class)
    {
      if (this.ctx != 0L) {
        SSLContext.free(this.ctx);
      }
    }
    destroyPools();
  }
  
  @Deprecated
  public final void setTicketKeys(byte[] keys)
  {
    sessionContext().setTicketKeys(keys);
  }
  
  protected final void destroyPools()
  {
    if ((this.aprPool != 0L) && (DESTROY_UPDATER.compareAndSet(this, 0, 1))) {
      Pool.destroy(this.aprPool);
    }
  }
  
  protected static X509Certificate[] certificates(byte[][] chain)
  {
    X509Certificate[] peerCerts = new X509Certificate[chain.length];
    for (int i = 0; i < peerCerts.length; i++) {
      peerCerts[i] = new OpenSslX509Certificate(chain[i]);
    }
    return peerCerts;
  }
  
  protected static X509TrustManager chooseTrustManager(TrustManager[] managers)
  {
    for (TrustManager m : managers) {
      if ((m instanceof X509TrustManager)) {
        return (X509TrustManager)m;
      }
    }
    throw new IllegalStateException("no X509TrustManager found");
  }
  
  static OpenSslApplicationProtocolNegotiator toNegotiator(ApplicationProtocolConfig config, boolean isServer)
  {
    if (config == null) {
      return OpenSslDefaultApplicationProtocolNegotiator.INSTANCE;
    }
    switch (config.protocol())
    {
    case NONE: 
      return OpenSslDefaultApplicationProtocolNegotiator.INSTANCE;
    case NPN: 
      if (isServer)
      {
        switch (config.selectedListenerFailureBehavior())
        {
        case CHOOSE_MY_LAST_PROTOCOL: 
          return new OpenSslNpnApplicationProtocolNegotiator(config.supportedProtocols());
        }
        throw new UnsupportedOperationException("OpenSSL provider does not support " + config.selectedListenerFailureBehavior() + " behavior");
      }
      throw new UnsupportedOperationException("OpenSSL provider does not support client mode");
    }
    throw new UnsupportedOperationException("OpenSSL provider does not support " + config.protocol() + " protocol");
  }
  
  public abstract OpenSslSessionContext sessionContext();
}
