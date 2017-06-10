package io.netty.handler.codec.http;

import io.netty.handler.codec.AsciiString;
import java.util.HashMap;
import java.util.Map;

public class HttpMethod
  implements Comparable<HttpMethod>
{
  public static final HttpMethod OPTIONS = new HttpMethod("OPTIONS");
  public static final HttpMethod GET = new HttpMethod("GET");
  public static final HttpMethod HEAD = new HttpMethod("HEAD");
  public static final HttpMethod POST = new HttpMethod("POST");
  public static final HttpMethod PUT = new HttpMethod("PUT");
  public static final HttpMethod PATCH = new HttpMethod("PATCH");
  public static final HttpMethod DELETE = new HttpMethod("DELETE");
  public static final HttpMethod TRACE = new HttpMethod("TRACE");
  public static final HttpMethod CONNECT = new HttpMethod("CONNECT");
  private static final Map<String, HttpMethod> methodMap = new HashMap();
  private final AsciiString name;
  private final String nameAsString;
  
  static
  {
    methodMap.put(OPTIONS.toString(), OPTIONS);
    methodMap.put(GET.toString(), GET);
    methodMap.put(HEAD.toString(), HEAD);
    methodMap.put(POST.toString(), POST);
    methodMap.put(PUT.toString(), PUT);
    methodMap.put(PATCH.toString(), PATCH);
    methodMap.put(DELETE.toString(), DELETE);
    methodMap.put(TRACE.toString(), TRACE);
    methodMap.put(CONNECT.toString(), CONNECT);
  }
  
  public static HttpMethod valueOf(String name)
  {
    if (name == null) {
      throw new NullPointerException("name");
    }
    name = name.trim();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("empty name");
    }
    HttpMethod result = (HttpMethod)methodMap.get(name);
    if (result != null) {
      return result;
    }
    return new HttpMethod(name);
  }
  
  public HttpMethod(String name)
  {
    if (name == null) {
      throw new NullPointerException("name");
    }
    name = name.trim();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("empty name");
    }
    for (int i = 0; i < name.length(); i++)
    {
      char c = name.charAt(i);
      if ((Character.isISOControl(c)) || (Character.isWhitespace(c))) {
        throw new IllegalArgumentException("invalid character in name");
      }
    }
    this.name = new AsciiString(name);
    this.nameAsString = name;
  }
  
  public AsciiString name()
  {
    return this.name;
  }
  
  public int hashCode()
  {
    return name().hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof HttpMethod)) {
      return false;
    }
    HttpMethod that = (HttpMethod)o;
    return name().equals(that.name());
  }
  
  public String toString()
  {
    return this.nameAsString;
  }
  
  public int compareTo(HttpMethod o)
  {
    return name().compareTo(o.name());
  }
}
