package io.netty.handler.ssl;

import java.util.Collections;
import java.util.List;

final class OpenSslDefaultApplicationProtocolNegotiator
  implements OpenSslApplicationProtocolNegotiator
{
  static final OpenSslDefaultApplicationProtocolNegotiator INSTANCE = new OpenSslDefaultApplicationProtocolNegotiator();
  
  public List<String> protocols()
  {
    return Collections.emptyList();
  }
}
