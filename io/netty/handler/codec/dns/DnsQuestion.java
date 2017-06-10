package io.netty.handler.codec.dns;

public final class DnsQuestion
  extends DnsEntry
{
  public DnsQuestion(String name, DnsType type)
  {
    this(name, type, DnsClass.IN);
  }
  
  public DnsQuestion(String name, DnsType type, DnsClass qClass)
  {
    super(name, type, qClass);
    if (name.isEmpty()) {
      throw new IllegalArgumentException("name must not be left blank.");
    }
  }
  
  public boolean equals(Object other)
  {
    if (!(other instanceof DnsQuestion)) {
      return false;
    }
    return super.equals(other);
  }
  
  public int hashCode()
  {
    return super.hashCode();
  }
}
