package io.netty.handler.codec.dns;

import io.netty.util.internal.StringUtil;

public class DnsEntry
{
  private final String name;
  private final DnsType type;
  private final DnsClass dnsClass;
  
  DnsEntry(String name, DnsType type, DnsClass dnsClass)
  {
    if (name == null) {
      throw new NullPointerException("name");
    }
    if (type == null) {
      throw new NullPointerException("type");
    }
    if (dnsClass == null) {
      throw new NullPointerException("dnsClass");
    }
    this.name = name;
    this.type = type;
    this.dnsClass = dnsClass;
  }
  
  public String name()
  {
    return this.name;
  }
  
  public DnsType type()
  {
    return this.type;
  }
  
  public DnsClass dnsClass()
  {
    return this.dnsClass;
  }
  
  public int hashCode()
  {
    return (this.name.hashCode() * 31 + this.type.hashCode()) * 31 + this.dnsClass.hashCode();
  }
  
  public String toString()
  {
    return 128 + StringUtil.simpleClassName(this) + "(name: " + this.name + ", type: " + this.type + ", class: " + this.dnsClass + ')';
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DnsEntry)) {
      return false;
    }
    DnsEntry that = (DnsEntry)o;
    return (type().intValue() == that.type().intValue()) && (dnsClass().intValue() == that.dnsClass().intValue()) && (name().equals(that.name()));
  }
}
