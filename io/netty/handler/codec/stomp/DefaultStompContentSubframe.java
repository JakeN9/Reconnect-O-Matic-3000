package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderResult;

public class DefaultStompContentSubframe
  implements StompContentSubframe
{
  private DecoderResult decoderResult = DecoderResult.SUCCESS;
  private final ByteBuf content;
  
  public DefaultStompContentSubframe(ByteBuf content)
  {
    if (content == null) {
      throw new NullPointerException("content");
    }
    this.content = content;
  }
  
  public ByteBuf content()
  {
    return this.content;
  }
  
  public StompContentSubframe copy()
  {
    return new DefaultStompContentSubframe(content().copy());
  }
  
  public StompContentSubframe duplicate()
  {
    return new DefaultStompContentSubframe(content().duplicate());
  }
  
  public int refCnt()
  {
    return content().refCnt();
  }
  
  public StompContentSubframe retain()
  {
    content().retain();
    return this;
  }
  
  public StompContentSubframe retain(int increment)
  {
    content().retain(increment);
    return this;
  }
  
  public StompContentSubframe touch()
  {
    this.content.touch();
    return this;
  }
  
  public StompContentSubframe touch(Object hint)
  {
    this.content.touch(hint);
    return this;
  }
  
  public boolean release()
  {
    return content().release();
  }
  
  public boolean release(int decrement)
  {
    return content().release(decrement);
  }
  
  public DecoderResult decoderResult()
  {
    return this.decoderResult;
  }
  
  public void setDecoderResult(DecoderResult decoderResult)
  {
    this.decoderResult = decoderResult;
  }
  
  public String toString()
  {
    return "DefaultStompContent{decoderResult=" + this.decoderResult + '}';
  }
}
