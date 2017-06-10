package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderResult;

final class ComposedLastHttpContent
  implements LastHttpContent
{
  private final HttpHeaders trailingHeaders;
  private DecoderResult result;
  
  ComposedLastHttpContent(HttpHeaders trailingHeaders)
  {
    this.trailingHeaders = trailingHeaders;
  }
  
  public HttpHeaders trailingHeaders()
  {
    return this.trailingHeaders;
  }
  
  public LastHttpContent copy()
  {
    LastHttpContent content = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
    content.trailingHeaders().set(trailingHeaders());
    return content;
  }
  
  public LastHttpContent retain(int increment)
  {
    return this;
  }
  
  public LastHttpContent retain()
  {
    return this;
  }
  
  public LastHttpContent touch()
  {
    return this;
  }
  
  public LastHttpContent touch(Object hint)
  {
    return this;
  }
  
  public LastHttpContent duplicate()
  {
    return copy();
  }
  
  public ByteBuf content()
  {
    return Unpooled.EMPTY_BUFFER;
  }
  
  public DecoderResult decoderResult()
  {
    return this.result;
  }
  
  public void setDecoderResult(DecoderResult result)
  {
    this.result = result;
  }
  
  public int refCnt()
  {
    return 1;
  }
  
  public boolean release()
  {
    return false;
  }
  
  public boolean release(int decrement)
  {
    return false;
  }
}
