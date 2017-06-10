package io.netty.handler.ssl;

import java.io.File;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManagerFactory;

public final class JdkSslServerContext
  extends JdkSslContext
{
  private final SSLContext ctx;
  
  public JdkSslServerContext(File certChainFile, File keyFile)
    throws SSLException
  {
    this(certChainFile, keyFile, null);
  }
  
  public JdkSslServerContext(File certChainFile, File keyFile, String keyPassword)
    throws SSLException
  {
    this(certChainFile, keyFile, keyPassword, null, IdentityCipherSuiteFilter.INSTANCE, JdkDefaultApplicationProtocolNegotiator.INSTANCE, 0L, 0L);
  }
  
  public JdkSslServerContext(File certChainFile, File keyFile, String keyPassword, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(certChainFile, keyFile, keyPassword, ciphers, cipherFilter, toNegotiator(apn, true), sessionCacheSize, sessionTimeout);
  }
  
  public JdkSslServerContext(File certChainFile, File keyFile, String keyPassword, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(null, null, certChainFile, keyFile, keyPassword, null, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
  }
  
  public JdkSslServerContext(File trustCertChainFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(trustCertChainFile, trustManagerFactory, keyCertChainFile, keyFile, keyPassword, keyManagerFactory, ciphers, cipherFilter, toNegotiator(apn, true), sessionCacheSize, sessionTimeout);
  }
  
  public JdkSslServerContext(File trustCertChainFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    super(ciphers, cipherFilter, apn);
    if ((keyFile == null) && (keyManagerFactory == null)) {
      throw new NullPointerException("keyFile, keyManagerFactory");
    }
    try
    {
      if (trustCertChainFile != null) {
        trustManagerFactory = buildTrustManagerFactory(trustCertChainFile, trustManagerFactory);
      }
      if (keyFile != null) {
        keyManagerFactory = buildKeyManagerFactory(keyCertChainFile, keyFile, keyPassword, keyManagerFactory);
      }
      this.ctx = SSLContext.getInstance("TLS");
      this.ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(), null);
      
      SSLSessionContext sessCtx = this.ctx.getServerSessionContext();
      if (sessionCacheSize > 0L) {
        sessCtx.setSessionCacheSize((int)Math.min(sessionCacheSize, 2147483647L));
      }
      if (sessionTimeout > 0L) {
        sessCtx.setSessionTimeout((int)Math.min(sessionTimeout, 2147483647L));
      }
    }
    catch (Exception e)
    {
      throw new SSLException("failed to initialize the server-side SSL context", e);
    }
  }
  
  public boolean isClient()
  {
    return false;
  }
  
  public SSLContext context()
  {
    return this.ctx;
  }
}
