package io.netty.handler.codec.spdy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

public class SpdyFrameDecoder
{
  private final int spdyVersion;
  private final int maxChunkSize;
  private final SpdyFrameDecoderDelegate delegate;
  private State state;
  private byte flags;
  private int length;
  private int streamId;
  private int numSettings;
  
  private static enum State
  {
    READ_COMMON_HEADER,  READ_DATA_FRAME,  READ_SYN_STREAM_FRAME,  READ_SYN_REPLY_FRAME,  READ_RST_STREAM_FRAME,  READ_SETTINGS_FRAME,  READ_SETTING,  READ_PING_FRAME,  READ_GOAWAY_FRAME,  READ_HEADERS_FRAME,  READ_WINDOW_UPDATE_FRAME,  READ_HEADER_BLOCK,  DISCARD_FRAME,  FRAME_ERROR;
    
    private State() {}
  }
  
  public SpdyFrameDecoder(SpdyVersion spdyVersion, SpdyFrameDecoderDelegate delegate)
  {
    this(spdyVersion, delegate, 8192);
  }
  
  public SpdyFrameDecoder(SpdyVersion spdyVersion, SpdyFrameDecoderDelegate delegate, int maxChunkSize)
  {
    if (spdyVersion == null) {
      throw new NullPointerException("spdyVersion");
    }
    if (delegate == null) {
      throw new NullPointerException("delegate");
    }
    if (maxChunkSize <= 0) {
      throw new IllegalArgumentException("maxChunkSize must be a positive integer: " + maxChunkSize);
    }
    this.spdyVersion = spdyVersion.getVersion();
    this.delegate = delegate;
    this.maxChunkSize = maxChunkSize;
    this.state = State.READ_COMMON_HEADER;
  }
  
  public void decode(ByteBuf buffer)
  {
    for (;;)
    {
      boolean last;
      int statusCode;
      switch (this.state)
      {
      case READ_COMMON_HEADER: 
        if (buffer.readableBytes() < 8) {
          return;
        }
        int frameOffset = buffer.readerIndex();
        int flagsOffset = frameOffset + 4;
        int lengthOffset = frameOffset + 5;
        buffer.skipBytes(8);
        
        boolean control = (buffer.getByte(frameOffset) & 0x80) != 0;
        int version;
        int type;
        if (control)
        {
          int version = SpdyCodecUtil.getUnsignedShort(buffer, frameOffset) & 0x7FFF;
          int type = SpdyCodecUtil.getUnsignedShort(buffer, frameOffset + 2);
          this.streamId = 0;
        }
        else
        {
          version = this.spdyVersion;
          type = 0;
          this.streamId = SpdyCodecUtil.getUnsignedInt(buffer, frameOffset);
        }
        this.flags = buffer.getByte(flagsOffset);
        this.length = SpdyCodecUtil.getUnsignedMedium(buffer, lengthOffset);
        if (version != this.spdyVersion)
        {
          this.state = State.FRAME_ERROR;
          this.delegate.readFrameError("Invalid SPDY Version");
        }
        else if (!isValidFrameHeader(this.streamId, type, this.flags, this.length))
        {
          this.state = State.FRAME_ERROR;
          this.delegate.readFrameError("Invalid Frame Error");
        }
        else
        {
          this.state = getNextState(type, this.length);
        }
        break;
      case READ_DATA_FRAME: 
        if (this.length == 0)
        {
          this.state = State.READ_COMMON_HEADER;
          this.delegate.readDataFrame(this.streamId, hasFlag(this.flags, (byte)1), Unpooled.buffer(0));
        }
        else
        {
          int dataLength = Math.min(this.maxChunkSize, this.length);
          if (buffer.readableBytes() < dataLength) {
            return;
          }
          ByteBuf data = buffer.alloc().buffer(dataLength);
          data.writeBytes(buffer, dataLength);
          this.length -= dataLength;
          if (this.length == 0) {
            this.state = State.READ_COMMON_HEADER;
          }
          last = (this.length == 0) && (hasFlag(this.flags, (byte)1));
          
          this.delegate.readDataFrame(this.streamId, last, data);
        }
        break;
      case READ_SYN_STREAM_FRAME: 
        if (buffer.readableBytes() < 10) {
          return;
        }
        int offset = buffer.readerIndex();
        this.streamId = SpdyCodecUtil.getUnsignedInt(buffer, offset);
        int associatedToStreamId = SpdyCodecUtil.getUnsignedInt(buffer, offset + 4);
        byte priority = (byte)(buffer.getByte(offset + 8) >> 5 & 0x7);
        last = hasFlag(this.flags, (byte)1);
        boolean unidirectional = hasFlag(this.flags, (byte)2);
        buffer.skipBytes(10);
        this.length -= 10;
        if (this.streamId == 0)
        {
          this.state = State.FRAME_ERROR;
          this.delegate.readFrameError("Invalid SYN_STREAM Frame");
        }
        else
        {
          this.state = State.READ_HEADER_BLOCK;
          this.delegate.readSynStreamFrame(this.streamId, associatedToStreamId, priority, last, unidirectional);
        }
        break;
      case READ_SYN_REPLY_FRAME: 
        if (buffer.readableBytes() < 4) {
          return;
        }
        this.streamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        last = hasFlag(this.flags, (byte)1);
        
        buffer.skipBytes(4);
        this.length -= 4;
        if (this.streamId == 0)
        {
          this.state = State.FRAME_ERROR;
          this.delegate.readFrameError("Invalid SYN_REPLY Frame");
        }
        else
        {
          this.state = State.READ_HEADER_BLOCK;
          this.delegate.readSynReplyFrame(this.streamId, last);
        }
        break;
      case READ_RST_STREAM_FRAME: 
        if (buffer.readableBytes() < 8) {
          return;
        }
        this.streamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        statusCode = SpdyCodecUtil.getSignedInt(buffer, buffer.readerIndex() + 4);
        buffer.skipBytes(8);
        if ((this.streamId == 0) || (statusCode == 0))
        {
          this.state = State.FRAME_ERROR;
          this.delegate.readFrameError("Invalid RST_STREAM Frame");
        }
        else
        {
          this.state = State.READ_COMMON_HEADER;
          this.delegate.readRstStreamFrame(this.streamId, statusCode);
        }
        break;
      case READ_SETTINGS_FRAME: 
        if (buffer.readableBytes() < 4) {
          return;
        }
        boolean clear = hasFlag(this.flags, (byte)1);
        
        this.numSettings = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        buffer.skipBytes(4);
        this.length -= 4;
        if (((this.length & 0x7) != 0) || (this.length >> 3 != this.numSettings))
        {
          this.state = State.FRAME_ERROR;
          this.delegate.readFrameError("Invalid SETTINGS Frame");
        }
        else
        {
          this.state = State.READ_SETTING;
          this.delegate.readSettingsFrame(clear);
        }
        break;
      case READ_SETTING: 
        if (this.numSettings == 0)
        {
          this.state = State.READ_COMMON_HEADER;
          this.delegate.readSettingsEnd();
        }
        else
        {
          if (buffer.readableBytes() < 8) {
            return;
          }
          byte settingsFlags = buffer.getByte(buffer.readerIndex());
          int id = SpdyCodecUtil.getUnsignedMedium(buffer, buffer.readerIndex() + 1);
          int value = SpdyCodecUtil.getSignedInt(buffer, buffer.readerIndex() + 4);
          boolean persistValue = hasFlag(settingsFlags, (byte)1);
          boolean persisted = hasFlag(settingsFlags, (byte)2);
          buffer.skipBytes(8);
          
          this.numSettings -= 1;
          
          this.delegate.readSetting(id, value, persistValue, persisted);
        }
        break;
      case READ_PING_FRAME: 
        if (buffer.readableBytes() < 4) {
          return;
        }
        int pingId = SpdyCodecUtil.getSignedInt(buffer, buffer.readerIndex());
        buffer.skipBytes(4);
        
        this.state = State.READ_COMMON_HEADER;
        this.delegate.readPingFrame(pingId);
        break;
      case READ_GOAWAY_FRAME: 
        if (buffer.readableBytes() < 8) {
          return;
        }
        int lastGoodStreamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        statusCode = SpdyCodecUtil.getSignedInt(buffer, buffer.readerIndex() + 4);
        buffer.skipBytes(8);
        
        this.state = State.READ_COMMON_HEADER;
        this.delegate.readGoAwayFrame(lastGoodStreamId, statusCode);
        break;
      case READ_HEADERS_FRAME: 
        if (buffer.readableBytes() < 4) {
          return;
        }
        this.streamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        last = hasFlag(this.flags, (byte)1);
        
        buffer.skipBytes(4);
        this.length -= 4;
        if (this.streamId == 0)
        {
          this.state = State.FRAME_ERROR;
          this.delegate.readFrameError("Invalid HEADERS Frame");
        }
        else
        {
          this.state = State.READ_HEADER_BLOCK;
          this.delegate.readHeadersFrame(this.streamId, last);
        }
        break;
      case READ_WINDOW_UPDATE_FRAME: 
        if (buffer.readableBytes() < 8) {
          return;
        }
        this.streamId = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex());
        int deltaWindowSize = SpdyCodecUtil.getUnsignedInt(buffer, buffer.readerIndex() + 4);
        buffer.skipBytes(8);
        if (deltaWindowSize == 0)
        {
          this.state = State.FRAME_ERROR;
          this.delegate.readFrameError("Invalid WINDOW_UPDATE Frame");
        }
        else
        {
          this.state = State.READ_COMMON_HEADER;
          this.delegate.readWindowUpdateFrame(this.streamId, deltaWindowSize);
        }
        break;
      case READ_HEADER_BLOCK: 
        if (this.length == 0)
        {
          this.state = State.READ_COMMON_HEADER;
          this.delegate.readHeaderBlockEnd();
        }
        else
        {
          if (!buffer.isReadable()) {
            return;
          }
          int compressedBytes = Math.min(buffer.readableBytes(), this.length);
          ByteBuf headerBlock = buffer.alloc().buffer(compressedBytes);
          headerBlock.writeBytes(buffer, compressedBytes);
          this.length -= compressedBytes;
          
          this.delegate.readHeaderBlock(headerBlock);
        }
        break;
      case DISCARD_FRAME: 
        int numBytes = Math.min(buffer.readableBytes(), this.length);
        buffer.skipBytes(numBytes);
        this.length -= numBytes;
        if (this.length == 0) {
          this.state = State.READ_COMMON_HEADER;
        } else {
          return;
        }
        break;
      case FRAME_ERROR: 
        buffer.skipBytes(buffer.readableBytes());
        return;
      default: 
        throw new Error("Shouldn't reach here.");
      }
    }
  }
  
  private static boolean hasFlag(byte flags, byte flag)
  {
    return (flags & flag) != 0;
  }
  
  private static State getNextState(int type, int length)
  {
    switch (type)
    {
    case 0: 
      return State.READ_DATA_FRAME;
    case 1: 
      return State.READ_SYN_STREAM_FRAME;
    case 2: 
      return State.READ_SYN_REPLY_FRAME;
    case 3: 
      return State.READ_RST_STREAM_FRAME;
    case 4: 
      return State.READ_SETTINGS_FRAME;
    case 6: 
      return State.READ_PING_FRAME;
    case 7: 
      return State.READ_GOAWAY_FRAME;
    case 8: 
      return State.READ_HEADERS_FRAME;
    case 9: 
      return State.READ_WINDOW_UPDATE_FRAME;
    }
    if (length != 0) {
      return State.DISCARD_FRAME;
    }
    return State.READ_COMMON_HEADER;
  }
  
  private static boolean isValidFrameHeader(int streamId, int type, byte flags, int length)
  {
    switch (type)
    {
    case 0: 
      return streamId != 0;
    case 1: 
      return length >= 10;
    case 2: 
      return length >= 4;
    case 3: 
      return (flags == 0) && (length == 8);
    case 4: 
      return length >= 4;
    case 6: 
      return length == 4;
    case 7: 
      return length == 8;
    case 8: 
      return length >= 4;
    case 9: 
      return length == 8;
    }
    return true;
  }
}
