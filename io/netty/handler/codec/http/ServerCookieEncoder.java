package io.netty.handler.codec.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public final class ServerCookieEncoder
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
    
    CookieEncoderUtil.addUnquoted(buf, cookie.name(), cookie.value());
    if (cookie.maxAge() != Long.MIN_VALUE)
    {
      CookieEncoderUtil.add(buf, "Max-Age", cookie.maxAge());
      Date expires = new Date(cookie.maxAge() * 1000L + System.currentTimeMillis());
      CookieEncoderUtil.addUnquoted(buf, "Expires", HttpHeaderDateFormat.get().format(expires));
    }
    if (cookie.path() != null) {
      CookieEncoderUtil.addUnquoted(buf, "Path", cookie.path());
    }
    if (cookie.domain() != null) {
      CookieEncoderUtil.addUnquoted(buf, "Domain", cookie.domain());
    }
    if (cookie.isSecure())
    {
      buf.append("Secure");
      buf.append(';');
      buf.append(' ');
    }
    if (cookie.isHttpOnly())
    {
      buf.append("HTTPOnly");
      buf.append(';');
      buf.append(' ');
    }
    return CookieEncoderUtil.stripTrailingSeparator(buf);
  }
  
  public static List<String> encode(Cookie... cookies)
  {
    if (cookies == null) {
      throw new NullPointerException("cookies");
    }
    if (cookies.length == 0) {
      return Collections.emptyList();
    }
    List<String> encoded = new ArrayList(cookies.length);
    for (Cookie c : cookies)
    {
      if (c == null) {
        break;
      }
      encoded.add(encode(c));
    }
    return encoded;
  }
  
  public static List<String> encode(Collection<Cookie> cookies)
  {
    if (cookies == null) {
      throw new NullPointerException("cookies");
    }
    if (cookies.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> encoded = new ArrayList(cookies.size());
    for (Cookie c : cookies)
    {
      if (c == null) {
        break;
      }
      encoded.add(encode(c));
    }
    return encoded;
  }
  
  public static List<String> encode(Iterable<Cookie> cookies)
  {
    if (cookies == null) {
      throw new NullPointerException("cookies");
    }
    if (!cookies.iterator().hasNext()) {
      return Collections.emptyList();
    }
    List<String> encoded = new ArrayList();
    for (Cookie c : cookies)
    {
      if (c == null) {
        break;
      }
      encoded.add(encode(c));
    }
    return encoded;
  }
}
