package io.netty.handler.codec.dns;

import java.net.InetSocketAddress;

public class DnsQuery
  extends DnsMessage
{
  private final InetSocketAddress recipient;
  
  public DnsQuery(int id, InetSocketAddress recipient)
  {
    super(id);
    if (recipient == null) {
      throw new NullPointerException("recipient");
    }
    this.recipient = recipient;
  }
  
  public InetSocketAddress recipient()
  {
    return this.recipient;
  }
  
  public DnsQuery addAnswer(DnsResource answer)
  {
    super.addAnswer(answer);
    return this;
  }
  
  public DnsQuery addQuestion(DnsQuestion question)
  {
    super.addQuestion(question);
    return this;
  }
  
  public DnsQuery addAuthorityResource(DnsResource resource)
  {
    super.addAuthorityResource(resource);
    return this;
  }
  
  public DnsQuery addAdditionalResource(DnsResource resource)
  {
    super.addAdditionalResource(resource);
    return this;
  }
  
  public DnsQuery touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
  
  public DnsQuery retain()
  {
    super.retain();
    return this;
  }
  
  public DnsQuery retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public DnsQuery touch()
  {
    super.touch();
    return this;
  }
  
  public DnsQueryHeader header()
  {
    return (DnsQueryHeader)super.header();
  }
  
  protected DnsQueryHeader newHeader(int id)
  {
    return new DnsQueryHeader(this, id);
  }
}
