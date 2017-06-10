package io.netty.channel.epoll;

import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.util.concurrent.FastThreadLocal;

final class IovArrayThreadLocal
{
  private static final FastThreadLocal<IovArray> ARRAY = new FastThreadLocal()
  {
    protected IovArray initialValue()
      throws Exception
    {
      return new IovArray();
    }
    
    protected void onRemoval(IovArray value)
      throws Exception
    {
      value.release();
    }
  };
  
  static IovArray get(ChannelOutboundBuffer buffer)
    throws Exception
  {
    IovArray array = (IovArray)ARRAY.get();
    array.clear();
    buffer.forEachFlushedMessage(array);
    return array;
  }
  
  static IovArray get(CompositeByteBuf buf)
    throws Exception
  {
    IovArray array = (IovArray)ARRAY.get();
    array.clear();
    array.add(buf);
    return array;
  }
}
