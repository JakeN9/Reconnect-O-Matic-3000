package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.memcache.AbstractMemcacheObjectEncoder;
import io.netty.util.CharsetUtil;

public abstract class AbstractBinaryMemcacheEncoder<M extends BinaryMemcacheMessage>
  extends AbstractMemcacheObjectEncoder<M>
{
  private static final int DEFAULT_BUFFER_SIZE = 24;
  
  protected ByteBuf encodeMessage(ChannelHandlerContext ctx, M msg)
  {
    ByteBuf buf = ctx.alloc().buffer(24);
    
    encodeHeader(buf, msg);
    encodeExtras(buf, msg.extras());
    encodeKey(buf, msg.key());
    
    return buf;
  }
  
  private static void encodeExtras(ByteBuf buf, ByteBuf extras)
  {
    if ((extras == null) || (!extras.isReadable())) {
      return;
    }
    buf.writeBytes(extras);
  }
  
  private static void encodeKey(ByteBuf buf, String key)
  {
    if ((key == null) || (key.isEmpty())) {
      return;
    }
    buf.writeBytes(key.getBytes(CharsetUtil.UTF_8));
  }
  
  protected abstract void encodeHeader(ByteBuf paramByteBuf, M paramM);
}
