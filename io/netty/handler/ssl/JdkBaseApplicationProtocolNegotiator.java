package io.netty.handler.ssl;

import io.netty.util.internal.ObjectUtil;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;

class JdkBaseApplicationProtocolNegotiator
  implements JdkApplicationProtocolNegotiator
{
  private final List<String> protocols;
  private final JdkApplicationProtocolNegotiator.ProtocolSelectorFactory selectorFactory;
  private final JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory listenerFactory;
  private final JdkApplicationProtocolNegotiator.SslEngineWrapperFactory wrapperFactory;
  
  protected JdkBaseApplicationProtocolNegotiator(JdkApplicationProtocolNegotiator.SslEngineWrapperFactory wrapperFactory, JdkApplicationProtocolNegotiator.ProtocolSelectorFactory selectorFactory, JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory listenerFactory, Iterable<String> protocols)
  {
    this(wrapperFactory, selectorFactory, listenerFactory, ApplicationProtocolUtil.toList(protocols));
  }
  
  protected JdkBaseApplicationProtocolNegotiator(JdkApplicationProtocolNegotiator.SslEngineWrapperFactory wrapperFactory, JdkApplicationProtocolNegotiator.ProtocolSelectorFactory selectorFactory, JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory listenerFactory, String... protocols)
  {
    this(wrapperFactory, selectorFactory, listenerFactory, ApplicationProtocolUtil.toList(protocols));
  }
  
  private JdkBaseApplicationProtocolNegotiator(JdkApplicationProtocolNegotiator.SslEngineWrapperFactory wrapperFactory, JdkApplicationProtocolNegotiator.ProtocolSelectorFactory selectorFactory, JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory listenerFactory, List<String> protocols)
  {
    this.wrapperFactory = ((JdkApplicationProtocolNegotiator.SslEngineWrapperFactory)ObjectUtil.checkNotNull(wrapperFactory, "wrapperFactory"));
    this.selectorFactory = ((JdkApplicationProtocolNegotiator.ProtocolSelectorFactory)ObjectUtil.checkNotNull(selectorFactory, "selectorFactory"));
    this.listenerFactory = ((JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory)ObjectUtil.checkNotNull(listenerFactory, "listenerFactory"));
    this.protocols = Collections.unmodifiableList((List)ObjectUtil.checkNotNull(protocols, "protocols"));
  }
  
  public List<String> protocols()
  {
    return this.protocols;
  }
  
  public JdkApplicationProtocolNegotiator.ProtocolSelectorFactory protocolSelectorFactory()
  {
    return this.selectorFactory;
  }
  
  public JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory protocolListenerFactory()
  {
    return this.listenerFactory;
  }
  
  public JdkApplicationProtocolNegotiator.SslEngineWrapperFactory wrapperFactory()
  {
    return this.wrapperFactory;
  }
  
  static final JdkApplicationProtocolNegotiator.ProtocolSelectorFactory FAIL_SELECTOR_FACTORY = new JdkApplicationProtocolNegotiator.ProtocolSelectorFactory()
  {
    public JdkApplicationProtocolNegotiator.ProtocolSelector newSelector(SSLEngine engine, Set<String> supportedProtocols)
    {
      return new JdkBaseApplicationProtocolNegotiator.FailProtocolSelector((JdkSslEngine)engine, supportedProtocols);
    }
  };
  static final JdkApplicationProtocolNegotiator.ProtocolSelectorFactory NO_FAIL_SELECTOR_FACTORY = new JdkApplicationProtocolNegotiator.ProtocolSelectorFactory()
  {
    public JdkApplicationProtocolNegotiator.ProtocolSelector newSelector(SSLEngine engine, Set<String> supportedProtocols)
    {
      return new JdkBaseApplicationProtocolNegotiator.NoFailProtocolSelector((JdkSslEngine)engine, supportedProtocols);
    }
  };
  static final JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory FAIL_SELECTION_LISTENER_FACTORY = new JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory()
  {
    public JdkApplicationProtocolNegotiator.ProtocolSelectionListener newListener(SSLEngine engine, List<String> supportedProtocols)
    {
      return new JdkBaseApplicationProtocolNegotiator.FailProtocolSelectionListener((JdkSslEngine)engine, supportedProtocols);
    }
  };
  static final JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory NO_FAIL_SELECTION_LISTENER_FACTORY = new JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory()
  {
    public JdkApplicationProtocolNegotiator.ProtocolSelectionListener newListener(SSLEngine engine, List<String> supportedProtocols)
    {
      return new JdkBaseApplicationProtocolNegotiator.NoFailProtocolSelectionListener((JdkSslEngine)engine, supportedProtocols);
    }
  };
  
  protected static class NoFailProtocolSelector
    implements JdkApplicationProtocolNegotiator.ProtocolSelector
  {
    private final JdkSslEngine jettyWrapper;
    private final Set<String> supportedProtocols;
    
    public NoFailProtocolSelector(JdkSslEngine jettyWrapper, Set<String> supportedProtocols)
    {
      this.jettyWrapper = jettyWrapper;
      this.supportedProtocols = supportedProtocols;
    }
    
    public void unsupported()
    {
      this.jettyWrapper.getSession().setApplicationProtocol(null);
    }
    
    public String select(List<String> protocols)
      throws Exception
    {
      for (int i = 0; i < protocols.size(); i++)
      {
        String p = (String)protocols.get(i);
        if (this.supportedProtocols.contains(p))
        {
          this.jettyWrapper.getSession().setApplicationProtocol(p);
          return p;
        }
      }
      return noSelectMatchFound();
    }
    
    public String noSelectMatchFound()
      throws Exception
    {
      this.jettyWrapper.getSession().setApplicationProtocol(null);
      return null;
    }
  }
  
  protected static final class FailProtocolSelector
    extends JdkBaseApplicationProtocolNegotiator.NoFailProtocolSelector
  {
    public FailProtocolSelector(JdkSslEngine jettyWrapper, Set<String> supportedProtocols)
    {
      super(supportedProtocols);
    }
    
    public String noSelectMatchFound()
      throws Exception
    {
      throw new SSLHandshakeException("Selected protocol is not supported");
    }
  }
  
  protected static class NoFailProtocolSelectionListener
    implements JdkApplicationProtocolNegotiator.ProtocolSelectionListener
  {
    private final JdkSslEngine jettyWrapper;
    private final List<String> supportedProtocols;
    
    public NoFailProtocolSelectionListener(JdkSslEngine jettyWrapper, List<String> supportedProtocols)
    {
      this.jettyWrapper = jettyWrapper;
      this.supportedProtocols = supportedProtocols;
    }
    
    public void unsupported()
    {
      this.jettyWrapper.getSession().setApplicationProtocol(null);
    }
    
    public void selected(String protocol)
      throws Exception
    {
      if (this.supportedProtocols.contains(protocol)) {
        this.jettyWrapper.getSession().setApplicationProtocol(protocol);
      } else {
        noSelectedMatchFound(protocol);
      }
    }
    
    public void noSelectedMatchFound(String protocol)
      throws Exception
    {}
  }
  
  protected static final class FailProtocolSelectionListener
    extends JdkBaseApplicationProtocolNegotiator.NoFailProtocolSelectionListener
  {
    public FailProtocolSelectionListener(JdkSslEngine jettyWrapper, List<String> supportedProtocols)
    {
      super(supportedProtocols);
    }
    
    public void noSelectedMatchFound(String protocol)
      throws Exception
    {
      throw new SSLHandshakeException("No compatible protocols found");
    }
  }
}
