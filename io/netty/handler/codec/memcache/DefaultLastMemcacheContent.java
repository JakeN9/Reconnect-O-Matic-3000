package io.netty.handler.codec.memcache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DefaultLastMemcacheContent
  extends DefaultMemcacheContent
  implements LastMemcacheContent
{
  public DefaultLastMemcacheContent()
  {
    super(Unpooled.buffer());
  }
  
  public DefaultLastMemcacheContent(ByteBuf content)
  {
    super(content);
  }
  
  public LastMemcacheContent retain()
  {
    super.retain();
    return this;
  }
  
  public LastMemcacheContent retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public LastMemcacheContent touch()
  {
    super.touch();
    return this;
  }
  
  public LastMemcacheContent touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
  
  public LastMemcacheContent copy()
  {
    return new DefaultLastMemcacheContent(content().copy());
  }
  
  public LastMemcacheContent duplicate()
  {
    return new DefaultLastMemcacheContent(content().duplicate());
  }
}
