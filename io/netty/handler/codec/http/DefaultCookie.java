package io.netty.handler.codec.http;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class DefaultCookie
  implements Cookie
{
  private final String name;
  private String value;
  private String rawValue;
  private String domain;
  private String path;
  private String comment;
  private String commentUrl;
  private boolean discard;
  private Set<Integer> ports = Collections.emptySet();
  private Set<Integer> unmodifiablePorts = this.ports;
  private long maxAge = Long.MIN_VALUE;
  private int version;
  private boolean secure;
  private boolean httpOnly;
  
  public DefaultCookie(String name, String value)
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
      if (c > '') {
        throw new IllegalArgumentException("name contains non-ascii character: " + name);
      }
      switch (c)
      {
      case '\t': 
      case '\n': 
      case '\013': 
      case '\f': 
      case '\r': 
      case ' ': 
      case ',': 
      case ';': 
      case '=': 
        throw new IllegalArgumentException("name contains one of the following prohibited characters: =,; \\t\\r\\n\\v\\f: " + name);
      }
    }
    if (name.charAt(0) == '$') {
      throw new IllegalArgumentException("name starting with '$' not allowed: " + name);
    }
    this.name = name;
    setValue(value);
  }
  
  public String name()
  {
    return this.name;
  }
  
  public String value()
  {
    return this.value;
  }
  
  public void setValue(String value)
  {
    if (value == null) {
      throw new NullPointerException("value");
    }
    this.value = value;
  }
  
  public String rawValue()
  {
    return this.rawValue;
  }
  
  public void setRawValue(String rawValue)
  {
    if (this.value == null) {
      throw new NullPointerException("rawValue");
    }
    this.rawValue = rawValue;
  }
  
  public String domain()
  {
    return this.domain;
  }
  
  public void setDomain(String domain)
  {
    this.domain = validateValue("domain", domain);
  }
  
  public String path()
  {
    return this.path;
  }
  
  public void setPath(String path)
  {
    this.path = validateValue("path", path);
  }
  
  public String comment()
  {
    return this.comment;
  }
  
  public void setComment(String comment)
  {
    this.comment = validateValue("comment", comment);
  }
  
  public String commentUrl()
  {
    return this.commentUrl;
  }
  
  public void setCommentUrl(String commentUrl)
  {
    this.commentUrl = validateValue("commentUrl", commentUrl);
  }
  
  public boolean isDiscard()
  {
    return this.discard;
  }
  
  public void setDiscard(boolean discard)
  {
    this.discard = discard;
  }
  
  public Set<Integer> ports()
  {
    if (this.unmodifiablePorts == null) {
      this.unmodifiablePorts = Collections.unmodifiableSet(this.ports);
    }
    return this.unmodifiablePorts;
  }
  
  public void setPorts(int... ports)
  {
    if (ports == null) {
      throw new NullPointerException("ports");
    }
    int[] portsCopy = (int[])ports.clone();
    if (portsCopy.length == 0)
    {
      this.unmodifiablePorts = (this.ports = Collections.emptySet());
    }
    else
    {
      Set<Integer> newPorts = new TreeSet();
      for (int p : portsCopy)
      {
        if ((p <= 0) || (p > 65535)) {
          throw new IllegalArgumentException("port out of range: " + p);
        }
        newPorts.add(Integer.valueOf(p));
      }
      this.ports = newPorts;
      this.unmodifiablePorts = null;
    }
  }
  
  public void setPorts(Iterable<Integer> ports)
  {
    Set<Integer> newPorts = new TreeSet();
    for (Iterator i$ = ports.iterator(); i$.hasNext();)
    {
      int p = ((Integer)i$.next()).intValue();
      if ((p <= 0) || (p > 65535)) {
        throw new IllegalArgumentException("port out of range: " + p);
      }
      newPorts.add(Integer.valueOf(p));
    }
    if (newPorts.isEmpty())
    {
      this.unmodifiablePorts = (this.ports = Collections.emptySet());
    }
    else
    {
      this.ports = newPorts;
      this.unmodifiablePorts = null;
    }
  }
  
  public long maxAge()
  {
    return this.maxAge;
  }
  
  public void setMaxAge(long maxAge)
  {
    this.maxAge = maxAge;
  }
  
  public int version()
  {
    return this.version;
  }
  
  public void setVersion(int version)
  {
    this.version = version;
  }
  
  public boolean isSecure()
  {
    return this.secure;
  }
  
  public void setSecure(boolean secure)
  {
    this.secure = secure;
  }
  
  public boolean isHttpOnly()
  {
    return this.httpOnly;
  }
  
  public void setHttpOnly(boolean httpOnly)
  {
    this.httpOnly = httpOnly;
  }
  
  public int hashCode()
  {
    return name().hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof Cookie)) {
      return false;
    }
    Cookie that = (Cookie)o;
    if (!name().equalsIgnoreCase(that.name())) {
      return false;
    }
    if (path() == null)
    {
      if (that.path() != null) {
        return false;
      }
    }
    else
    {
      if (that.path() == null) {
        return false;
      }
      if (!path().equals(that.path())) {
        return false;
      }
    }
    if (domain() == null)
    {
      if (that.domain() != null) {
        return false;
      }
    }
    else
    {
      if (that.domain() == null) {
        return false;
      }
      return domain().equalsIgnoreCase(that.domain());
    }
    return true;
  }
  
  public int compareTo(Cookie c)
  {
    int v = name().compareToIgnoreCase(c.name());
    if (v != 0) {
      return v;
    }
    if (path() == null)
    {
      if (c.path() != null) {
        return -1;
      }
    }
    else
    {
      if (c.path() == null) {
        return 1;
      }
      v = path().compareTo(c.path());
      if (v != 0) {
        return v;
      }
    }
    if (domain() == null)
    {
      if (c.domain() != null) {
        return -1;
      }
    }
    else
    {
      if (c.domain() == null) {
        return 1;
      }
      v = domain().compareToIgnoreCase(c.domain());
      return v;
    }
    return 0;
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder().append(name()).append('=').append(value());
    if (domain() != null) {
      buf.append(", domain=").append(domain());
    }
    if (path() != null) {
      buf.append(", path=").append(path());
    }
    if (comment() != null) {
      buf.append(", comment=").append(comment());
    }
    if (maxAge() >= 0L) {
      buf.append(", maxAge=").append(maxAge()).append('s');
    }
    if (isSecure()) {
      buf.append(", secure");
    }
    if (isHttpOnly()) {
      buf.append(", HTTPOnly");
    }
    return buf.toString();
  }
  
  private static String validateValue(String name, String value)
  {
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (value.isEmpty()) {
      return null;
    }
    for (int i = 0; i < value.length(); i++)
    {
      char c = value.charAt(i);
      switch (c)
      {
      case '\n': 
      case '\013': 
      case '\f': 
      case '\r': 
      case ';': 
        throw new IllegalArgumentException(name + " contains one of the following prohibited characters: " + ";\\r\\n\\f\\v (" + value + ')');
      }
    }
    return value;
  }
}
