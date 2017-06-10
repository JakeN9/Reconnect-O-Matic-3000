package io.netty.channel.udt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

public final class UdtMessage
  extends DefaultByteBufHolder
{
  public UdtMessage(ByteBuf data)
  {
    super(data);
  }
  
  public UdtMessage copy()
  {
    return new UdtMessage(content().copy());
  }
  
  public UdtMessage duplicate()
  {
    return new UdtMessage(content().duplicate());
  }
  
  public UdtMessage retain()
  {
    super.retain();
    return this;
  }
  
  public UdtMessage retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public UdtMessage touch()
  {
    super.touch();
    return this;
  }
  
  public UdtMessage touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
}
