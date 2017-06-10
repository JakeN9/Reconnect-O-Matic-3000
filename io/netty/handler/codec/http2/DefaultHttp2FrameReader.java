package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

public class DefaultHttp2FrameReader
  implements Http2FrameReader, Http2FrameSizePolicy, Http2FrameReader.Configuration
{
  private final Http2HeadersDecoder headersDecoder;
  
  private static enum State
  {
    FRAME_HEADER,  FRAME_PAYLOAD,  ERROR;
    
    private State() {}
  }
  
  private State state = State.FRAME_HEADER;
  private byte frameType;
  private int streamId;
  private Http2Flags flags;
  private int payloadLength;
  private HeadersContinuation headersContinuation;
  private int maxFrameSize;
  
  public DefaultHttp2FrameReader()
  {
    this(new DefaultHttp2HeadersDecoder());
  }
  
  public DefaultHttp2FrameReader(Http2HeadersDecoder headersDecoder)
  {
    this.headersDecoder = headersDecoder;
    this.maxFrameSize = 16384;
  }
  
  public Http2HeaderTable headerTable()
  {
    return this.headersDecoder.configuration().headerTable();
  }
  
  public Http2FrameReader.Configuration configuration()
  {
    return this;
  }
  
  public Http2FrameSizePolicy frameSizePolicy()
  {
    return this;
  }
  
  public void maxFrameSize(int max)
    throws Http2Exception
  {
    if (!Http2CodecUtil.isMaxFrameSizeValid(max)) {
      throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Invalid MAX_FRAME_SIZE specified in sent settings: %d", new Object[] { Integer.valueOf(max) });
    }
    this.maxFrameSize = max;
  }
  
  public int maxFrameSize()
  {
    return this.maxFrameSize;
  }
  
  public void close()
  {
    if (this.headersContinuation != null) {
      this.headersContinuation.close();
    }
  }
  
  public void readFrame(ChannelHandlerContext ctx, ByteBuf input, Http2FrameListener listener)
    throws Http2Exception
  {
    try
    {
      while (input.isReadable()) {
        switch (this.state)
        {
        case FRAME_HEADER: 
          processHeaderState(input);
          if (this.state == State.FRAME_HEADER) {
            return;
          }
        case FRAME_PAYLOAD: 
          processPayloadState(ctx, input, listener);
          if (this.state == State.FRAME_PAYLOAD) {
            return;
          }
          break;
        case ERROR: 
          input.skipBytes(input.readableBytes());
          return;
        default: 
          throw new IllegalStateException("Should never get here");
        }
      }
    }
    catch (Http2Exception e)
    {
      this.state = State.ERROR;
      throw e;
    }
    catch (RuntimeException e)
    {
      this.state = State.ERROR;
      throw e;
    }
    catch (Error e)
    {
      this.state = State.ERROR;
      throw e;
    }
  }
  
  private void processHeaderState(ByteBuf in)
    throws Http2Exception
  {
    if (in.readableBytes() < 9) {
      return;
    }
    this.payloadLength = in.readUnsignedMedium();
    if (this.payloadLength > this.maxFrameSize) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Frame length: %d exceeds maximum: %d", new Object[] { Integer.valueOf(this.payloadLength), Integer.valueOf(this.maxFrameSize) });
    }
    this.frameType = in.readByte();
    this.flags = new Http2Flags(in.readUnsignedByte());
    this.streamId = Http2CodecUtil.readUnsignedInt(in);
    switch (this.frameType)
    {
    case 0: 
      verifyDataFrame();
      break;
    case 1: 
      verifyHeadersFrame();
      break;
    case 2: 
      verifyPriorityFrame();
      break;
    case 3: 
      verifyRstStreamFrame();
      break;
    case 4: 
      verifySettingsFrame();
      break;
    case 5: 
      verifyPushPromiseFrame();
      break;
    case 6: 
      verifyPingFrame();
      break;
    case 7: 
      verifyGoAwayFrame();
      break;
    case 8: 
      verifyWindowUpdateFrame();
      break;
    case 9: 
      verifyContinuationFrame();
      break;
    }
    this.state = State.FRAME_PAYLOAD;
  }
  
  private void processPayloadState(ChannelHandlerContext ctx, ByteBuf in, Http2FrameListener listener)
    throws Http2Exception
  {
    if (in.readableBytes() < this.payloadLength) {
      return;
    }
    ByteBuf payload = in.readSlice(this.payloadLength);
    switch (this.frameType)
    {
    case 0: 
      readDataFrame(ctx, payload, listener);
      break;
    case 1: 
      readHeadersFrame(ctx, payload, listener);
      break;
    case 2: 
      readPriorityFrame(ctx, payload, listener);
      break;
    case 3: 
      readRstStreamFrame(ctx, payload, listener);
      break;
    case 4: 
      readSettingsFrame(ctx, payload, listener);
      break;
    case 5: 
      readPushPromiseFrame(ctx, payload, listener);
      break;
    case 6: 
      readPingFrame(ctx, payload, listener);
      break;
    case 7: 
      readGoAwayFrame(ctx, payload, listener);
      break;
    case 8: 
      readWindowUpdateFrame(ctx, payload, listener);
      break;
    case 9: 
      readContinuationFrame(payload, listener);
      break;
    default: 
      readUnknownFrame(ctx, payload, listener);
    }
    this.state = State.FRAME_HEADER;
  }
  
  private void verifyDataFrame()
    throws Http2Exception
  {
    verifyNotProcessingHeaders();
    verifyPayloadLength(this.payloadLength);
    if (this.payloadLength < this.flags.getPaddingPresenceFieldLength()) {
      throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame length %d too small.", new Object[] { Integer.valueOf(this.payloadLength) });
    }
  }
  
  private void verifyHeadersFrame()
    throws Http2Exception
  {
    verifyNotProcessingHeaders();
    verifyPayloadLength(this.payloadLength);
    
    int requiredLength = this.flags.getPaddingPresenceFieldLength() + this.flags.getNumPriorityBytes();
    if (this.payloadLength < requiredLength) {
      throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame length too small." + this.payloadLength, new Object[0]);
    }
  }
  
  private void verifyPriorityFrame()
    throws Http2Exception
  {
    verifyNotProcessingHeaders();
    if (this.payloadLength != 5) {
      throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Invalid frame length %d.", new Object[] { Integer.valueOf(this.payloadLength) });
    }
  }
  
  private void verifyRstStreamFrame()
    throws Http2Exception
  {
    verifyNotProcessingHeaders();
    if (this.payloadLength != 4) {
      throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Invalid frame length %d.", new Object[] { Integer.valueOf(this.payloadLength) });
    }
  }
  
  private void verifySettingsFrame()
    throws Http2Exception
  {
    verifyNotProcessingHeaders();
    verifyPayloadLength(this.payloadLength);
    if (this.streamId != 0) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "A stream ID must be zero.", new Object[0]);
    }
    if ((this.flags.ack()) && (this.payloadLength > 0)) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Ack settings frame must have an empty payload.", new Object[0]);
    }
    if (this.payloadLength % 6 > 0) {
      throw Http2Exception.connectionError(Http2Error.FRAME_SIZE_ERROR, "Frame length %d invalid.", new Object[] { Integer.valueOf(this.payloadLength) });
    }
  }
  
  private void verifyPushPromiseFrame()
    throws Http2Exception
  {
    verifyNotProcessingHeaders();
    verifyPayloadLength(this.payloadLength);
    
    int minLength = this.flags.getPaddingPresenceFieldLength() + 4;
    if (this.payloadLength < minLength) {
      throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame length %d too small.", new Object[] { Integer.valueOf(this.payloadLength) });
    }
  }
  
  private void verifyPingFrame()
    throws Http2Exception
  {
    verifyNotProcessingHeaders();
    if (this.streamId != 0) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "A stream ID must be zero.", new Object[0]);
    }
    if (this.payloadLength != 8) {
      throw Http2Exception.connectionError(Http2Error.FRAME_SIZE_ERROR, "Frame length %d incorrect size for ping.", new Object[] { Integer.valueOf(this.payloadLength) });
    }
  }
  
  private void verifyGoAwayFrame()
    throws Http2Exception
  {
    verifyNotProcessingHeaders();
    verifyPayloadLength(this.payloadLength);
    if (this.streamId != 0) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "A stream ID must be zero.", new Object[0]);
    }
    if (this.payloadLength < 8) {
      throw Http2Exception.connectionError(Http2Error.FRAME_SIZE_ERROR, "Frame length %d too small.", new Object[] { Integer.valueOf(this.payloadLength) });
    }
  }
  
  private void verifyWindowUpdateFrame()
    throws Http2Exception
  {
    verifyNotProcessingHeaders();
    verifyStreamOrConnectionId(this.streamId, "Stream ID");
    if (this.payloadLength != 4) {
      throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Invalid frame length %d.", new Object[] { Integer.valueOf(this.payloadLength) });
    }
  }
  
  private void verifyContinuationFrame()
    throws Http2Exception
  {
    verifyPayloadLength(this.payloadLength);
    if (this.headersContinuation == null) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Received %s frame but not currently processing headers.", new Object[] { Byte.valueOf(this.frameType) });
    }
    if (this.streamId != this.headersContinuation.getStreamId()) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Continuation stream ID does not match pending headers. Expected %d, but received %d.", new Object[] { Integer.valueOf(this.headersContinuation.getStreamId()), Integer.valueOf(this.streamId) });
    }
    if (this.payloadLength < this.flags.getPaddingPresenceFieldLength()) {
      throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame length %d too small for padding.", new Object[] { Integer.valueOf(this.payloadLength) });
    }
  }
  
  private void readDataFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    short padding = readPadding(payload);
    
    int dataLength = payload.readableBytes() - padding;
    if (dataLength < 0) {
      throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame payload too small for padding.", new Object[0]);
    }
    ByteBuf data = payload.readSlice(dataLength);
    listener.onDataRead(ctx, this.streamId, data, padding, this.flags.endOfStream());
    payload.skipBytes(payload.readableBytes());
  }
  
  private void readHeadersFrame(final ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    final int headersStreamId = this.streamId;
    final Http2Flags headersFlags = this.flags;
    final int padding = readPadding(payload);
    if (this.flags.priorityPresent())
    {
      long word1 = payload.readUnsignedInt();
      final boolean exclusive = (word1 & 0x80000000) != 0L;
      final int streamDependency = (int)(word1 & 0x7FFFFFFF);
      final short weight = (short)(payload.readUnsignedByte() + 1);
      ByteBuf fragment = payload.readSlice(payload.readableBytes() - padding);
      
      this.headersContinuation = new HeadersContinuation(headersStreamId, ctx)
      {
        public int getStreamId()
        {
          return headersStreamId;
        }
        
        public void processFragment(boolean endOfHeaders, ByteBuf fragment, Http2FrameListener listener)
          throws Http2Exception
        {
          DefaultHttp2FrameReader.HeadersBlockBuilder hdrBlockBuilder = headersBlockBuilder();
          hdrBlockBuilder.addFragment(fragment, ctx.alloc(), endOfHeaders);
          if (endOfHeaders)
          {
            listener.onHeadersRead(ctx, headersStreamId, hdrBlockBuilder.headers(), streamDependency, weight, exclusive, padding, headersFlags.endOfStream());
            
            close();
          }
        }
      };
      this.headersContinuation.processFragment(this.flags.endOfHeaders(), fragment, listener);
      return;
    }
    this.headersContinuation = new HeadersContinuation(headersStreamId, ctx)
    {
      public int getStreamId()
      {
        return headersStreamId;
      }
      
      public void processFragment(boolean endOfHeaders, ByteBuf fragment, Http2FrameListener listener)
        throws Http2Exception
      {
        DefaultHttp2FrameReader.HeadersBlockBuilder hdrBlockBuilder = headersBlockBuilder();
        hdrBlockBuilder.addFragment(fragment, ctx.alloc(), endOfHeaders);
        if (endOfHeaders)
        {
          listener.onHeadersRead(ctx, headersStreamId, hdrBlockBuilder.headers(), padding, headersFlags.endOfStream());
          
          close();
        }
      }
    };
    ByteBuf fragment = payload.readSlice(payload.readableBytes() - padding);
    this.headersContinuation.processFragment(this.flags.endOfHeaders(), fragment, listener);
  }
  
  private void readPriorityFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    long word1 = payload.readUnsignedInt();
    boolean exclusive = (word1 & 0x80000000) != 0L;
    int streamDependency = (int)(word1 & 0x7FFFFFFF);
    short weight = (short)(payload.readUnsignedByte() + 1);
    listener.onPriorityRead(ctx, this.streamId, streamDependency, weight, exclusive);
  }
  
  private void readRstStreamFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    long errorCode = payload.readUnsignedInt();
    listener.onRstStreamRead(ctx, this.streamId, errorCode);
  }
  
  private void readSettingsFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    if (this.flags.ack())
    {
      listener.onSettingsAckRead(ctx);
    }
    else
    {
      int numSettings = this.payloadLength / 6;
      Http2Settings settings = new Http2Settings();
      for (int index = 0; index < numSettings; index++)
      {
        int id = payload.readUnsignedShort();
        long value = payload.readUnsignedInt();
        try
        {
          settings.put(id, Long.valueOf(value));
        }
        catch (IllegalArgumentException e)
        {
          switch (id)
          {
          case 5: 
            throw Http2Exception.connectionError(Http2Error.FRAME_SIZE_ERROR, e, e.getMessage(), new Object[0]);
          }
        }
        throw Http2Exception.connectionError(Http2Error.FLOW_CONTROL_ERROR, e, e.getMessage(), new Object[0]);
        
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, e, e.getMessage(), new Object[0]);
      }
      listener.onSettingsRead(ctx, settings);
    }
  }
  
  private void readPushPromiseFrame(final ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    final int pushPromiseStreamId = this.streamId;
    final int padding = readPadding(payload);
    final int promisedStreamId = Http2CodecUtil.readUnsignedInt(payload);
    
    this.headersContinuation = new HeadersContinuation(pushPromiseStreamId, ctx)
    {
      public int getStreamId()
      {
        return pushPromiseStreamId;
      }
      
      public void processFragment(boolean endOfHeaders, ByteBuf fragment, Http2FrameListener listener)
        throws Http2Exception
      {
        headersBlockBuilder().addFragment(fragment, ctx.alloc(), endOfHeaders);
        if (endOfHeaders)
        {
          Http2Headers headers = headersBlockBuilder().headers();
          listener.onPushPromiseRead(ctx, pushPromiseStreamId, promisedStreamId, headers, padding);
          
          close();
        }
      }
    };
    ByteBuf fragment = payload.readSlice(payload.readableBytes() - padding);
    this.headersContinuation.processFragment(this.flags.endOfHeaders(), fragment, listener);
  }
  
  private void readPingFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    ByteBuf data = payload.readSlice(payload.readableBytes());
    if (this.flags.ack()) {
      listener.onPingAckRead(ctx, data);
    } else {
      listener.onPingRead(ctx, data);
    }
  }
  
  private static void readGoAwayFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    int lastStreamId = Http2CodecUtil.readUnsignedInt(payload);
    long errorCode = payload.readUnsignedInt();
    ByteBuf debugData = payload.readSlice(payload.readableBytes());
    listener.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
  }
  
  private void readWindowUpdateFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    int windowSizeIncrement = Http2CodecUtil.readUnsignedInt(payload);
    if (windowSizeIncrement == 0) {
      throw Http2Exception.streamError(this.streamId, Http2Error.PROTOCOL_ERROR, "Received WINDOW_UPDATE with delta 0 for stream: %d", new Object[] { Integer.valueOf(this.streamId) });
    }
    listener.onWindowUpdateRead(ctx, this.streamId, windowSizeIncrement);
  }
  
  private void readContinuationFrame(ByteBuf payload, Http2FrameListener listener)
    throws Http2Exception
  {
    ByteBuf continuationFragment = payload.readSlice(payload.readableBytes());
    this.headersContinuation.processFragment(this.flags.endOfHeaders(), continuationFragment, listener);
  }
  
  private void readUnknownFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener)
  {
    payload = payload.readSlice(payload.readableBytes());
    listener.onUnknownFrame(ctx, this.frameType, this.streamId, this.flags, payload);
  }
  
  private short readPadding(ByteBuf payload)
  {
    if (!this.flags.paddingPresent()) {
      return 0;
    }
    return payload.readUnsignedByte();
  }
  
  private abstract class HeadersContinuation
  {
    private final DefaultHttp2FrameReader.HeadersBlockBuilder builder = new DefaultHttp2FrameReader.HeadersBlockBuilder(DefaultHttp2FrameReader.this);
    
    private HeadersContinuation() {}
    
    abstract int getStreamId();
    
    abstract void processFragment(boolean paramBoolean, ByteBuf paramByteBuf, Http2FrameListener paramHttp2FrameListener)
      throws Http2Exception;
    
    final DefaultHttp2FrameReader.HeadersBlockBuilder headersBlockBuilder()
    {
      return this.builder;
    }
    
    final void close()
    {
      this.builder.close();
    }
  }
  
  protected class HeadersBlockBuilder
  {
    private ByteBuf headerBlock;
    
    protected HeadersBlockBuilder() {}
    
    final void addFragment(ByteBuf fragment, ByteBufAllocator alloc, boolean endOfHeaders)
    {
      if (this.headerBlock == null)
      {
        if (endOfHeaders)
        {
          this.headerBlock = fragment.retain();
        }
        else
        {
          this.headerBlock = alloc.buffer(fragment.readableBytes());
          this.headerBlock.writeBytes(fragment);
        }
        return;
      }
      if (this.headerBlock.isWritable(fragment.readableBytes()))
      {
        this.headerBlock.writeBytes(fragment);
      }
      else
      {
        ByteBuf buf = alloc.buffer(this.headerBlock.readableBytes() + fragment.readableBytes());
        buf.writeBytes(this.headerBlock);
        buf.writeBytes(fragment);
        this.headerBlock.release();
        this.headerBlock = buf;
      }
    }
    
    Http2Headers headers()
      throws Http2Exception
    {
      try
      {
        return DefaultHttp2FrameReader.this.headersDecoder.decodeHeaders(this.headerBlock);
      }
      finally
      {
        close();
      }
    }
    
    void close()
    {
      if (this.headerBlock != null)
      {
        this.headerBlock.release();
        this.headerBlock = null;
      }
      DefaultHttp2FrameReader.this.headersContinuation = null;
    }
  }
  
  private void verifyNotProcessingHeaders()
    throws Http2Exception
  {
    if (this.headersContinuation != null) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Received frame of type %s while processing headers.", new Object[] { Byte.valueOf(this.frameType) });
    }
  }
  
  private void verifyPayloadLength(int payloadLength)
    throws Http2Exception
  {
    if (payloadLength > this.maxFrameSize) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Total payload length %d exceeds max frame length.", new Object[] { Integer.valueOf(payloadLength) });
    }
  }
  
  private static void verifyStreamOrConnectionId(int streamId, String argumentName)
    throws Http2Exception
  {
    if (streamId < 0) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "%s must be >= 0", new Object[] { argumentName });
    }
  }
}
