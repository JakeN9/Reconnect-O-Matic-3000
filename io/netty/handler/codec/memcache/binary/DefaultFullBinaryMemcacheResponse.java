package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DefaultFullBinaryMemcacheResponse
  extends DefaultBinaryMemcacheResponse
  implements FullBinaryMemcacheResponse
{
  private final ByteBuf content;
  
  public DefaultFullBinaryMemcacheResponse(String key, ByteBuf extras)
  {
    this(key, extras, Unpooled.buffer(0));
  }
  
  public DefaultFullBinaryMemcacheResponse(String key, ByteBuf extras, ByteBuf content)
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
  
  public FullBinaryMemcacheResponse retain()
  {
    super.retain();
    this.content.retain();
    return this;
  }
  
  public FullBinaryMemcacheResponse retain(int increment)
  {
    super.retain(increment);
    this.content.retain(increment);
    return this;
  }
  
  public FullBinaryMemcacheResponse touch()
  {
    super.touch();
    this.content.touch();
    return this;
  }
  
  public FullBinaryMemcacheResponse touch(Object hint)
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
  
  public FullBinaryMemcacheResponse copy()
  {
    ByteBuf extras = extras();
    if (extras != null) {
      extras = extras.copy();
    }
    return new DefaultFullBinaryMemcacheResponse(key(), extras, content().copy());
  }
  
  public FullBinaryMemcacheResponse duplicate()
  {
    ByteBuf extras = extras();
    if (extras != null) {
      extras = extras.duplicate();
    }
    return new DefaultFullBinaryMemcacheResponse(key(), extras, content().duplicate());
  }
}
