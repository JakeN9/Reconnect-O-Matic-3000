package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;

public class DefaultLastStompContentSubframe
  extends DefaultStompContentSubframe
  implements LastStompContentSubframe
{
  public DefaultLastStompContentSubframe(ByteBuf content)
  {
    super(content);
  }
  
  public DefaultLastStompContentSubframe retain()
  {
    super.retain();
    return this;
  }
  
  public LastStompContentSubframe retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public LastStompContentSubframe touch()
  {
    super.touch();
    return this;
  }
  
  public LastStompContentSubframe touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
  
  public LastStompContentSubframe copy()
  {
    return new DefaultLastStompContentSubframe(content().copy());
  }
  
  public LastStompContentSubframe duplicate()
  {
    return new DefaultLastStompContentSubframe(content().duplicate());
  }
  
  public String toString()
  {
    return "DefaultLastStompContent{decoderResult=" + decoderResult() + '}';
  }
}
