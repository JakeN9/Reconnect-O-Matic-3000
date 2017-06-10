package io.netty.handler.ssl;

import io.netty.util.internal.ObjectUtil;
import java.util.List;

public final class OpenSslNpnApplicationProtocolNegotiator
  implements OpenSslApplicationProtocolNegotiator
{
  private final List<String> protocols;
  
  public OpenSslNpnApplicationProtocolNegotiator(Iterable<String> protocols)
  {
    this.protocols = ((List)ObjectUtil.checkNotNull(ApplicationProtocolUtil.toList(protocols), "protocols"));
  }
  
  public OpenSslNpnApplicationProtocolNegotiator(String... protocols)
  {
    this.protocols = ((List)ObjectUtil.checkNotNull(ApplicationProtocolUtil.toList(protocols), "protocols"));
  }
  
  public List<String> protocols()
  {
    return this.protocols;
  }
}
