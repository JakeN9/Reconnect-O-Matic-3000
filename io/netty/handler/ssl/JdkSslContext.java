package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

public abstract class JdkSslContext
  extends SslContext
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(JdkSslContext.class);
  static final String PROTOCOL = "TLS";
  static final String[] PROTOCOLS;
  static final List<String> DEFAULT_CIPHERS;
  static final Set<String> SUPPORTED_CIPHERS;
  private final String[] cipherSuites;
  private final List<String> unmodifiableCipherSuites;
  private final JdkApplicationProtocolNegotiator apn;
  
  static
  {
    SSLContext context;
    try
    {
      context = SSLContext.getInstance("TLS");
      context.init(null, null, null);
    }
    catch (Exception e)
    {
      throw new Error("failed to initialize the default SSL context", e);
    }
    SSLEngine engine = context.createSSLEngine();
    
    String[] supportedProtocols = engine.getSupportedProtocols();
    Set<String> supportedProtocolsSet = new HashSet(supportedProtocols.length);
    for (int i = 0; i < supportedProtocols.length; i++) {
      supportedProtocolsSet.add(supportedProtocols[i]);
    }
    List<String> protocols = new ArrayList();
    addIfSupported(supportedProtocolsSet, protocols, new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" });
    if (!protocols.isEmpty()) {
      PROTOCOLS = (String[])protocols.toArray(new String[protocols.size()]);
    } else {
      PROTOCOLS = engine.getEnabledProtocols();
    }
    String[] supportedCiphers = engine.getSupportedCipherSuites();
    SUPPORTED_CIPHERS = new HashSet(supportedCiphers.length);
    for (i = 0; i < supportedCiphers.length; i++) {
      SUPPORTED_CIPHERS.add(supportedCiphers[i]);
    }
    List<String> ciphers = new ArrayList();
    addIfSupported(SUPPORTED_CIPHERS, ciphers, new String[] { "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_RC4_128_SHA" });
    if (!ciphers.isEmpty()) {
      DEFAULT_CIPHERS = Collections.unmodifiableList(ciphers);
    } else {
      DEFAULT_CIPHERS = Collections.unmodifiableList(Arrays.asList(engine.getEnabledCipherSuites()));
    }
    if (logger.isDebugEnabled())
    {
      logger.debug("Default protocols (JDK): {} ", Arrays.asList(PROTOCOLS));
      logger.debug("Default cipher suites (JDK): {}", DEFAULT_CIPHERS);
    }
  }
  
  private static void addIfSupported(Set<String> supported, List<String> enabled, String... names)
  {
    for (String n : names) {
      if (supported.contains(n)) {
        enabled.add(n);
      }
    }
  }
  
  JdkSslContext(Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig config, boolean isServer)
  {
    this(ciphers, cipherFilter, toNegotiator(config, isServer));
  }
  
  JdkSslContext(Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn)
  {
    this.apn = ((JdkApplicationProtocolNegotiator)ObjectUtil.checkNotNull(apn, "apn"));
    this.cipherSuites = ((CipherSuiteFilter)ObjectUtil.checkNotNull(cipherFilter, "cipherFilter")).filterCipherSuites(ciphers, DEFAULT_CIPHERS, SUPPORTED_CIPHERS);
    
    this.unmodifiableCipherSuites = Collections.unmodifiableList(Arrays.asList(this.cipherSuites));
  }
  
  public final SSLSessionContext sessionContext()
  {
    if (isServer()) {
      return context().getServerSessionContext();
    }
    return context().getClientSessionContext();
  }
  
  public final List<String> cipherSuites()
  {
    return this.unmodifiableCipherSuites;
  }
  
  public final long sessionCacheSize()
  {
    return sessionContext().getSessionCacheSize();
  }
  
  public final long sessionTimeout()
  {
    return sessionContext().getSessionTimeout();
  }
  
  public final SSLEngine newEngine(ByteBufAllocator alloc)
  {
    SSLEngine engine = context().createSSLEngine();
    engine.setEnabledCipherSuites(this.cipherSuites);
    engine.setEnabledProtocols(PROTOCOLS);
    engine.setUseClientMode(isClient());
    return wrapEngine(engine);
  }
  
  public final SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort)
  {
    SSLEngine engine = context().createSSLEngine(peerHost, peerPort);
    engine.setEnabledCipherSuites(this.cipherSuites);
    engine.setEnabledProtocols(PROTOCOLS);
    engine.setUseClientMode(isClient());
    return wrapEngine(engine);
  }
  
  private SSLEngine wrapEngine(SSLEngine engine)
  {
    return this.apn.wrapperFactory().wrapSslEngine(engine, this.apn, isServer());
  }
  
  public JdkApplicationProtocolNegotiator applicationProtocolNegotiator()
  {
    return this.apn;
  }
  
  static JdkApplicationProtocolNegotiator toNegotiator(ApplicationProtocolConfig config, boolean isServer)
  {
    if (config == null) {
      return JdkDefaultApplicationProtocolNegotiator.INSTANCE;
    }
    switch (config.protocol())
    {
    case NONE: 
      return JdkDefaultApplicationProtocolNegotiator.INSTANCE;
    case ALPN: 
      if (isServer)
      {
        switch (config.selectorFailureBehavior())
        {
        case FATAL_ALERT: 
          return new JdkAlpnApplicationProtocolNegotiator(true, config.supportedProtocols());
        case NO_ADVERTISE: 
          return new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
        }
        throw new UnsupportedOperationException("JDK provider does not support " + config.selectorFailureBehavior() + " failure behavior");
      }
      switch (config.selectedListenerFailureBehavior())
      {
      case ACCEPT: 
        return new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
      case FATAL_ALERT: 
        return new JdkAlpnApplicationProtocolNegotiator(true, config.supportedProtocols());
      }
      throw new UnsupportedOperationException("JDK provider does not support " + config.selectedListenerFailureBehavior() + " failure behavior");
    case NPN: 
      if (isServer)
      {
        switch (config.selectedListenerFailureBehavior())
        {
        case ACCEPT: 
          return new JdkNpnApplicationProtocolNegotiator(false, config.supportedProtocols());
        case FATAL_ALERT: 
          return new JdkNpnApplicationProtocolNegotiator(true, config.supportedProtocols());
        }
        throw new UnsupportedOperationException("JDK provider does not support " + config.selectedListenerFailureBehavior() + " failure behavior");
      }
      switch (config.selectorFailureBehavior())
      {
      case FATAL_ALERT: 
        return new JdkNpnApplicationProtocolNegotiator(true, config.supportedProtocols());
      case NO_ADVERTISE: 
        return new JdkNpnApplicationProtocolNegotiator(false, config.supportedProtocols());
      }
      throw new UnsupportedOperationException("JDK provider does not support " + config.selectorFailureBehavior() + " failure behavior");
    }
    throw new UnsupportedOperationException("JDK provider does not support " + config.protocol() + " protocol");
  }
  
  protected static KeyManagerFactory buildKeyManagerFactory(File certChainFile, File keyFile, String keyPassword, KeyManagerFactory kmf)
    throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, CertificateException, KeyException, IOException
  {
    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "SunX509";
    }
    return buildKeyManagerFactory(certChainFile, algorithm, keyFile, keyPassword, kmf);
  }
  
  protected static KeyManagerFactory buildKeyManagerFactory(File certChainFile, String keyAlgorithm, File keyFile, String keyPassword, KeyManagerFactory kmf)
    throws KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, IOException, CertificateException, KeyException, UnrecoverableKeyException
  {
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, null);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    KeyFactory rsaKF = KeyFactory.getInstance("RSA");
    KeyFactory dsaKF = KeyFactory.getInstance("DSA");
    
    ByteBuf encodedKeyBuf = PemReader.readPrivateKey(keyFile);
    byte[] encodedKey = new byte[encodedKeyBuf.readableBytes()];
    encodedKeyBuf.readBytes(encodedKey).release();
    
    char[] keyPasswordChars = keyPassword == null ? EmptyArrays.EMPTY_CHARS : keyPassword.toCharArray();
    PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(keyPasswordChars, encodedKey);
    PrivateKey key;
    try
    {
      key = rsaKF.generatePrivate(encodedKeySpec);
    }
    catch (InvalidKeySpecException ignore)
    {
      key = dsaKF.generatePrivate(encodedKeySpec);
    }
    List<Certificate> certChain = new ArrayList();
    ByteBuf[] certs = PemReader.readCertificates(certChainFile);
    try
    {
      for (ByteBuf buf : certs) {
        certChain.add(cf.generateCertificate(new ByteBufInputStream(buf)));
      }
    }
    finally
    {
      ByteBuf[] arr$;
      int len$;
      int i$;
      ByteBuf buf;
      for (ByteBuf buf : certs) {
        buf.release();
      }
    }
    ks.setKeyEntry("key", key, keyPasswordChars, (Certificate[])certChain.toArray(new Certificate[certChain.size()]));
    if (kmf == null) {
      kmf = KeyManagerFactory.getInstance(keyAlgorithm);
    }
    kmf.init(ks, keyPasswordChars);
    
    return kmf;
  }
  
  protected static TrustManagerFactory buildTrustManagerFactory(File certChainFile, TrustManagerFactory trustManagerFactory)
    throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
  {
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, null);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    
    ByteBuf[] certs = PemReader.readCertificates(certChainFile);
    try
    {
      for (ByteBuf buf : certs)
      {
        X509Certificate cert = (X509Certificate)cf.generateCertificate(new ByteBufInputStream(buf));
        X500Principal principal = cert.getSubjectX500Principal();
        ks.setCertificateEntry(principal.getName("RFC2253"), cert);
      }
    }
    finally
    {
      ByteBuf[] arr$;
      int len$;
      int i$;
      ByteBuf buf;
      for (ByteBuf buf : certs) {
        buf.release();
      }
    }
    if (trustManagerFactory == null) {
      trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    }
    trustManagerFactory.init(ks);
    
    return trustManagerFactory;
  }
  
  public abstract SSLContext context();
}
