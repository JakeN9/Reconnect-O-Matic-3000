package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.zip.Checksum;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.XXHashFactory;

public class Lz4FrameDecoder
  extends ByteToMessageDecoder
{
  private static enum State
  {
    INIT_BLOCK,  DECOMPRESS_DATA,  FINISHED,  CORRUPTED;
    
    private State() {}
  }
  
  private State currentState = State.INIT_BLOCK;
  private LZ4FastDecompressor decompressor;
  private Checksum checksum;
  private int blockType;
  private int compressedLength;
  private int decompressedLength;
  private int currentChecksum;
  
  public Lz4FrameDecoder()
  {
    this(false);
  }
  
  public Lz4FrameDecoder(boolean validateChecksums)
  {
    this(LZ4Factory.fastestInstance(), validateChecksums);
  }
  
  public Lz4FrameDecoder(LZ4Factory factory, boolean validateChecksums)
  {
    this(factory, validateChecksums ? XXHashFactory.fastestInstance().newStreamingHash32(-1756908916).asChecksum() : null);
  }
  
  public Lz4FrameDecoder(LZ4Factory factory, Checksum checksum)
  {
    if (factory == null) {
      throw new NullPointerException("factory");
    }
    this.decompressor = factory.fastDecompressor();
    this.checksum = checksum;
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    try
    {
      int blockType;
      int compressedLength;
      int decompressedLength;
      int currentChecksum;
      switch (this.currentState)
      {
      case INIT_BLOCK: 
        if (in.readableBytes() >= 21)
        {
          long magic = in.readLong();
          if (magic != 5501767354678207339L) {
            throw new DecompressionException("unexpected block identifier");
          }
          int token = in.readByte();
          int compressionLevel = (token & 0xF) + 10;
          blockType = token & 0xF0;
          
          compressedLength = Integer.reverseBytes(in.readInt());
          if ((compressedLength < 0) || (compressedLength > 33554432)) {
            throw new DecompressionException(String.format("invalid compressedLength: %d (expected: 0-%d)", new Object[] { Integer.valueOf(compressedLength), Integer.valueOf(33554432) }));
          }
          decompressedLength = Integer.reverseBytes(in.readInt());
          int maxDecompressedLength = 1 << compressionLevel;
          if ((decompressedLength < 0) || (decompressedLength > maxDecompressedLength)) {
            throw new DecompressionException(String.format("invalid decompressedLength: %d (expected: 0-%d)", new Object[] { Integer.valueOf(decompressedLength), Integer.valueOf(maxDecompressedLength) }));
          }
          if (((decompressedLength == 0) && (compressedLength != 0)) || ((decompressedLength != 0) && (compressedLength == 0)) || ((blockType == 16) && (decompressedLength != compressedLength))) {
            throw new DecompressionException(String.format("stream corrupted: compressedLength(%d) and decompressedLength(%d) mismatch", new Object[] { Integer.valueOf(compressedLength), Integer.valueOf(decompressedLength) }));
          }
          currentChecksum = Integer.reverseBytes(in.readInt());
          if ((decompressedLength == 0) && (compressedLength == 0))
          {
            if (currentChecksum != 0) {
              throw new DecompressionException("stream corrupted: checksum error");
            }
            this.currentState = State.FINISHED;
            this.decompressor = null;
            this.checksum = null;
          }
          else
          {
            this.blockType = blockType;
            this.compressedLength = compressedLength;
            this.decompressedLength = decompressedLength;
            this.currentChecksum = currentChecksum;
            
            this.currentState = State.DECOMPRESS_DATA;
          }
        }
        break;
      case DECOMPRESS_DATA: 
        blockType = this.blockType;
        compressedLength = this.compressedLength;
        decompressedLength = this.decompressedLength;
        currentChecksum = this.currentChecksum;
        if (in.readableBytes() >= compressedLength)
        {
          int idx = in.readerIndex();
          
          ByteBuf uncompressed = ctx.alloc().heapBuffer(decompressedLength, decompressedLength);
          byte[] dest = uncompressed.array();
          int destOff = uncompressed.arrayOffset() + uncompressed.writerIndex();
          
          boolean success = false;
          try
          {
            switch (blockType)
            {
            case 16: 
              in.getBytes(idx, dest, destOff, decompressedLength);
              break;
            case 32: 
              int srcOff;
              byte[] src;
              int srcOff;
              if (in.hasArray())
              {
                byte[] src = in.array();
                srcOff = in.arrayOffset() + idx;
              }
              else
              {
                src = new byte[compressedLength];
                in.getBytes(idx, src);
                srcOff = 0;
              }
              try
              {
                int readBytes = this.decompressor.decompress(src, srcOff, dest, destOff, decompressedLength);
                if (compressedLength != readBytes) {
                  throw new DecompressionException(String.format("stream corrupted: compressedLength(%d) and actual length(%d) mismatch", new Object[] { Integer.valueOf(compressedLength), Integer.valueOf(readBytes) }));
                }
              }
              catch (LZ4Exception e)
              {
                throw new DecompressionException(e);
              }
            default: 
              throw new DecompressionException(String.format("unexpected blockType: %d (expected: %d or %d)", new Object[] { Integer.valueOf(blockType), Integer.valueOf(16), Integer.valueOf(32) }));
            }
            Checksum checksum = this.checksum;
            if (checksum != null)
            {
              checksum.reset();
              checksum.update(dest, destOff, decompressedLength);
              int checksumResult = (int)checksum.getValue();
              if (checksumResult != currentChecksum) {
                throw new DecompressionException(String.format("stream corrupted: mismatching checksum: %d (expected: %d)", new Object[] { Integer.valueOf(checksumResult), Integer.valueOf(currentChecksum) }));
              }
            }
            uncompressed.writerIndex(uncompressed.writerIndex() + decompressedLength);
            out.add(uncompressed);
            in.skipBytes(compressedLength);
            
            this.currentState = State.INIT_BLOCK;
            success = true;
          }
          finally
          {
            if (!success) {
              uncompressed.release();
            }
          }
        }
        break;
      case FINISHED: 
      case CORRUPTED: 
        in.skipBytes(in.readableBytes());
        break;
      default: 
        throw new IllegalStateException();
      }
    }
    catch (Exception e)
    {
      this.currentState = State.CORRUPTED;
      throw e;
    }
  }
  
  public boolean isClosed()
  {
    return this.currentState == State.FINISHED;
  }
}
