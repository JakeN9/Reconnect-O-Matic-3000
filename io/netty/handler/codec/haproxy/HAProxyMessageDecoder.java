package io.netty.handler.codec.haproxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import java.util.List;

public class HAProxyMessageDecoder
  extends ByteToMessageDecoder
{
  private static final int V1_MAX_LENGTH = 108;
  private static final int V2_MAX_LENGTH = 65551;
  private static final int V2_MIN_LENGTH = 232;
  private static final int V2_MAX_TLV = 65319;
  private static final int DELIMITER_LENGTH = 2;
  private static final byte[] BINARY_PREFIX = { 13, 10, 13, 10, 0, 13, 10, 81, 85, 73, 84, 10 };
  private static final int BINARY_PREFIX_LENGTH = BINARY_PREFIX.length;
  private boolean discarding;
  private int discardedBytes;
  private boolean finished;
  private int version = -1;
  private final int v2MaxHeaderSize;
  
  public HAProxyMessageDecoder()
  {
    this.v2MaxHeaderSize = 65551;
  }
  
  public HAProxyMessageDecoder(int maxTlvSize)
  {
    if (maxTlvSize < 1)
    {
      this.v2MaxHeaderSize = 232;
    }
    else if (maxTlvSize > 65319)
    {
      this.v2MaxHeaderSize = 65551;
    }
    else
    {
      int calcMax = maxTlvSize + 232;
      if (calcMax > 65551) {
        this.v2MaxHeaderSize = 65551;
      } else {
        this.v2MaxHeaderSize = calcMax;
      }
    }
  }
  
  private static int findVersion(ByteBuf buffer)
  {
    int n = buffer.readableBytes();
    if (n < 13) {
      return -1;
    }
    int idx = buffer.readerIndex();
    for (int i = 0; i < BINARY_PREFIX_LENGTH; i++)
    {
      byte b = buffer.getByte(idx + i);
      if (b != BINARY_PREFIX[i]) {
        return 1;
      }
    }
    return buffer.getByte(idx + BINARY_PREFIX_LENGTH);
  }
  
  private static int findEndOfHeader(ByteBuf buffer)
  {
    int n = buffer.readableBytes();
    if (n < 16) {
      return -1;
    }
    int offset = buffer.readerIndex() + 14;
    
    int totalHeaderBytes = 16 + buffer.getUnsignedShort(offset);
    if (n >= totalHeaderBytes) {
      return totalHeaderBytes;
    }
    return -1;
  }
  
  private static int findEndOfLine(ByteBuf buffer)
  {
    int n = buffer.writerIndex();
    for (int i = buffer.readerIndex(); i < n; i++)
    {
      byte b = buffer.getByte(i);
      if ((b == 13) && (i < n - 1) && (buffer.getByte(i + 1) == 10)) {
        return i;
      }
    }
    return -1;
  }
  
  public boolean isSingleDecode()
  {
    return true;
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    super.channelRead(ctx, msg);
    if (this.finished) {
      ctx.pipeline().remove(this);
    }
  }
  
  protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    if ((this.version == -1) && 
      ((this.version = findVersion(in)) == -1)) {
      return;
    }
    ByteBuf decoded;
    ByteBuf decoded;
    if (this.version == 1) {
      decoded = decodeLine(ctx, in);
    } else {
      decoded = decodeStruct(ctx, in);
    }
    if (decoded != null)
    {
      this.finished = true;
      try
      {
        if (this.version == 1) {
          out.add(HAProxyMessage.decodeHeader(decoded.toString(CharsetUtil.US_ASCII)));
        } else {
          out.add(HAProxyMessage.decodeHeader(decoded));
        }
      }
      catch (HAProxyProtocolException e)
      {
        fail(ctx, null, e);
      }
    }
  }
  
  private ByteBuf decodeStruct(ChannelHandlerContext ctx, ByteBuf buffer)
    throws Exception
  {
    int eoh = findEndOfHeader(buffer);
    if (!this.discarding)
    {
      if (eoh >= 0)
      {
        int length = eoh - buffer.readerIndex();
        if (length > this.v2MaxHeaderSize)
        {
          buffer.readerIndex(eoh);
          failOverLimit(ctx, length);
          return null;
        }
        return buffer.readSlice(length);
      }
      int length = buffer.readableBytes();
      if (length > this.v2MaxHeaderSize)
      {
        this.discardedBytes = length;
        buffer.skipBytes(length);
        this.discarding = true;
        failOverLimit(ctx, "over " + this.discardedBytes);
      }
      return null;
    }
    if (eoh >= 0)
    {
      buffer.readerIndex(eoh);
      this.discardedBytes = 0;
      this.discarding = false;
    }
    else
    {
      this.discardedBytes = buffer.readableBytes();
      buffer.skipBytes(this.discardedBytes);
    }
    return null;
  }
  
  private ByteBuf decodeLine(ChannelHandlerContext ctx, ByteBuf buffer)
    throws Exception
  {
    int eol = findEndOfLine(buffer);
    if (!this.discarding)
    {
      if (eol >= 0)
      {
        int length = eol - buffer.readerIndex();
        if (length > 108)
        {
          buffer.readerIndex(eol + 2);
          failOverLimit(ctx, length);
          return null;
        }
        ByteBuf frame = buffer.readSlice(length);
        buffer.skipBytes(2);
        return frame;
      }
      int length = buffer.readableBytes();
      if (length > 108)
      {
        this.discardedBytes = length;
        buffer.skipBytes(length);
        this.discarding = true;
        failOverLimit(ctx, "over " + this.discardedBytes);
      }
      return null;
    }
    if (eol >= 0)
    {
      int delimLength = buffer.getByte(eol) == 13 ? 2 : 1;
      buffer.readerIndex(eol + delimLength);
      this.discardedBytes = 0;
      this.discarding = false;
    }
    else
    {
      this.discardedBytes = buffer.readableBytes();
      buffer.skipBytes(this.discardedBytes);
    }
    return null;
  }
  
  private void failOverLimit(ChannelHandlerContext ctx, int length)
  {
    failOverLimit(ctx, String.valueOf(length));
  }
  
  private void failOverLimit(ChannelHandlerContext ctx, String length)
  {
    int maxLength = this.version == 1 ? 108 : this.v2MaxHeaderSize;
    fail(ctx, "header length (" + length + ") exceeds the allowed maximum (" + maxLength + ')', null);
  }
  
  private void fail(ChannelHandlerContext ctx, String errMsg, Throwable t)
  {
    this.finished = true;
    ctx.close();
    HAProxyProtocolException ppex;
    HAProxyProtocolException ppex;
    if ((errMsg != null) && (t != null))
    {
      ppex = new HAProxyProtocolException(errMsg, t);
    }
    else
    {
      HAProxyProtocolException ppex;
      if (errMsg != null)
      {
        ppex = new HAProxyProtocolException(errMsg);
      }
      else
      {
        HAProxyProtocolException ppex;
        if (t != null) {
          ppex = new HAProxyProtocolException(t);
        } else {
          ppex = new HAProxyProtocolException();
        }
      }
    }
    throw ppex;
  }
}
