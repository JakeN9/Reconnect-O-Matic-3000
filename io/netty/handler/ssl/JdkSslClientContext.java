package io.netty.handler.ssl;

import java.io.File;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManagerFactory;

public final class JdkSslClientContext
  extends JdkSslContext
{
  private final SSLContext ctx;
  
  public JdkSslClientContext()
    throws SSLException
  {
    this(null, null);
  }
  
  public JdkSslClientContext(File certChainFile)
    throws SSLException
  {
    this(certChainFile, null);
  }
  
  public JdkSslClientContext(TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    this(null, trustManagerFactory);
  }
  
  public JdkSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    this(certChainFile, trustManagerFactory, null, IdentityCipherSuiteFilter.INSTANCE, JdkDefaultApplicationProtocolNegotiator.INSTANCE, 0L, 0L);
  }
  
  public JdkSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(certChainFile, trustManagerFactory, ciphers, cipherFilter, toNegotiator(apn, false), sessionCacheSize, sessionTimeout);
  }
  
  public JdkSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(certChainFile, trustManagerFactory, null, null, null, null, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
  }
  
  public JdkSslClientContext(File trustCertChainFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(trustCertChainFile, trustManagerFactory, keyCertChainFile, keyFile, keyPassword, keyManagerFactory, ciphers, cipherFilter, toNegotiator(apn, false), sessionCacheSize, sessionTimeout);
  }
  
  public JdkSslClientContext(File trustCertChainFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    super(ciphers, cipherFilter, apn);
    try
    {
      if (trustCertChainFile != null) {
        trustManagerFactory = buildTrustManagerFactory(trustCertChainFile, trustManagerFactory);
      }
      if (keyFile != null) {
        keyManagerFactory = buildKeyManagerFactory(keyCertChainFile, keyFile, keyPassword, keyManagerFactory);
      }
      this.ctx = SSLContext.getInstance("TLS");
      this.ctx.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(), trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(), null);
      
      SSLSessionContext sessCtx = this.ctx.getClientSessionContext();
      if (sessionCacheSize > 0L) {
        sessCtx.setSessionCacheSize((int)Math.min(sessionCacheSize, 2147483647L));
      }
      if (sessionTimeout > 0L) {
        sessCtx.setSessionTimeout((int)Math.min(sessionTimeout, 2147483647L));
      }
    }
    catch (Exception e)
    {
      throw new SSLException("failed to initialize the client-side SSL context", e);
    }
  }
  
  public boolean isClient()
  {
    return true;
  }
  
  public SSLContext context()
  {
    return this.ctx;
  }
}
