package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DefaultFullBinaryMemcacheRequest
  extends DefaultBinaryMemcacheRequest
  implements FullBinaryMemcacheRequest
{
  private final ByteBuf content;
  
  public DefaultFullBinaryMemcacheRequest(String key, ByteBuf extras)
  {
    this(key, extras, Unpooled.buffer(0));
  }
  
  public DefaultFullBinaryMemcacheRequest(String key, ByteBuf extras, ByteBuf content)
  {
    super(key, extras);
    if (content == null) {
      throw new NullPointerException("Supplied content is null.");
    }
    this.content = content;
  }
  
  public ByteBuf content()
  {
    return this.content;
  }
  
  public int refCnt()
  {
    return this.content.refCnt();
  }
  
  public FullBinaryMemcacheRequest retain()
  {
    super.retain();
    this.content.retain();
    return this;
  }
  
  public FullBinaryMemcacheRequest retain(int increment)
  {
    super.retain(increment);
    this.content.retain(increment);
    return this;
  }
  
  public FullBinaryMemcacheRequest touch()
  {
    super.touch();
    this.content.touch();
    return this;
  }
  
  public FullBinaryMemcacheRequest touch(Object hint)
  {
    super.touch(hint);
    this.content.touch(hint);
    return this;
  }
  
  public boolean release()
  {
    super.release();
    return this.content.release();
  }
  
  public boolean release(int decrement)
  {
    super.release(decrement);
    return this.content.release(decrement);
  }
  
  public FullBinaryMemcacheRequest copy()
  {
    ByteBuf extras = extras();
    if (extras != null) {
      extras = extras.copy();
    }
    return new DefaultFullBinaryMemcacheRequest(key(), extras, content().copy());
  }
  
  public FullBinaryMemcacheRequest duplicate()
  {
    ByteBuf extras = extras();
    if (extras != null) {
      extras = extras.duplicate();
    }
    return new DefaultFullBinaryMemcacheRequest(key(), extras, content().duplicate());
  }
}
