package io.netty.handler.codec.http;

import java.text.ParsePosition;
import java.util.Date;

public final class ClientCookieDecoder
{
  public static Cookie decode(String header)
  {
    if (header == null) {
      throw new NullPointerException("header");
    }
    int headerLen = header.length();
    if (headerLen == 0) {
      return null;
    }
    CookieBuilder cookieBuilder = null;
    
    int i = 0;
    while (i != headerLen)
    {
      char c = header.charAt(i);
      if (c == ',') {
        break;
      }
      if ((c == '\t') || (c == '\n') || (c == '\013') || (c == '\f') || (c == '\r') || (c == ' ') || (c == ';'))
      {
        i++;
      }
      else
      {
        int newNameStart = i;
        int newNameEnd = i;
        String value;
        if (i == headerLen)
        {
          String rawValue;
          value = rawValue = null;
        }
        else
        {
          for (;;)
          {
            char curChar = header.charAt(i);
            if (curChar == ';')
            {
              newNameEnd = i;
              String rawValue;
              String value = rawValue = null;
              break;
            }
            if (curChar == '=')
            {
              newNameEnd = i;
              i++;
              if (i == headerLen)
              {
                String rawValue;
                String value = rawValue = "";
                break;
              }
              int newValueStart = i;
              char c = header.charAt(i);
              if (c == '"')
              {
                StringBuilder newValueBuf = CookieEncoderUtil.stringBuilder();
                
                int rawValueStart = i;
                int rawValueEnd = i;
                
                char q = c;
                boolean hadBackslash = false;
                i++;
                for (;;)
                {
                  if (i == headerLen)
                  {
                    String value = newValueBuf.toString();
                    
                    String rawValue = header.substring(rawValueStart, rawValueEnd);
                    break;
                  }
                  if (hadBackslash)
                  {
                    hadBackslash = false;
                    c = header.charAt(i++);
                    rawValueEnd = i;
                    if ((c == '\\') || (c == '"')) {
                      newValueBuf.setCharAt(newValueBuf.length() - 1, c);
                    } else {
                      newValueBuf.append(c);
                    }
                  }
                  else
                  {
                    c = header.charAt(i++);
                    rawValueEnd = i;
                    if (c == q)
                    {
                      String value = newValueBuf.toString();
                      
                      String rawValue = header.substring(rawValueStart, rawValueEnd);
                      break;
                    }
                    newValueBuf.append(c);
                    if (c == '\\') {
                      hadBackslash = true;
                    }
                  }
                }
              }
              int semiPos = header.indexOf(';', i);
              if (semiPos > 0)
              {
                String rawValue;
                String value = rawValue = header.substring(newValueStart, semiPos);
                i = semiPos;
              }
              else
              {
                String rawValue;
                String value = rawValue = header.substring(newValueStart);
                i = headerLen;
              }
              break;
            }
            i++;
            if (i == headerLen)
            {
              newNameEnd = i;
              String rawValue;
              String value = rawValue = null;
              break;
            }
          }
        }
        String rawValue;
        String value;
        if (cookieBuilder == null) {
          cookieBuilder = new CookieBuilder(header, newNameStart, newNameEnd, value, rawValue);
        } else {
          cookieBuilder.appendAttribute(header, newNameStart, newNameEnd, value);
        }
      }
    }
    return cookieBuilder.cookie();
  }
  
  private static class CookieBuilder
  {
    private final String name;
    private final String value;
    private final String rawValue;
    private String domain;
    private String path;
    private long maxAge = Long.MIN_VALUE;
    private String expires;
    private boolean secure;
    private boolean httpOnly;
    
    public CookieBuilder(String header, int keyStart, int keyEnd, String value, String rawValue)
    {
      this.name = header.substring(keyStart, keyEnd);
      this.value = value;
      this.rawValue = rawValue;
    }
    
    private long mergeMaxAgeAndExpire(long maxAge, String expires)
    {
      if (maxAge != Long.MIN_VALUE) {
        return maxAge;
      }
      if (expires != null)
      {
        Date expiresDate = HttpHeaderDateFormat.get().parse(expires, new ParsePosition(0));
        if (expiresDate != null)
        {
          long maxAgeMillis = expiresDate.getTime() - System.currentTimeMillis();
          return maxAgeMillis / 1000L + (maxAgeMillis % 1000L != 0L ? 1 : 0);
        }
      }
      return Long.MIN_VALUE;
    }
    
    public Cookie cookie()
    {
      if (this.name == null) {
        return null;
      }
      DefaultCookie cookie = new DefaultCookie(this.name, this.value);
      cookie.setValue(this.value);
      cookie.setRawValue(this.rawValue);
      cookie.setDomain(this.domain);
      cookie.setPath(this.path);
      cookie.setMaxAge(mergeMaxAgeAndExpire(this.maxAge, this.expires));
      cookie.setSecure(this.secure);
      cookie.setHttpOnly(this.httpOnly);
      return cookie;
    }
    
    public void appendAttribute(String header, int keyStart, int keyEnd, String value)
    {
      setCookieAttribute(header, keyStart, keyEnd, value);
    }
    
    private void setCookieAttribute(String header, int keyStart, int keyEnd, String value)
    {
      int length = keyEnd - keyStart;
      if (length == 4) {
        parse4(header, keyStart, value);
      } else if (length == 6) {
        parse6(header, keyStart, value);
      } else if (length == 7) {
        parse7(header, keyStart, value);
      } else if (length == 8) {
        parse8(header, keyStart, value);
      }
    }
    
    private void parse4(String header, int nameStart, String value)
    {
      if (header.regionMatches(true, nameStart, "Path", 0, 4)) {
        this.path = value;
      }
    }
    
    private void parse6(String header, int nameStart, String value)
    {
      if (header.regionMatches(true, nameStart, "Domain", 0, 5)) {
        this.domain = (value.isEmpty() ? null : value);
      } else if (header.regionMatches(true, nameStart, "Secure", 0, 5)) {
        this.secure = true;
      }
    }
    
    private void setExpire(String value)
    {
      this.expires = value;
    }
    
    private void setMaxAge(String value)
    {
      try
      {
        this.maxAge = Math.max(Long.valueOf(value).longValue(), 0L);
      }
      catch (NumberFormatException e1) {}
    }
    
    private void parse7(String header, int nameStart, String value)
    {
      if (header.regionMatches(true, nameStart, "Expires", 0, 7)) {
        setExpire(value);
      } else if (header.regionMatches(true, nameStart, "Max-Age", 0, 7)) {
        setMaxAge(value);
      }
    }
    
    private void parse8(String header, int nameStart, String value)
    {
      if (header.regionMatches(true, nameStart, "HttpOnly", 0, 8)) {
        this.httpOnly = true;
      }
    }
  }
}
