package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.memcache.AbstractMemcacheObjectAggregator;
import io.netty.handler.codec.memcache.FullMemcacheMessage;
import io.netty.handler.codec.memcache.MemcacheObject;

public class BinaryMemcacheObjectAggregator
  extends AbstractMemcacheObjectAggregator<BinaryMemcacheMessage>
{
  public BinaryMemcacheObjectAggregator(int maxContentLength)
  {
    super(maxContentLength);
  }
  
  protected boolean isStartMessage(MemcacheObject msg)
    throws Exception
  {
    return msg instanceof BinaryMemcacheMessage;
  }
  
  protected FullMemcacheMessage beginAggregation(BinaryMemcacheMessage start, ByteBuf content)
    throws Exception
  {
    if ((start instanceof BinaryMemcacheRequest)) {
      return toFullRequest((BinaryMemcacheRequest)start, content);
    }
    if ((start instanceof BinaryMemcacheResponse)) {
      return toFullResponse((BinaryMemcacheResponse)start, content);
    }
    throw new Error();
  }
  
  private static FullBinaryMemcacheRequest toFullRequest(BinaryMemcacheRequest request, ByteBuf content)
  {
    FullBinaryMemcacheRequest fullRequest = new DefaultFullBinaryMemcacheRequest(request.key(), request.extras(), content);
    
    fullRequest.setMagic(request.magic());
    fullRequest.setOpcode(request.opcode());
    fullRequest.setKeyLength(request.keyLength());
    fullRequest.setExtrasLength(request.extrasLength());
    fullRequest.setDataType(request.dataType());
    fullRequest.setTotalBodyLength(request.totalBodyLength());
    fullRequest.setOpaque(request.opaque());
    fullRequest.setCas(request.cas());
    fullRequest.setReserved(request.reserved());
    
    return fullRequest;
  }
  
  private static FullBinaryMemcacheResponse toFullResponse(BinaryMemcacheResponse response, ByteBuf content)
  {
    FullBinaryMemcacheResponse fullResponse = new DefaultFullBinaryMemcacheResponse(response.key(), response.extras(), content);
    
    fullResponse.setMagic(response.magic());
    fullResponse.setOpcode(response.opcode());
    fullResponse.setKeyLength(response.keyLength());
    fullResponse.setExtrasLength(response.extrasLength());
    fullResponse.setDataType(response.dataType());
    fullResponse.setTotalBodyLength(response.totalBodyLength());
    fullResponse.setOpaque(response.opaque());
    fullResponse.setCas(response.cas());
    fullResponse.setStatus(response.status());
    
    return fullResponse;
  }
}
