package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

public final class DnsResource
  extends DnsEntry
  implements ByteBufHolder
{
  private final long ttl;
  private final ByteBuf content;
  
  public DnsResource(String name, DnsType type, DnsClass aClass, long ttl, ByteBuf content)
  {
    super(name, type, aClass);
    this.ttl = ttl;
    this.content = content;
  }
  
  public long timeToLive()
  {
    return this.ttl;
  }
  
  public ByteBuf content()
  {
    return this.content;
  }
  
  public DnsResource copy()
  {
    return new DnsResource(name(), type(), dnsClass(), this.ttl, this.content.copy());
  }
  
  public DnsResource duplicate()
  {
    return new DnsResource(name(), type(), dnsClass(), this.ttl, this.content.duplicate());
  }
  
  public int refCnt()
  {
    return this.content.refCnt();
  }
  
  public DnsResource retain()
  {
    this.content.retain();
    return this;
  }
  
  public DnsResource retain(int increment)
  {
    this.content.retain(increment);
    return this;
  }
  
  public boolean release()
  {
    return this.content.release();
  }
  
  public boolean release(int decrement)
  {
    return this.content.release(decrement);
  }
  
  public DnsResource touch()
  {
    this.content.touch();
    return this;
  }
  
  public DnsResource touch(Object hint)
  {
    this.content.touch(hint);
    return this;
  }
}
