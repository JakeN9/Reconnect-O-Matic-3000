package io.netty.util.internal;

public final class ObjectUtil
{
  public static <T> T checkNotNull(T arg, String text)
  {
    if (arg == null) {
      throw new NullPointerException(text);
    }
    return arg;
  }
}
