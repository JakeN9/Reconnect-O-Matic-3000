package io.netty.util.internal.logging;

import java.util.logging.Logger;

public class JdkLoggerFactory
  extends InternalLoggerFactory
{
  public InternalLogger newInstance(String name)
  {
    return new JdkLogger(Logger.getLogger(name));
  }
}
