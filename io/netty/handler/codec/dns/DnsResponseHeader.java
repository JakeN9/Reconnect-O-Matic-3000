package io.netty.handler.codec.dns;

public final class DnsResponseHeader
  extends DnsHeader
{
  private boolean authoritativeAnswer;
  private boolean truncated;
  private boolean recursionAvailable;
  private DnsResponseCode responseCode;
  
  public DnsResponseHeader(DnsMessage parent, int id)
  {
    super(parent);
    setId(id);
  }
  
  public boolean isAuthoritativeAnswer()
  {
    return this.authoritativeAnswer;
  }
  
  public boolean isTruncated()
  {
    return this.truncated;
  }
  
  public boolean isRecursionAvailable()
  {
    return this.recursionAvailable;
  }
  
  public DnsResponseCode responseCode()
  {
    return this.responseCode;
  }
  
  public int type()
  {
    return 1;
  }
  
  public DnsResponseHeader setAuthoritativeAnswer(boolean authoritativeAnswer)
  {
    this.authoritativeAnswer = authoritativeAnswer;
    return this;
  }
  
  public DnsResponseHeader setTruncated(boolean truncated)
  {
    this.truncated = truncated;
    return this;
  }
  
  public DnsResponseHeader setRecursionAvailable(boolean recursionAvailable)
  {
    this.recursionAvailable = recursionAvailable;
    return this;
  }
  
  public DnsResponseHeader setResponseCode(DnsResponseCode responseCode)
  {
    this.responseCode = responseCode;
    return this;
  }
  
  public DnsResponseHeader setType(int type)
  {
    if (type != 1) {
      throw new IllegalArgumentException("type cannot be anything but TYPE_RESPONSE (1) for a response header.");
    }
    super.setType(type);
    return this;
  }
  
  public DnsResponseHeader setId(int id)
  {
    super.setId(id);
    return this;
  }
  
  public DnsHeader setRecursionDesired(boolean recursionDesired)
  {
    return super.setRecursionDesired(recursionDesired);
  }
  
  public DnsResponseHeader setOpcode(int opcode)
  {
    super.setOpcode(opcode);
    return this;
  }
  
  public DnsResponseHeader setZ(int z)
  {
    super.setZ(z);
    return this;
  }
}
