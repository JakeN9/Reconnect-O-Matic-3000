package io.netty.handler.codec.compression;

import com.ning.compress.BufferRecycler;
import com.ning.compress.lzf.ChunkDecoder;
import com.ning.compress.lzf.util.ChunkDecoderFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class LzfDecoder
  extends ByteToMessageDecoder
{
  private static enum State
  {
    INIT_BLOCK,  INIT_ORIGINAL_LENGTH,  DECOMPRESS_DATA,  CORRUPTED;
    
    private State() {}
  }
  
  private State currentState = State.INIT_BLOCK;
  private static final short MAGIC_NUMBER = 23126;
  private ChunkDecoder decoder;
  private BufferRecycler recycler;
  private int chunkLength;
  private int originalLength;
  private boolean isCompressed;
  
  public LzfDecoder()
  {
    this(false);
  }
  
  public LzfDecoder(boolean safeInstance)
  {
    this.decoder = (safeInstance ? ChunkDecoderFactory.safeInstance() : ChunkDecoderFactory.optimalInstance());
    
    this.recycler = BufferRecycler.instance();
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    try
    {
      switch (this.currentState)
      {
      case INIT_BLOCK: 
        if (in.readableBytes() >= 5)
        {
          int magic = in.readUnsignedShort();
          if (magic != 23126) {
            throw new DecompressionException("unexpected block identifier");
          }
          int type = in.readByte();
          switch (type)
          {
          case 0: 
            this.isCompressed = false;
            this.currentState = State.DECOMPRESS_DATA;
            break;
          case 1: 
            this.isCompressed = true;
            this.currentState = State.INIT_ORIGINAL_LENGTH;
            break;
          default: 
            throw new DecompressionException(String.format("unknown type of chunk: %d (expected: %d or %d)", new Object[] { Integer.valueOf(type), Integer.valueOf(0), Integer.valueOf(1) }));
          }
          this.chunkLength = in.readUnsignedShort();
          if (type != 1) {
            break;
          }
        }
        break;
      case INIT_ORIGINAL_LENGTH: 
        if (in.readableBytes() >= 2)
        {
          this.originalLength = in.readUnsignedShort();
          
          this.currentState = State.DECOMPRESS_DATA;
        }
        break;
      case DECOMPRESS_DATA: 
        int chunkLength = this.chunkLength;
        if (in.readableBytes() >= chunkLength)
        {
          int originalLength = this.originalLength;
          if (this.isCompressed)
          {
            int idx = in.readerIndex();
            int inPos;
            byte[] inputArray;
            int inPos;
            if (in.hasArray())
            {
              byte[] inputArray = in.array();
              inPos = in.arrayOffset() + idx;
            }
            else
            {
              inputArray = this.recycler.allocInputBuffer(chunkLength);
              in.getBytes(idx, inputArray, 0, chunkLength);
              inPos = 0;
            }
            ByteBuf uncompressed = ctx.alloc().heapBuffer(originalLength, originalLength);
            byte[] outputArray = uncompressed.array();
            int outPos = uncompressed.arrayOffset() + uncompressed.writerIndex();
            
            boolean success = false;
            try
            {
              this.decoder.decodeChunk(inputArray, inPos, outputArray, outPos, outPos + originalLength);
              uncompressed.writerIndex(uncompressed.writerIndex() + originalLength);
              out.add(uncompressed);
              in.skipBytes(chunkLength);
              success = true;
            }
            finally
            {
              if (!success) {
                uncompressed.release();
              }
            }
            if (!in.hasArray()) {
              this.recycler.releaseInputBuffer(inputArray);
            }
          }
          else if (chunkLength > 0)
          {
            out.add(in.readSlice(chunkLength).retain());
          }
          this.currentState = State.INIT_BLOCK;
        }
        break;
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
      this.decoder = null;
      this.recycler = null;
      throw e;
    }
  }
}
