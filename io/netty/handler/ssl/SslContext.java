package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManagerFactory;

public abstract class SslContext
{
  static final CertificateFactory X509_CERT_FACTORY;
  
  static
  {
    try
    {
      X509_CERT_FACTORY = CertificateFactory.getInstance("X.509");
    }
    catch (CertificateException e)
    {
      throw new IllegalStateException("unable to instance X.509 CertificateFactory", e);
    }
  }
  
  public static SslProvider defaultServerProvider()
  {
    return defaultProvider();
  }
  
  public static SslProvider defaultClientProvider()
  {
    return defaultProvider();
  }
  
  private static SslProvider defaultProvider()
  {
    if (OpenSsl.isAvailable()) {
      return SslProvider.OPENSSL;
    }
    return SslProvider.JDK;
  }
  
  public static SslContext newServerContext(File certChainFile, File keyFile)
    throws SSLException
  {
    return newServerContext(certChainFile, keyFile, null);
  }
  
  public static SslContext newServerContext(File certChainFile, File keyFile, String keyPassword)
    throws SSLException
  {
    return newServerContext(null, certChainFile, keyFile, keyPassword);
  }
  
  public static SslContext newServerContext(File certChainFile, File keyFile, String keyPassword, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    return newServerContext(null, certChainFile, keyFile, keyPassword, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
  }
  
  public static SslContext newServerContext(SslProvider provider, File certChainFile, File keyFile)
    throws SSLException
  {
    return newServerContext(provider, certChainFile, keyFile, null);
  }
  
  public static SslContext newServerContext(SslProvider provider, File certChainFile, File keyFile, String keyPassword)
    throws SSLException
  {
    return newServerContext(provider, certChainFile, keyFile, keyPassword, null, IdentityCipherSuiteFilter.INSTANCE, null, 0L, 0L);
  }
  
  public static SslContext newServerContext(SslProvider provider, File certChainFile, File keyFile, String keyPassword, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    return newServerContext(provider, null, null, certChainFile, keyFile, keyPassword, null, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
  }
  
  public static SslContext newServerContext(SslProvider provider, File trustCertChainFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    if (provider == null) {
      provider = defaultServerProvider();
    }
    switch (provider)
    {
    case JDK: 
      return new JdkSslServerContext(trustCertChainFile, trustManagerFactory, keyCertChainFile, keyFile, keyPassword, keyManagerFactory, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
    case OPENSSL: 
      return new OpenSslServerContext(keyCertChainFile, keyFile, keyPassword, trustManagerFactory, ciphers, apn, sessionCacheSize, sessionTimeout);
    }
    throw new Error(provider.toString());
  }
  
  public static SslContext newClientContext()
    throws SSLException
  {
    return newClientContext(null, null, null);
  }
  
  public static SslContext newClientContext(File certChainFile)
    throws SSLException
  {
    return newClientContext(null, certChainFile);
  }
  
  public static SslContext newClientContext(TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    return newClientContext(null, null, trustManagerFactory);
  }
  
  public static SslContext newClientContext(File certChainFile, TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    return newClientContext(null, certChainFile, trustManagerFactory);
  }
  
  public static SslContext newClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    return newClientContext(null, certChainFile, trustManagerFactory, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
  }
  
  public static SslContext newClientContext(SslProvider provider)
    throws SSLException
  {
    return newClientContext(provider, null, null);
  }
  
  public static SslContext newClientContext(SslProvider provider, File certChainFile)
    throws SSLException
  {
    return newClientContext(provider, certChainFile, null);
  }
  
  public static SslContext newClientContext(SslProvider provider, TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    return newClientContext(provider, null, trustManagerFactory);
  }
  
  public static SslContext newClientContext(SslProvider provider, File certChainFile, TrustManagerFactory trustManagerFactory)
    throws SSLException
  {
    return newClientContext(provider, certChainFile, trustManagerFactory, null, IdentityCipherSuiteFilter.INSTANCE, null, 0L, 0L);
  }
  
  public static SslContext newClientContext(SslProvider provider, File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    return newClientContext(provider, certChainFile, trustManagerFactory, null, null, null, null, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
  }
  
  public static SslContext newClientContext(SslProvider provider, File trustCertChainFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    if (provider == null) {
      provider = defaultClientProvider();
    }
    switch (provider)
    {
    case JDK: 
      return new JdkSslClientContext(trustCertChainFile, trustManagerFactory, keyCertChainFile, keyFile, keyPassword, keyManagerFactory, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout);
    case OPENSSL: 
      return new OpenSslClientContext(trustCertChainFile, trustManagerFactory, ciphers, apn, sessionCacheSize, sessionTimeout);
    }
    throw new Error();
  }
  
  public final boolean isServer()
  {
    return !isClient();
  }
  
  public final SslHandler newHandler(ByteBufAllocator alloc)
  {
    return newHandler(newEngine(alloc));
  }
  
  public final SslHandler newHandler(ByteBufAllocator alloc, String peerHost, int peerPort)
  {
    return newHandler(newEngine(alloc, peerHost, peerPort));
  }
  
  private static SslHandler newHandler(SSLEngine engine)
  {
    return new SslHandler(engine);
  }
  
  protected static PKCS8EncodedKeySpec generateKeySpec(char[] password, byte[] key)
    throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException
  {
    if ((password == null) || (password.length == 0)) {
      return new PKCS8EncodedKeySpec(key);
    }
    EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
    PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
    SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);
    
    Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
    cipher.init(2, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());
    
    return encryptedPrivateKeyInfo.getKeySpec(cipher);
  }
  
  public abstract boolean isClient();
  
  public abstract List<String> cipherSuites();
  
  public abstract long sessionCacheSize();
  
  public abstract long sessionTimeout();
  
  public abstract ApplicationProtocolNegotiator applicationProtocolNegotiator();
  
  public abstract SSLEngine newEngine(ByteBufAllocator paramByteBufAllocator);
  
  public abstract SSLEngine newEngine(ByteBufAllocator paramByteBufAllocator, String paramString, int paramInt);
  
  public abstract SSLSessionContext sessionContext();
}
