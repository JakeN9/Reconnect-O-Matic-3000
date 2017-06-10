package io.netty.handler.codec.memcache;

import io.netty.handler.codec.MessageAggregator;

public abstract class AbstractMemcacheObjectAggregator<H extends MemcacheMessage>
  extends MessageAggregator<MemcacheObject, H, MemcacheContent, FullMemcacheMessage>
{
  protected AbstractMemcacheObjectAggregator(int maxContentLength)
  {
    super(maxContentLength);
  }
  
  protected boolean isContentMessage(MemcacheObject msg)
    throws Exception
  {
    return msg instanceof MemcacheContent;
  }
  
  protected boolean isLastContentMessage(MemcacheContent msg)
    throws Exception
  {
    return msg instanceof LastMemcacheContent;
  }
  
  protected boolean isAggregated(MemcacheObject msg)
    throws Exception
  {
    return msg instanceof FullMemcacheMessage;
  }
  
  protected boolean hasContentLength(H start)
    throws Exception
  {
    return false;
  }
  
  protected long contentLength(H start)
    throws Exception
  {
    throw new UnsupportedOperationException();
  }
  
  protected Object newContinueResponse(H start)
    throws Exception
  {
    return null;
  }
}
