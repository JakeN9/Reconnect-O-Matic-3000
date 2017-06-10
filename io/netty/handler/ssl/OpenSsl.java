package io.netty.handler.ssl;

import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLContext;

public final class OpenSsl
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(OpenSsl.class);
  private static final Throwable UNAVAILABILITY_CAUSE;
  private static final Set<String> AVAILABLE_CIPHER_SUITES;
  
  static
  {
    Throwable cause = null;
    try
    {
      Class.forName("org.apache.tomcat.jni.SSL", false, OpenSsl.class.getClassLoader());
    }
    catch (ClassNotFoundException t)
    {
      cause = t;
      logger.debug("netty-tcnative not in the classpath; " + OpenSslEngine.class.getSimpleName() + " will be unavailable.");
    }
    if (cause == null) {
      try
      {
        NativeLibraryLoader.load("netty-tcnative", SSL.class.getClassLoader());
        Library.initialize("provided");
        SSL.initialize(null);
      }
      catch (Throwable t)
      {
        cause = t;
        logger.debug("Failed to load netty-tcnative; " + OpenSslEngine.class.getSimpleName() + " will be unavailable. " + "See http://netty.io/wiki/forked-tomcat-native.html for more information.", t);
      }
    }
    UNAVAILABILITY_CAUSE = cause;
    if (cause == null)
    {
      Set<String> availableCipherSuites = new LinkedHashSet(128);
      long aprPool = Pool.create(0L);
      try
      {
        long sslCtx = SSLContext.make(aprPool, 28, 1);
        try
        {
          SSLContext.setOptions(sslCtx, 4095);
          SSLContext.setCipherSuite(sslCtx, "ALL");
          long ssl = SSL.newSSL(sslCtx, true);
          try
          {
            for (String c : SSL.getCiphers(ssl)) {
              if ((c != null) && (c.length() != 0) && (!availableCipherSuites.contains(c))) {
                availableCipherSuites.add(c);
              }
            }
          }
          finally {}
        }
        finally
        {
          SSLContext.free(sslCtx);
        }
      }
      catch (Exception e)
      {
        logger.warn("Failed to get the list of available OpenSSL cipher suites.", e);
      }
      finally
      {
        Pool.destroy(aprPool);
      }
      AVAILABLE_CIPHER_SUITES = Collections.unmodifiableSet(availableCipherSuites);
    }
    else
    {
      AVAILABLE_CIPHER_SUITES = Collections.emptySet();
    }
  }
  
  public static boolean isAvailable()
  {
    return UNAVAILABILITY_CAUSE == null;
  }
  
  public static void ensureAvailability()
  {
    if (UNAVAILABILITY_CAUSE != null) {
      throw ((Error)new UnsatisfiedLinkError("failed to load the required native library").initCause(UNAVAILABILITY_CAUSE));
    }
  }
  
  public static Throwable unavailabilityCause()
  {
    return UNAVAILABILITY_CAUSE;
  }
  
  public static Set<String> availableCipherSuites()
  {
    return AVAILABLE_CIPHER_SUITES;
  }
  
  public static boolean isCipherSuiteAvailable(String cipherSuite)
  {
    String converted = CipherSuiteConverter.toOpenSsl(cipherSuite);
    if (converted != null) {
      cipherSuite = converted;
    }
    return AVAILABLE_CIPHER_SUITES.contains(cipherSuite);
  }
  
  static boolean isError(long errorCode)
  {
    return errorCode != 0L;
  }
}
