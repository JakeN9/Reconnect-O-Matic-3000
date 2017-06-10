package io.netty.handler.codec.http;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class ServerCookieDecoder
{
  public static Set<Cookie> decode(String header)
  {
    if (header == null) {
      throw new NullPointerException("header");
    }
    int headerLen = header.length();
    if (headerLen == 0) {
      return Collections.emptySet();
    }
    Set<Cookie> cookies = new TreeSet();
    
    int i = 0;
    
    boolean rfc2965Style = false;
    if (header.regionMatches(true, 0, "$Version", 0, 8))
    {
      i = header.indexOf(';') + 1;
      rfc2965Style = true;
    }
    while (i != headerLen)
    {
      char c = header.charAt(i);
      if ((c == '\t') || (c == '\n') || (c == '\013') || (c == '\f') || (c == '\r') || (c == ' ') || (c == ',') || (c == ';'))
      {
        i++;
      }
      else
      {
        int newNameStart = i;
        int newNameEnd = i;
        String value;
        if (i == headerLen) {
          value = null;
        } else {
          for (;;)
          {
            char curChar = header.charAt(i);
            if (curChar == ';')
            {
              newNameEnd = i;
              String value = null;
              break;
            }
            if (curChar == '=')
            {
              newNameEnd = i;
              i++;
              if (i == headerLen)
              {
                String value = "";
                break;
              }
              int newValueStart = i;
              char c = header.charAt(i);
              if (c == '"')
              {
                StringBuilder newValueBuf = CookieEncoderUtil.stringBuilder();
                
                char q = c;
                boolean hadBackslash = false;
                i++;
                for (;;)
                {
                  if (i == headerLen)
                  {
                    String value = newValueBuf.toString();
                    break;
                  }
                  if (hadBackslash)
                  {
                    hadBackslash = false;
                    c = header.charAt(i++);
                    if ((c == '\\') || (c == '"')) {
                      newValueBuf.setCharAt(newValueBuf.length() - 1, c);
                    } else {
                      newValueBuf.append(c);
                    }
                  }
                  else
                  {
                    c = header.charAt(i++);
                    if (c == q)
                    {
                      String value = newValueBuf.toString();
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
                String value = header.substring(newValueStart, semiPos);
                i = semiPos;
              }
              else
              {
                String value = header.substring(newValueStart);
                i = headerLen;
              }
              break;
            }
            i++;
            if (i == headerLen)
            {
              newNameEnd = headerLen;
              String value = null;
              break;
            }
          }
        }
        String value;
        if ((!rfc2965Style) || ((!header.regionMatches(newNameStart, "$Path", 0, "$Path".length())) && (!header.regionMatches(newNameStart, "$Domain", 0, "$Domain".length())) && (!header.regionMatches(newNameStart, "$Port", 0, "$Port".length()))))
        {
          String name = header.substring(newNameStart, newNameEnd);
          cookies.add(new DefaultCookie(name, value));
        }
      }
    }
    return cookies;
  }
}
