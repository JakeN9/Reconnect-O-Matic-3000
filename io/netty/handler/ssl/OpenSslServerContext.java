package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.File;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.tomcat.jni.CertificateVerifier;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLContext;

public final class OpenSslServerContext
  extends OpenSslContext
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(OpenSslServerContext.class);
  private final OpenSslServerSessionContext sessionContext;
  
  public OpenSslServerContext(File certChainFile, File keyFile)
    throws SSLException
  {
    this(certChainFile, keyFile, null);
  }
  
  public OpenSslServerContext(File certChainFile, File keyFile, String keyPassword)
    throws SSLException
  {
    this(certChainFile, keyFile, keyPassword, null, null, OpenSslDefaultApplicationProtocolNegotiator.INSTANCE, 0L, 0L);
  }
  
  public OpenSslServerContext(File certChainFile, File keyFile, String keyPassword, Iterable<String> ciphers, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(certChainFile, keyFile, keyPassword, null, ciphers, toNegotiator(apn, false), sessionCacheSize, sessionTimeout);
  }
  
  public OpenSslServerContext(File certChainFile, File keyFile, String keyPassword, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, ApplicationProtocolConfig config, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    this(certChainFile, keyFile, keyPassword, trustManagerFactory, ciphers, toNegotiator(config, true), sessionCacheSize, sessionTimeout);
  }
  
  public OpenSslServerContext(File certChainFile, File keyFile, String keyPassword, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, OpenSslApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout)
    throws SSLException
  {
    super(ciphers, apn, sessionCacheSize, sessionTimeout, 1);
    OpenSsl.ensureAvailability();
    
    ObjectUtil.checkNotNull(certChainFile, "certChainFile");
    if (!certChainFile.isFile()) {
      throw new IllegalArgumentException("certChainFile is not a file: " + certChainFile);
    }
    ObjectUtil.checkNotNull(keyFile, "keyFile");
    if (!keyFile.isFile()) {
      throw new IllegalArgumentException("keyPath is not a file: " + keyFile);
    }
    if (keyPassword == null) {
      keyPassword = "";
    }
    boolean success = false;
    try
    {
      synchronized (OpenSslContext.class)
      {
        SSLContext.setVerify(this.ctx, 0, 10);
        if (!SSLContext.setCertificateChainFile(this.ctx, certChainFile.getPath(), true))
        {
          long error = SSL.getLastErrorNumber();
          if (OpenSsl.isError(error))
          {
            String err = SSL.getErrorString(error);
            throw new SSLException("failed to set certificate chain: " + certChainFile + " (" + err + ')');
          }
        }
        try
        {
          if (!SSLContext.setCertificate(this.ctx, certChainFile.getPath(), keyFile.getPath(), keyPassword, 0))
          {
            long error = SSL.getLastErrorNumber();
            if (OpenSsl.isError(error))
            {
              String err = SSL.getErrorString(error);
              throw new SSLException("failed to set certificate: " + certChainFile + " and " + keyFile + " (" + err + ')');
            }
          }
        }
        catch (SSLException e)
        {
          throw e;
        }
        catch (Exception e)
        {
          throw new SSLException("failed to set certificate: " + certChainFile + " and " + keyFile, e);
        }
        try
        {
          KeyStore ks = KeyStore.getInstance("JKS");
          ks.load(null, null);
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          KeyFactory rsaKF = KeyFactory.getInstance("RSA");
          KeyFactory dsaKF = KeyFactory.getInstance("DSA");
          
          ByteBuf encodedKeyBuf = PemReader.readPrivateKey(keyFile);
          byte[] encodedKey = new byte[encodedKeyBuf.readableBytes()];
          encodedKeyBuf.readBytes(encodedKey).release();
          
          char[] keyPasswordChars = keyPassword.toCharArray();
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
          if (trustManagerFactory == null)
          {
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            
            trustManagerFactory.init((KeyStore)null);
          }
          else
          {
            trustManagerFactory.init(ks);
          }
          final X509TrustManager manager = chooseTrustManager(trustManagerFactory.getTrustManagers());
          SSLContext.setCertVerifyCallback(this.ctx, new CertificateVerifier()
          {
            public boolean verify(long ssl, byte[][] chain, String auth)
            {
              X509Certificate[] peerCerts = OpenSslContext.certificates(chain);
              try
              {
                manager.checkClientTrusted(peerCerts, auth);
                return true;
              }
              catch (Exception e)
              {
                OpenSslServerContext.logger.debug("verification of certificate failed", e);
              }
              return false;
            }
          });
        }
        catch (Exception e)
        {
          throw new SSLException("unable to setup trustmanager", e);
        }
      }
      this.sessionContext = new OpenSslServerSessionContext(this.ctx);
      success = true;
    }
    finally
    {
      if (!success) {
        destroyPools();
      }
    }
  }
  
  public OpenSslServerSessionContext sessionContext()
  {
    return this.sessionContext;
  }
}
