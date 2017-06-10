package io.netty.handler.codec;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.TypeParameterMatcher;
import java.util.List;

public abstract class MessageToMessageDecoder<I>
  extends ChannelHandlerAdapter
{
  private final TypeParameterMatcher matcher;
  
  protected MessageToMessageDecoder()
  {
    this.matcher = TypeParameterMatcher.find(this, MessageToMessageDecoder.class, "I");
  }
  
  protected MessageToMessageDecoder(Class<? extends I> inboundMessageType)
  {
    this.matcher = TypeParameterMatcher.get(inboundMessageType);
  }
  
  public boolean acceptInboundMessage(Object msg)
    throws Exception
  {
    return this.matcher.match(msg);
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    RecyclableArrayList out = RecyclableArrayList.newInstance();
    try
    {
      if (acceptInboundMessage(msg))
      {
        I cast = (I)msg;
        try
        {
          decode(ctx, cast, out);
        }
        finally
        {
          ReferenceCountUtil.release(cast);
        }
      }
      else
      {
        out.add(msg);
      }
    }
    catch (DecoderException e)
    {
      int size;
      int i;
      throw e;
    }
    catch (Exception e)
    {
      throw new DecoderException(e);
    }
    finally
    {
      int size = out.size();
      for (int i = 0; i < size; i++) {
        ctx.fireChannelRead(out.get(i));
      }
      out.recycle();
    }
  }
  
  protected abstract void decode(ChannelHandlerContext paramChannelHandlerContext, I paramI, List<Object> paramList)
    throws Exception;
}
