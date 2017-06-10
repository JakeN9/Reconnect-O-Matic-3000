package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.MessageAggregator;

public class StompSubframeAggregator
  extends MessageAggregator<StompSubframe, StompHeadersSubframe, StompContentSubframe, StompFrame>
{
  public StompSubframeAggregator(int maxContentLength)
  {
    super(maxContentLength);
  }
  
  protected boolean isStartMessage(StompSubframe msg)
    throws Exception
  {
    return msg instanceof StompHeadersSubframe;
  }
  
  protected boolean isContentMessage(StompSubframe msg)
    throws Exception
  {
    return msg instanceof StompContentSubframe;
  }
  
  protected boolean isLastContentMessage(StompContentSubframe msg)
    throws Exception
  {
    return msg instanceof LastStompContentSubframe;
  }
  
  protected boolean isAggregated(StompSubframe msg)
    throws Exception
  {
    return msg instanceof StompFrame;
  }
  
  protected boolean hasContentLength(StompHeadersSubframe start)
    throws Exception
  {
    return start.headers().contains(StompHeaders.CONTENT_LENGTH);
  }
  
  protected long contentLength(StompHeadersSubframe start)
    throws Exception
  {
    return start.headers().getLong(StompHeaders.CONTENT_LENGTH, 0L);
  }
  
  protected Object newContinueResponse(StompHeadersSubframe start)
    throws Exception
  {
    return null;
  }
  
  protected StompFrame beginAggregation(StompHeadersSubframe start, ByteBuf content)
    throws Exception
  {
    StompFrame ret = new DefaultStompFrame(start.command(), content);
    ret.headers().set(start.headers());
    return ret;
  }
}
