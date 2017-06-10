package io.netty.handler.ssl;

import java.util.List;

public abstract interface ApplicationProtocolNegotiator
{
  public abstract List<String> protocols();
}
