package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelPromiseNotifier;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.EventExecutor;
import java.util.concurrent.TimeUnit;

public class Bzip2Encoder
  extends MessageToByteEncoder<ByteBuf>
{
  private static enum State
  {
    INIT,  INIT_BLOCK,  WRITE_DATA,  CLOSE_BLOCK;
    
    private State() {}
  }
  
  private State currentState = State.INIT;
  private final Bzip2BitWriter writer = new Bzip2BitWriter();
  private final int streamBlockSize;
  private int streamCRC;
  private Bzip2BlockCompressor blockCompressor;
  private volatile boolean finished;
  private volatile ChannelHandlerContext ctx;
  
  public Bzip2Encoder()
  {
    this(9);
  }
  
  public Bzip2Encoder(int blockSizeMultiplier)
  {
    if ((blockSizeMultiplier < 1) || (blockSizeMultiplier > 9)) {
      throw new IllegalArgumentException("blockSizeMultiplier: " + blockSizeMultiplier + " (expected: 1-9)");
    }
    this.streamBlockSize = (blockSizeMultiplier * 100000);
  }
  
  protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out)
    throws Exception
  {
    if (this.finished) {
      out.writeBytes(in);
    } else {
      for (;;)
      {
        switch (this.currentState)
        {
        case INIT: 
          out.ensureWritable(4);
          out.writeMedium(4348520);
          out.writeByte(48 + this.streamBlockSize / 100000);
          this.currentState = State.INIT_BLOCK;
        case INIT_BLOCK: 
          this.blockCompressor = new Bzip2BlockCompressor(this.writer, this.streamBlockSize);
          this.currentState = State.WRITE_DATA;
        case WRITE_DATA: 
          if (!in.isReadable()) {
            return;
          }
          Bzip2BlockCompressor blockCompressor = this.blockCompressor;
          int length = in.readableBytes() < blockCompressor.availableSize() ? in.readableBytes() : blockCompressor.availableSize();
          int offset;
          byte[] array;
          int offset;
          if (in.hasArray())
          {
            byte[] array = in.array();
            offset = in.arrayOffset() + in.readerIndex();
          }
          else
          {
            array = new byte[length];
            in.getBytes(in.readerIndex(), array);
            offset = 0;
          }
          int bytesWritten = blockCompressor.write(array, offset, length);
          in.skipBytes(bytesWritten);
          if (!blockCompressor.isFull()) {
            if (in.isReadable()) {
              break;
            }
          } else {
            this.currentState = State.CLOSE_BLOCK;
          }
          break;
        case CLOSE_BLOCK: 
          closeBlock(out);
          this.currentState = State.INIT_BLOCK;
          break;
        default: 
          throw new IllegalStateException();
        }
      }
    }
  }
  
  private void closeBlock(ByteBuf out)
  {
    Bzip2BlockCompressor blockCompressor = this.blockCompressor;
    if (!blockCompressor.isEmpty())
    {
      blockCompressor.close(out);
      int blockCRC = blockCompressor.crc();
      this.streamCRC = ((this.streamCRC << 1 | this.streamCRC >>> 31) ^ blockCRC);
    }
  }
  
  public boolean isClosed()
  {
    return this.finished;
  }
  
  public ChannelFuture close()
  {
    return close(ctx().newPromise());
  }
  
  public ChannelFuture close(final ChannelPromise promise)
  {
    ChannelHandlerContext ctx = ctx();
    EventExecutor executor = ctx.executor();
    if (executor.inEventLoop()) {
      return finishEncode(ctx, promise);
    }
    executor.execute(new Runnable()
    {
      public void run()
      {
        ChannelFuture f = Bzip2Encoder.this.finishEncode(Bzip2Encoder.access$000(Bzip2Encoder.this), promise);
        f.addListener(new ChannelPromiseNotifier(new ChannelPromise[] { promise }));
      }
    });
    return promise;
  }
  
  public void close(final ChannelHandlerContext ctx, final ChannelPromise promise)
    throws Exception
  {
    ChannelFuture f = finishEncode(ctx, ctx.newPromise());
    f.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture f)
        throws Exception
      {
        ctx.close(promise);
      }
    });
    if (!f.isDone()) {
      ctx.executor().schedule(new Runnable()
      {
        public void run()
        {
          ctx.close(promise);
        }
      }, 10L, TimeUnit.SECONDS);
    }
  }
  
  private ChannelFuture finishEncode(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    if (this.finished)
    {
      promise.setSuccess();
      return promise;
    }
    this.finished = true;
    
    ByteBuf footer = ctx.alloc().buffer();
    closeBlock(footer);
    
    int streamCRC = this.streamCRC;
    Bzip2BitWriter writer = this.writer;
    try
    {
      writer.writeBits(footer, 24, 1536581L);
      writer.writeBits(footer, 24, 3690640L);
      writer.writeInt(footer, streamCRC);
      writer.flush(footer);
    }
    finally
    {
      this.blockCompressor = null;
    }
    return ctx.writeAndFlush(footer, promise);
  }
  
  private ChannelHandlerContext ctx()
  {
    ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      throw new IllegalStateException("not added to a pipeline");
    }
    return ctx;
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {
    this.ctx = ctx;
  }
}
