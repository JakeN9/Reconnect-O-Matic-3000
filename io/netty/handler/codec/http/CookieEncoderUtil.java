package io.netty.handler.codec.http;

import io.netty.util.internal.InternalThreadLocalMap;

final class CookieEncoderUtil
{
  static StringBuilder stringBuilder()
  {
    return InternalThreadLocalMap.get().stringBuilder();
  }
  
  static String stripTrailingSeparatorOrNull(StringBuilder buf)
  {
    return buf.length() == 0 ? null : stripTrailingSeparator(buf);
  }
  
  static String stripTrailingSeparator(StringBuilder buf)
  {
    if (buf.length() > 0) {
      buf.setLength(buf.length() - 2);
    }
    return buf.toString();
  }
  
  static void addUnquoted(StringBuilder sb, String name, String val)
  {
    sb.append(name);
    sb.append('=');
    sb.append(val);
    sb.append(';');
    sb.append(' ');
  }
  
  static void add(StringBuilder sb, String name, long val)
  {
    sb.append(name);
    sb.append('=');
    sb.append(val);
    sb.append(';');
    sb.append(' ');
  }
}
