package io.netty.handler.ssl;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.util.HashSet;
import java.util.List;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.ALPN.ClientProvider;
import org.eclipse.jetty.alpn.ALPN.ServerProvider;

final class JdkAlpnSslEngine
  extends JdkSslEngine
{
  private static boolean available;
  
  static boolean isAvailable()
  {
    updateAvailability();
    return available;
  }
  
  private static void updateAvailability()
  {
    if (available) {
      return;
    }
    try
    {
      ClassLoader bootloader = ClassLoader.getSystemClassLoader().getParent();
      if (bootloader == null) {
        bootloader = ClassLoader.getSystemClassLoader();
      }
      Class.forName("sun.security.ssl.ALPNExtension", true, bootloader);
      available = true;
    }
    catch (Exception ignore) {}
  }
  
  JdkAlpnSslEngine(SSLEngine engine, final JdkApplicationProtocolNegotiator applicationNegotiator, boolean server)
  {
    super(engine);
    ObjectUtil.checkNotNull(applicationNegotiator, "applicationNegotiator");
    if (server)
    {
      final JdkApplicationProtocolNegotiator.ProtocolSelector protocolSelector = (JdkApplicationProtocolNegotiator.ProtocolSelector)ObjectUtil.checkNotNull(applicationNegotiator.protocolSelectorFactory().newSelector(this, new HashSet(applicationNegotiator.protocols())), "protocolSelector");
      
      ALPN.put(engine, new ALPN.ServerProvider()
      {
        public String select(List<String> protocols)
        {
          try
          {
            return protocolSelector.select(protocols);
          }
          catch (Throwable t)
          {
            PlatformDependent.throwException(t);
          }
          return null;
        }
        
        public void unsupported()
        {
          protocolSelector.unsupported();
        }
      });
    }
    else
    {
      final JdkApplicationProtocolNegotiator.ProtocolSelectionListener protocolListener = (JdkApplicationProtocolNegotiator.ProtocolSelectionListener)ObjectUtil.checkNotNull(applicationNegotiator.protocolListenerFactory().newListener(this, applicationNegotiator.protocols()), "protocolListener");
      
      ALPN.put(engine, new ALPN.ClientProvider()
      {
        public List<String> protocols()
        {
          return applicationNegotiator.protocols();
        }
        
        public void selected(String protocol)
        {
          try
          {
            protocolListener.selected(protocol);
          }
          catch (Throwable t)
          {
            PlatformDependent.throwException(t);
          }
        }
        
        public void unsupported()
        {
          protocolListener.unsupported();
        }
      });
    }
  }
  
  public void closeInbound()
    throws SSLException
  {
    ALPN.remove(getWrappedEngine());
    super.closeInbound();
  }
  
  public void closeOutbound()
  {
    ALPN.remove(getWrappedEngine());
    super.closeOutbound();
  }
}
