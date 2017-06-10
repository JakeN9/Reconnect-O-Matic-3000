package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.StringUtil;
import java.util.List;

public abstract class ByteToMessageDecoder
  extends ChannelHandlerAdapter
{
  public static final Cumulator MERGE_CUMULATOR = new Cumulator()
  {
    public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in)
    {
      ByteBuf buffer;
      ByteBuf buffer;
      if ((cumulation.writerIndex() > cumulation.maxCapacity() - in.readableBytes()) || (cumulation.refCnt() > 1)) {
        buffer = ByteToMessageDecoder.expandCumulation(alloc, cumulation, in.readableBytes());
      } else {
        buffer = cumulation;
      }
      buffer.writeBytes(in);
      in.release();
      return buffer;
    }
  };
  public static final Cumulator COMPOSITE_CUMULATOR = new Cumulator()
  {
    public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in)
    {
      ByteBuf buffer;
      if (cumulation.refCnt() > 1)
      {
        ByteBuf buffer = ByteToMessageDecoder.expandCumulation(alloc, cumulation, in.readableBytes());
        buffer.writeBytes(in);
        in.release();
      }
      else
      {
        CompositeByteBuf composite;
        CompositeByteBuf composite;
        if ((cumulation instanceof CompositeByteBuf))
        {
          composite = (CompositeByteBuf)cumulation;
        }
        else
        {
          int readable = cumulation.readableBytes();
          composite = alloc.compositeBuffer();
          composite.addComponent(cumulation).writerIndex(readable);
        }
        composite.addComponent(in).writerIndex(composite.writerIndex() + in.readableBytes());
        buffer = composite;
      }
      return buffer;
    }
  };
  ByteBuf cumulation;
  private Cumulator cumulator = MERGE_CUMULATOR;
  private boolean singleDecode;
  private boolean first;
  
  protected ByteToMessageDecoder()
  {
    CodecUtil.ensureNotSharable(this);
  }
  
  public void setSingleDecode(boolean singleDecode)
  {
    this.singleDecode = singleDecode;
  }
  
  public boolean isSingleDecode()
  {
    return this.singleDecode;
  }
  
  public void setCumulator(Cumulator cumulator)
  {
    if (cumulator == null) {
      throw new NullPointerException("cumulator");
    }
    this.cumulator = cumulator;
  }
  
  protected int actualReadableBytes()
  {
    return internalBuffer().readableBytes();
  }
  
  protected ByteBuf internalBuffer()
  {
    if (this.cumulation != null) {
      return this.cumulation;
    }
    return Unpooled.EMPTY_BUFFER;
  }
  
  public final void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {
    ByteBuf buf = internalBuffer();
    int readable = buf.readableBytes();
    if (readable > 0)
    {
      ByteBuf bytes = buf.readBytes(readable);
      buf.release();
      ctx.fireChannelRead(bytes);
      ctx.fireChannelReadComplete();
    }
    else
    {
      buf.release();
    }
    this.cumulation = null;
    handlerRemoved0(ctx);
  }
  
  protected void handlerRemoved0(ChannelHandlerContext ctx)
    throws Exception
  {}
  
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    if ((msg instanceof ByteBuf))
    {
      RecyclableArrayList out = RecyclableArrayList.newInstance();
      try
      {
        ByteBuf data = (ByteBuf)msg;
        this.first = (this.cumulation == null);
        if (this.first) {
          this.cumulation = data;
        } else {
          this.cumulation = this.cumulator.cumulate(ctx.alloc(), this.cumulation, data);
        }
        callDecode(ctx, this.cumulation, out);
      }
      catch (DecoderException e)
      {
        int size;
        int i;
        throw e;
      }
      catch (Throwable t)
      {
        throw new DecoderException(t);
      }
      finally
      {
        if ((this.cumulation != null) && (!this.cumulation.isReadable()))
        {
          this.cumulation.release();
          this.cumulation = null;
        }
        int size = out.size();
        for (int i = 0; i < size; i++) {
          ctx.fireChannelRead(out.get(i));
        }
        out.recycle();
      }
    }
    else
    {
      ctx.fireChannelRead(msg);
    }
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception
  {
    if ((this.cumulation != null) && (!this.first) && (this.cumulation.refCnt() == 1)) {
      this.cumulation.discardSomeReadBytes();
    }
    ctx.fireChannelReadComplete();
  }
  
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    RecyclableArrayList out = RecyclableArrayList.newInstance();
    try
    {
      if (this.cumulation != null)
      {
        callDecode(ctx, this.cumulation, out);
        decodeLast(ctx, this.cumulation, out);
      }
      else
      {
        decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
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
      try
      {
        if (this.cumulation != null)
        {
          this.cumulation.release();
          this.cumulation = null;
        }
        int size = out.size();
        for (int i = 0; i < size; i++) {
          ctx.fireChannelRead(out.get(i));
        }
        if (size > 0) {
          ctx.fireChannelReadComplete();
        }
        ctx.fireChannelInactive();
      }
      finally
      {
        out.recycle();
      }
    }
  }
  
  protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
  {
    try
    {
      while (in.isReadable())
      {
        int outSize = out.size();
        int oldInputLength = in.readableBytes();
        decode(ctx, in, out);
        if (ctx.isRemoved()) {
          break;
        }
        if (outSize == out.size())
        {
          if (oldInputLength == in.readableBytes()) {
            break;
          }
        }
        else
        {
          if (oldInputLength == in.readableBytes()) {
            throw new DecoderException(StringUtil.simpleClassName(getClass()) + ".decode() did not read anything but decoded a message.");
          }
          if (isSingleDecode()) {
            break;
          }
        }
      }
    }
    catch (DecoderException e)
    {
      throw e;
    }
    catch (Throwable cause)
    {
      throw new DecoderException(cause);
    }
  }
  
  protected abstract void decode(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf, List<Object> paramList)
    throws Exception;
  
  protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    decode(ctx, in, out);
  }
  
  static ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf cumulation, int readable)
  {
    ByteBuf oldCumulation = cumulation;
    cumulation = alloc.buffer(oldCumulation.readableBytes() + readable);
    cumulation.writeBytes(oldCumulation);
    oldCumulation.release();
    return cumulation;
  }
  
  public static abstract interface Cumulator
  {
    public abstract ByteBuf cumulate(ByteBufAllocator paramByteBufAllocator, ByteBuf paramByteBuf1, ByteBuf paramByteBuf2);
  }
}
