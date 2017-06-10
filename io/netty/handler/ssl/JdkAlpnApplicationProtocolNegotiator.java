package io.netty.handler.ssl;

import javax.net.ssl.SSLEngine;

public final class JdkAlpnApplicationProtocolNegotiator
  extends JdkBaseApplicationProtocolNegotiator
{
  private static final JdkApplicationProtocolNegotiator.SslEngineWrapperFactory ALPN_WRAPPER = new JdkApplicationProtocolNegotiator.SslEngineWrapperFactory()
  {
    public SSLEngine wrapSslEngine(SSLEngine engine, JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer)
    {
      return new JdkAlpnSslEngine(engine, applicationNegotiator, isServer);
    }
  };
  
  public JdkAlpnApplicationProtocolNegotiator(Iterable<String> protocols)
  {
    this(false, protocols);
  }
  
  public JdkAlpnApplicationProtocolNegotiator(String... protocols)
  {
    this(false, protocols);
  }
  
  public JdkAlpnApplicationProtocolNegotiator(boolean failIfNoCommonProtocols, Iterable<String> protocols)
  {
    this(failIfNoCommonProtocols, failIfNoCommonProtocols, protocols);
  }
  
  public JdkAlpnApplicationProtocolNegotiator(boolean failIfNoCommonProtocols, String... protocols)
  {
    this(failIfNoCommonProtocols, failIfNoCommonProtocols, protocols);
  }
  
  public JdkAlpnApplicationProtocolNegotiator(boolean clientFailIfNoCommonProtocols, boolean serverFailIfNoCommonProtocols, Iterable<String> protocols)
  {
    this(serverFailIfNoCommonProtocols ? FAIL_SELECTOR_FACTORY : NO_FAIL_SELECTOR_FACTORY, clientFailIfNoCommonProtocols ? FAIL_SELECTION_LISTENER_FACTORY : NO_FAIL_SELECTION_LISTENER_FACTORY, protocols);
  }
  
  public JdkAlpnApplicationProtocolNegotiator(boolean clientFailIfNoCommonProtocols, boolean serverFailIfNoCommonProtocols, String... protocols)
  {
    this(serverFailIfNoCommonProtocols ? FAIL_SELECTOR_FACTORY : NO_FAIL_SELECTOR_FACTORY, clientFailIfNoCommonProtocols ? FAIL_SELECTION_LISTENER_FACTORY : NO_FAIL_SELECTION_LISTENER_FACTORY, protocols);
  }
  
  public JdkAlpnApplicationProtocolNegotiator(JdkApplicationProtocolNegotiator.ProtocolSelectorFactory selectorFactory, JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory listenerFactory, Iterable<String> protocols)
  {
    super(ALPN_WRAPPER, selectorFactory, listenerFactory, protocols);
  }
  
  public JdkAlpnApplicationProtocolNegotiator(JdkApplicationProtocolNegotiator.ProtocolSelectorFactory selectorFactory, JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory listenerFactory, String... protocols)
  {
    super(ALPN_WRAPPER, selectorFactory, listenerFactory, protocols);
  }
}