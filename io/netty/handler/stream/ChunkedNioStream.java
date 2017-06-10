package io.netty.handler.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ChunkedNioStream
  implements ChunkedInput<ByteBuf>
{
  private final ReadableByteChannel in;
  private final int chunkSize;
  private long offset;
  private final ByteBuffer byteBuffer;
  
  public ChunkedNioStream(ReadableByteChannel in)
  {
    this(in, 8192);
  }
  
  public ChunkedNioStream(ReadableByteChannel in, int chunkSize)
  {
    if (in == null) {
      throw new NullPointerException("in");
    }
    if (chunkSize <= 0) {
      throw new IllegalArgumentException("chunkSize: " + chunkSize + " (expected: a positive integer)");
    }
    this.in = in;
    this.offset = 0L;
    this.chunkSize = chunkSize;
    this.byteBuffer = ByteBuffer.allocate(chunkSize);
  }
  
  public long transferredBytes()
  {
    return this.offset;
  }
  
  public boolean isEndOfInput()
    throws Exception
  {
    if (this.byteBuffer.position() > 0) {
      return false;
    }
    if (this.in.isOpen())
    {
      int b = this.in.read(this.byteBuffer);
      if (b < 0) {
        return true;
      }
      this.offset += b;
      return false;
    }
    return true;
  }
  
  public void close()
    throws Exception
  {
    this.in.close();
  }
  
  public ByteBuf readChunk(ChannelHandlerContext ctx)
    throws Exception
  {
    if (isEndOfInput()) {
      return null;
    }
    int readBytes = this.byteBuffer.position();
    for (;;)
    {
      int localReadBytes = this.in.read(this.byteBuffer);
      if (localReadBytes < 0) {
        break;
      }
      readBytes += localReadBytes;
      this.offset += localReadBytes;
      if (readBytes == this.chunkSize) {
        break;
      }
    }
    this.byteBuffer.flip();
    boolean release = true;
    ByteBuf buffer = ctx.alloc().buffer(this.byteBuffer.remaining());
    try
    {
      buffer.writeBytes(this.byteBuffer);
      this.byteBuffer.clear();
      release = false;
      return buffer;
    }
    finally
    {
      if (release) {
        buffer.release();
      }
    }
  }
  
  public long length()
  {
    return -1L;
  }
  
  public long progress()
  {
    return this.offset;
  }
}
