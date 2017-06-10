package io.netty.handler.codec.http;

import java.util.Iterator;

public final class ClientCookieEncoder
{
  public static String encode(String name, String value)
  {
    return encode(new DefaultCookie(name, value));
  }
  
  public static String encode(Cookie cookie)
  {
    if (cookie == null) {
      throw new NullPointerException("cookie");
    }
    StringBuilder buf = CookieEncoderUtil.stringBuilder();
    encode(buf, cookie);
    return CookieEncoderUtil.stripTrailingSeparator(buf);
  }
  
  public static String encode(Cookie... cookies)
  {
    if (cookies == null) {
      throw new NullPointerException("cookies");
    }
    if (cookies.length == 0) {
      return null;
    }
    StringBuilder buf = CookieEncoderUtil.stringBuilder();
    for (Cookie c : cookies)
    {
      if (c == null) {
        break;
      }
      encode(buf, c);
    }
    return CookieEncoderUtil.stripTrailingSeparatorOrNull(buf);
  }
  
  public static String encode(Iterable<Cookie> cookies)
  {
    if (cookies == null) {
      throw new NullPointerException("cookies");
    }
    if (!cookies.iterator().hasNext()) {
      return null;
    }
    StringBuilder buf = CookieEncoderUtil.stringBuilder();
    for (Cookie c : cookies)
    {
      if (c == null) {
        break;
      }
      encode(buf, c);
    }
    return CookieEncoderUtil.stripTrailingSeparatorOrNull(buf);
  }
  
  private static void encode(StringBuilder buf, Cookie c)
  {
    String value = c.value() != null ? c.value() : c.rawValue() != null ? c.rawValue() : "";
    
    CookieEncoderUtil.addUnquoted(buf, c.name(), value);
  }
}
