package io.netty.handler.codec.memcache;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

public class DefaultMemcacheContent
  extends AbstractMemcacheObject
  implements MemcacheContent
{
  private final ByteBuf content;
  
  public DefaultMemcacheContent(ByteBuf content)
  {
    if (content == null) {
      throw new NullPointerException("Content cannot be null.");
    }
    this.content = content;
  }
  
  public ByteBuf content()
  {
    return this.content;
  }
  
  public MemcacheContent copy()
  {
    return new DefaultMemcacheContent(this.content.copy());
  }
  
  public MemcacheContent duplicate()
  {
    return new DefaultMemcacheContent(this.content.duplicate());
  }
  
  public int refCnt()
  {
    return this.content.refCnt();
  }
  
  public MemcacheContent retain()
  {
    this.content.retain();
    return this;
  }
  
  public MemcacheContent retain(int increment)
  {
    this.content.retain(increment);
    return this;
  }
  
  public MemcacheContent touch()
  {
    this.content.touch();
    return this;
  }
  
  public MemcacheContent touch(Object hint)
  {
    this.content.touch(hint);
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
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + "(data: " + content() + ", decoderResult: " + decoderResult() + ')';
  }
}
