package io.netty.channel.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.FileRegion;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public abstract class AbstractNioByteChannel
  extends AbstractNioChannel
{
  private static final String EXPECTED_TYPES = " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ", " + StringUtil.simpleClassName(FileRegion.class) + ')';
  private Runnable flushTask;
  
  protected AbstractNioByteChannel(Channel parent, SelectableChannel ch)
  {
    super(parent, ch, 1);
  }
  
  protected AbstractNioChannel.AbstractNioUnsafe newUnsafe()
  {
    return new NioByteUnsafe();
  }
  
  protected class NioByteUnsafe
    extends AbstractNioChannel.AbstractNioUnsafe
  {
    protected NioByteUnsafe()
    {
      super();
    }
    
    private void closeOnRead(ChannelPipeline pipeline)
    {
      SelectionKey key = AbstractNioByteChannel.this.selectionKey();
      AbstractNioByteChannel.this.setInputShutdown();
      if (AbstractNioByteChannel.this.isOpen()) {
        if (Boolean.TRUE.equals(AbstractNioByteChannel.this.config().getOption(ChannelOption.ALLOW_HALF_CLOSURE)))
        {
          key.interestOps(key.interestOps() & (AbstractNioByteChannel.this.readInterestOp ^ 0xFFFFFFFF));
          pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
        }
        else
        {
          close(voidPromise());
        }
      }
    }
    
    private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close)
    {
      if (byteBuf != null) {
        if (byteBuf.isReadable())
        {
          AbstractNioByteChannel.this.setReadPending(false);
          pipeline.fireChannelRead(byteBuf);
        }
        else
        {
          byteBuf.release();
        }
      }
      pipeline.fireChannelReadComplete();
      pipeline.fireExceptionCaught(cause);
      if ((close) || ((cause instanceof IOException))) {
        closeOnRead(pipeline);
      }
    }
    
    public final void read()
    {
      ChannelConfig config = AbstractNioByteChannel.this.config();
      if ((!config.isAutoRead()) && (!AbstractNioByteChannel.this.isReadPending()))
      {
        removeReadOp();
        return;
      }
      ChannelPipeline pipeline = AbstractNioByteChannel.this.pipeline();
      ByteBufAllocator allocator = config.getAllocator();
      int maxMessagesPerRead = config.getMaxMessagesPerRead();
      RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();
      
      ByteBuf byteBuf = null;
      int messages = 0;
      boolean close = false;
      try
      {
        int totalReadAmount = 0;
        boolean readPendingReset = false;
        do
        {
          byteBuf = allocHandle.allocate(allocator);
          int writable = byteBuf.writableBytes();
          int localReadAmount = AbstractNioByteChannel.this.doReadBytes(byteBuf);
          if (localReadAmount <= 0)
          {
            byteBuf.release();
            byteBuf = null;
            close = localReadAmount < 0;
            break;
          }
          if (!readPendingReset)
          {
            readPendingReset = true;
            AbstractNioByteChannel.this.setReadPending(false);
          }
          pipeline.fireChannelRead(byteBuf);
          byteBuf = null;
          if (totalReadAmount >= Integer.MAX_VALUE - localReadAmount)
          {
            totalReadAmount = Integer.MAX_VALUE;
            break;
          }
          totalReadAmount += localReadAmount;
          if (!config.isAutoRead()) {
            break;
          }
          if (localReadAmount < writable) {
            break;
          }
          messages++;
        } while (messages < maxMessagesPerRead);
        pipeline.fireChannelReadComplete();
        allocHandle.record(totalReadAmount);
        if (close)
        {
          closeOnRead(pipeline);
          close = false;
        }
      }
      catch (Throwable t)
      {
        handleReadException(pipeline, byteBuf, t, close);
      }
      finally
      {
        if ((!config.isAutoRead()) && (!AbstractNioByteChannel.this.isReadPending())) {
          removeReadOp();
        }
      }
    }
  }
  
  protected void doWrite(ChannelOutboundBuffer in)
    throws Exception
  {
    int writeSpinCount = -1;
    for (;;)
    {
      Object msg = in.current();
      if (msg == null)
      {
        clearOpWrite();
        break;
      }
      if ((msg instanceof ByteBuf))
      {
        ByteBuf buf = (ByteBuf)msg;
        int readableBytes = buf.readableBytes();
        if (readableBytes == 0)
        {
          in.remove();
        }
        else
        {
          boolean setOpWrite = false;
          boolean done = false;
          long flushedAmount = 0L;
          if (writeSpinCount == -1) {
            writeSpinCount = config().getWriteSpinCount();
          }
          for (int i = writeSpinCount - 1; i >= 0; i--)
          {
            int localFlushedAmount = doWriteBytes(buf);
            if (localFlushedAmount == 0)
            {
              setOpWrite = true;
              break;
            }
            flushedAmount += localFlushedAmount;
            if (!buf.isReadable())
            {
              done = true;
              break;
            }
          }
          in.progress(flushedAmount);
          if (done)
          {
            in.remove();
          }
          else
          {
            incompleteWrite(setOpWrite);
            break;
          }
        }
      }
      else if ((msg instanceof FileRegion))
      {
        FileRegion region = (FileRegion)msg;
        boolean done = region.transfered() >= region.count();
        boolean setOpWrite = false;
        if (!done)
        {
          long flushedAmount = 0L;
          if (writeSpinCount == -1) {
            writeSpinCount = config().getWriteSpinCount();
          }
          for (int i = writeSpinCount - 1; i >= 0; i--)
          {
            long localFlushedAmount = doWriteFileRegion(region);
            if (localFlushedAmount == 0L)
            {
              setOpWrite = true;
              break;
            }
            flushedAmount += localFlushedAmount;
            if (region.transfered() >= region.count())
            {
              done = true;
              break;
            }
          }
          in.progress(flushedAmount);
        }
        if (done)
        {
          in.remove();
        }
        else
        {
          incompleteWrite(setOpWrite);
          break;
        }
      }
      else
      {
        throw new Error();
      }
    }
  }
  
  protected final Object filterOutboundMessage(Object msg)
  {
    if ((msg instanceof ByteBuf))
    {
      ByteBuf buf = (ByteBuf)msg;
      if (buf.isDirect()) {
        return msg;
      }
      return newDirectBuffer(buf);
    }
    if ((msg instanceof FileRegion)) {
      return msg;
    }
    throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
  }
  
  protected final void incompleteWrite(boolean setOpWrite)
  {
    if (setOpWrite)
    {
      setOpWrite();
    }
    else
    {
      Runnable flushTask = this.flushTask;
      if (flushTask == null) {
        flushTask = this. = new Runnable()
        {
          public void run()
          {
            AbstractNioByteChannel.this.flush();
          }
        };
      }
      eventLoop().execute(flushTask);
    }
  }
  
  protected abstract long doWriteFileRegion(FileRegion paramFileRegion)
    throws Exception;
  
  protected abstract int doReadBytes(ByteBuf paramByteBuf)
    throws Exception;
  
  protected abstract int doWriteBytes(ByteBuf paramByteBuf)
    throws Exception;
  
  protected final void setOpWrite()
  {
    SelectionKey key = selectionKey();
    if (!key.isValid()) {
      return;
    }
    int interestOps = key.interestOps();
    if ((interestOps & 0x4) == 0) {
      key.interestOps(interestOps | 0x4);
    }
  }
  
  protected final void clearOpWrite()
  {
    SelectionKey key = selectionKey();
    if (!key.isValid()) {
      return;
    }
    int interestOps = key.interestOps();
    if ((interestOps & 0x4) != 0) {
      key.interestOps(interestOps & 0xFFFFFFFB);
    }
  }
}
