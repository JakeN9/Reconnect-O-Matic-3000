package io.netty.handler.codec.dns;

public final class DnsQueryHeader
  extends DnsHeader
{
  public DnsQueryHeader(DnsMessage parent, int id)
  {
    super(parent);
    setId(id);
    setRecursionDesired(true);
  }
  
  public int type()
  {
    return 0;
  }
  
  public DnsQueryHeader setType(int type)
  {
    if (type != 0) {
      throw new IllegalArgumentException("type cannot be anything but TYPE_QUERY (0) for a query header.");
    }
    super.setType(type);
    return this;
  }
  
  public DnsQueryHeader setId(int id)
  {
    super.setId(id);
    return this;
  }
  
  public DnsQueryHeader setRecursionDesired(boolean recursionDesired)
  {
    super.setRecursionDesired(recursionDesired);
    return this;
  }
  
  public DnsQueryHeader setOpcode(int opcode)
  {
    super.setOpcode(opcode);
    return this;
  }
  
  public DnsQueryHeader setZ(int z)
  {
    super.setZ(z);
    return this;
  }
}
