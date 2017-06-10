package io.netty.handler.codec.dns;

import java.net.InetSocketAddress;

public final class DnsResponse
  extends DnsMessage
{
  private final InetSocketAddress sender;
  
  public DnsResponse(int id, InetSocketAddress sender)
  {
    super(id);
    if (sender == null) {
      throw new NullPointerException("sender");
    }
    this.sender = sender;
  }
  
  public InetSocketAddress sender()
  {
    return this.sender;
  }
  
  public DnsResponse addAnswer(DnsResource answer)
  {
    super.addAnswer(answer);
    return this;
  }
  
  public DnsResponse addQuestion(DnsQuestion question)
  {
    super.addQuestion(question);
    return this;
  }
  
  public DnsResponse addAuthorityResource(DnsResource resource)
  {
    super.addAuthorityResource(resource);
    return this;
  }
  
  public DnsResponse addAdditionalResource(DnsResource resource)
  {
    super.addAdditionalResource(resource);
    return this;
  }
  
  public DnsResponse touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
  
  public DnsResponse retain()
  {
    super.retain();
    return this;
  }
  
  public DnsResponse retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public DnsResponse touch()
  {
    super.touch();
    return this;
  }
  
  public DnsResponseHeader header()
  {
    return (DnsResponseHeader)super.header();
  }
  
  protected DnsResponseHeader newHeader(int id)
  {
    return new DnsResponseHeader(this, id);
  }
}
