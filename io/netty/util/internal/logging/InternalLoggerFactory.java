package io.netty.util.internal.logging;

import io.netty.util.internal.ThreadLocalRandom;

public abstract class InternalLoggerFactory
{
  private static volatile InternalLoggerFactory defaultFactory = newDefaultFactory(InternalLoggerFactory.class.getName());
  
  static
  {
    try
    {
      Class.forName(ThreadLocalRandom.class.getName(), true, InternalLoggerFactory.class.getClassLoader());
    }
    catch (Exception ignored) {}
  }
  
  private static InternalLoggerFactory newDefaultFactory(String name)
  {
    InternalLoggerFactory f;
    try
    {
      f = new Slf4JLoggerFactory(true);
      f.newInstance(name).debug("Using SLF4J as the default logging framework");
    }
    catch (Throwable t1)
    {
      try
      {
        f = new Log4JLoggerFactory();
        f.newInstance(name).debug("Using Log4J as the default logging framework");
      }
      catch (Throwable t2)
      {
        f = new JdkLoggerFactory();
        f.newInstance(name).debug("Using java.util.logging as the default logging framework");
      }
    }
    return f;
  }
  
  public static InternalLoggerFactory getDefaultFactory()
  {
    return defaultFactory;
  }
  
  public static void setDefaultFactory(InternalLoggerFactory defaultFactory)
  {
    if (defaultFactory == null) {
      throw new NullPointerException("defaultFactory");
    }
    defaultFactory = defaultFactory;
  }
  
  public static InternalLogger getInstance(Class<?> clazz)
  {
    return getInstance(clazz.getName());
  }
  
  public static InternalLogger getInstance(String name)
  {
    return getDefaultFactory().newInstance(name);
  }
  
  protected abstract InternalLogger newInstance(String paramString);
}
