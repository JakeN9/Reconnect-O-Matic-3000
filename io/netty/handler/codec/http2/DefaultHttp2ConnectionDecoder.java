package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import java.util.List;

public class DefaultHttp2ConnectionDecoder
  implements Http2ConnectionDecoder
{
  private final Http2FrameListener internalFrameListener = new FrameReadListener(null);
  private final Http2Connection connection;
  private final Http2LifecycleManager lifecycleManager;
  private final Http2ConnectionEncoder encoder;
  private final Http2FrameReader frameReader;
  private final Http2FrameListener listener;
  private boolean prefaceReceived;
  
  public static class Builder
    implements Http2ConnectionDecoder.Builder
  {
    private Http2Connection connection;
    private Http2LifecycleManager lifecycleManager;
    private Http2ConnectionEncoder encoder;
    private Http2FrameReader frameReader;
    private Http2FrameListener listener;
    
    public Builder connection(Http2Connection connection)
    {
      this.connection = connection;
      return this;
    }
    
    public Builder lifecycleManager(Http2LifecycleManager lifecycleManager)
    {
      this.lifecycleManager = lifecycleManager;
      return this;
    }
    
    public Http2LifecycleManager lifecycleManager()
    {
      return this.lifecycleManager;
    }
    
    public Builder frameReader(Http2FrameReader frameReader)
    {
      this.frameReader = frameReader;
      return this;
    }
    
    public Builder listener(Http2FrameListener listener)
    {
      this.listener = listener;
      return this;
    }
    
    public Builder encoder(Http2ConnectionEncoder encoder)
    {
      this.encoder = encoder;
      return this;
    }
    
    public Http2ConnectionDecoder build()
    {
      return new DefaultHttp2ConnectionDecoder(this);
    }
  }
  
  public static Builder newBuilder()
  {
    return new Builder();
  }
  
  protected DefaultHttp2ConnectionDecoder(Builder builder)
  {
    this.connection = ((Http2Connection)ObjectUtil.checkNotNull(builder.connection, "connection"));
    this.frameReader = ((Http2FrameReader)ObjectUtil.checkNotNull(builder.frameReader, "frameReader"));
    this.lifecycleManager = ((Http2LifecycleManager)ObjectUtil.checkNotNull(builder.lifecycleManager, "lifecycleManager"));
    this.encoder = ((Http2ConnectionEncoder)ObjectUtil.checkNotNull(builder.encoder, "encoder"));
    this.listener = ((Http2FrameListener)ObjectUtil.checkNotNull(builder.listener, "listener"));
    if (this.connection.local().flowController() == null) {
      this.connection.local().flowController(new DefaultHttp2LocalFlowController(this.connection, this.encoder.frameWriter()));
    }
  }
  
  public Http2Connection connection()
  {
    return this.connection;
  }
  
  public final Http2LocalFlowController flowController()
  {
    return (Http2LocalFlowController)this.connection.local().flowController();
  }
  
  public Http2FrameListener listener()
  {
    return this.listener;
  }
  
  public boolean prefaceReceived()
  {
    return this.prefaceReceived;
  }
  
  public void decodeFrame(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Http2Exception
  {
    this.frameReader.readFrame(ctx, in, this.internalFrameListener);
  }
  
  public Http2Settings localSettings()
  {
    Http2Settings settings = new Http2Settings();
    Http2FrameReader.Configuration config = this.frameReader.configuration();
    Http2HeaderTable headerTable = config.headerTable();
    Http2FrameSizePolicy frameSizePolicy = config.frameSizePolicy();
    settings.initialWindowSize(flowController().initialWindowSize());
    settings.maxConcurrentStreams(this.connection.remote().maxStreams());
    settings.headerTableSize(headerTable.maxHeaderTableSize());
    settings.maxFrameSize(frameSizePolicy.maxFrameSize());
    settings.maxHeaderListSize(headerTable.maxHeaderListSize());
    if (!this.connection.isServer()) {
      settings.pushEnabled(this.connection.local().allowPushTo());
    }
    return settings;
  }
  
  public void localSettings(Http2Settings settings)
    throws Http2Exception
  {
    Boolean pushEnabled = settings.pushEnabled();
    Http2FrameReader.Configuration config = this.frameReader.configuration();
    Http2HeaderTable inboundHeaderTable = config.headerTable();
    Http2FrameSizePolicy inboundFrameSizePolicy = config.frameSizePolicy();
    if (pushEnabled != null)
    {
      if (this.connection.isServer()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server sending SETTINGS frame with ENABLE_PUSH specified", new Object[0]);
      }
      this.connection.local().allowPushTo(pushEnabled.booleanValue());
    }
    Long maxConcurrentStreams = settings.maxConcurrentStreams();
    if (maxConcurrentStreams != null)
    {
      int value = (int)Math.min(maxConcurrentStreams.longValue(), 2147483647L);
      this.connection.remote().maxStreams(value);
    }
    Long headerTableSize = settings.headerTableSize();
    if (headerTableSize != null) {
      inboundHeaderTable.maxHeaderTableSize((int)Math.min(headerTableSize.longValue(), 2147483647L));
    }
    Integer maxHeaderListSize = settings.maxHeaderListSize();
    if (maxHeaderListSize != null) {
      inboundHeaderTable.maxHeaderListSize(maxHeaderListSize.intValue());
    }
    Integer maxFrameSize = settings.maxFrameSize();
    if (maxFrameSize != null) {
      inboundFrameSizePolicy.maxFrameSize(maxFrameSize.intValue());
    }
    Integer initialWindowSize = settings.initialWindowSize();
    if (initialWindowSize != null) {
      flowController().initialWindowSize(initialWindowSize.intValue());
    }
  }
  
  public void close()
  {
    this.frameReader.close();
  }
  
  private int unconsumedBytes(Http2Stream stream)
  {
    return flowController().unconsumedBytes(stream);
  }
  
  private final class FrameReadListener
    implements Http2FrameListener
  {
    private FrameReadListener() {}
    
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
      throws Http2Exception
    {
      verifyPrefaceReceived();
      
      Http2Stream stream = DefaultHttp2ConnectionDecoder.this.connection.requireStream(streamId);
      
      verifyGoAwayNotReceived();
      
      boolean shouldIgnore = shouldIgnoreFrame(stream, false);
      Http2Exception error = null;
      switch (DefaultHttp2ConnectionDecoder.1.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[stream.state().ordinal()])
      {
      case 1: 
      case 2: 
        break;
      case 3: 
        error = Http2Exception.streamError(stream.id(), Http2Error.STREAM_CLOSED, "Stream %d in unexpected state: %s", new Object[] { Integer.valueOf(stream.id()), stream.state() });
        
        break;
      case 4: 
        if (!shouldIgnore) {
          error = Http2Exception.streamError(stream.id(), Http2Error.STREAM_CLOSED, "Stream %d in unexpected state: %s", new Object[] { Integer.valueOf(stream.id()), stream.state() });
        }
        break;
      default: 
        if (!shouldIgnore) {
          error = Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR, "Stream %d in unexpected state: %s", new Object[] { Integer.valueOf(stream.id()), stream.state() });
        }
        break;
      }
      int bytesToReturn = data.readableBytes() + padding;
      int unconsumedBytes = DefaultHttp2ConnectionDecoder.this.unconsumedBytes(stream);
      Http2LocalFlowController flowController = DefaultHttp2ConnectionDecoder.this.flowController();
      try
      {
        flowController.receiveFlowControlledFrame(ctx, stream, data, padding, endOfStream);
        
        unconsumedBytes = DefaultHttp2ConnectionDecoder.this.unconsumedBytes(stream);
        int i;
        if (shouldIgnore) {
          return bytesToReturn;
        }
        if (error != null) {
          throw error;
        }
        bytesToReturn = DefaultHttp2ConnectionDecoder.this.listener.onDataRead(ctx, streamId, data, padding, endOfStream);
        return bytesToReturn;
      }
      catch (Http2Exception e)
      {
        int delta = unconsumedBytes - DefaultHttp2ConnectionDecoder.this.unconsumedBytes(stream);
        bytesToReturn -= delta;
        throw e;
      }
      catch (RuntimeException e)
      {
        int delta = unconsumedBytes - DefaultHttp2ConnectionDecoder.this.unconsumedBytes(stream);
        bytesToReturn -= delta;
        throw e;
      }
      finally
      {
        if (bytesToReturn > 0) {
          flowController.consumeBytes(ctx, stream, bytesToReturn);
        }
        if (endOfStream) {
          DefaultHttp2ConnectionDecoder.this.lifecycleManager.closeRemoteSide(stream, ctx.newSucceededFuture());
        }
      }
    }
    
    private void verifyPrefaceReceived()
      throws Http2Exception
    {
      if (!DefaultHttp2ConnectionDecoder.this.prefaceReceived) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Received non-SETTINGS as first frame.", new Object[0]);
      }
    }
    
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream)
      throws Http2Exception
    {
      onHeadersRead(ctx, streamId, headers, 0, (short)16, false, padding, endOfStream);
    }
    
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream)
      throws Http2Exception
    {
      verifyPrefaceReceived();
      
      Http2Stream stream = DefaultHttp2ConnectionDecoder.this.connection.stream(streamId);
      verifyGoAwayNotReceived();
      if (shouldIgnoreFrame(stream, false)) {
        return;
      }
      if (stream == null) {
        stream = DefaultHttp2ConnectionDecoder.this.connection.createRemoteStream(streamId).open(endOfStream);
      } else {
        switch (DefaultHttp2ConnectionDecoder.1.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[stream.state().ordinal()])
        {
        case 5: 
        case 6: 
          stream.open(endOfStream);
          break;
        case 1: 
        case 2: 
          break;
        case 3: 
        case 4: 
          throw Http2Exception.streamError(stream.id(), Http2Error.STREAM_CLOSED, "Stream %d in unexpected state: %s", new Object[] { Integer.valueOf(stream.id()), stream.state() });
        default: 
          throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream %d in unexpected state: %s", new Object[] { Integer.valueOf(stream.id()), stream.state() });
        }
      }
      DefaultHttp2ConnectionDecoder.this.listener.onHeadersRead(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endOfStream);
      
      stream.setPriority(streamDependency, weight, exclusive);
      if (endOfStream) {
        DefaultHttp2ConnectionDecoder.this.lifecycleManager.closeRemoteSide(stream, ctx.newSucceededFuture());
      }
    }
    
    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive)
      throws Http2Exception
    {
      verifyPrefaceReceived();
      
      Http2Stream stream = DefaultHttp2ConnectionDecoder.this.connection.stream(streamId);
      verifyGoAwayNotReceived();
      if (shouldIgnoreFrame(stream, true)) {
        return;
      }
      if (stream == null) {
        stream = DefaultHttp2ConnectionDecoder.this.connection.createRemoteStream(streamId);
      }
      stream.setPriority(streamDependency, weight, exclusive);
      
      DefaultHttp2ConnectionDecoder.this.listener.onPriorityRead(ctx, streamId, streamDependency, weight, exclusive);
    }
    
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode)
      throws Http2Exception
    {
      verifyPrefaceReceived();
      
      Http2Stream stream = DefaultHttp2ConnectionDecoder.this.connection.requireStream(streamId);
      if (stream.state() == Http2Stream.State.CLOSED) {
        return;
      }
      DefaultHttp2ConnectionDecoder.this.listener.onRstStreamRead(ctx, streamId, errorCode);
      
      DefaultHttp2ConnectionDecoder.this.lifecycleManager.closeStream(stream, ctx.newSucceededFuture());
    }
    
    public void onSettingsAckRead(ChannelHandlerContext ctx)
      throws Http2Exception
    {
      verifyPrefaceReceived();
      
      Http2Settings settings = DefaultHttp2ConnectionDecoder.this.encoder.pollSentSettings();
      if (settings != null) {
        applyLocalSettings(settings);
      }
      DefaultHttp2ConnectionDecoder.this.listener.onSettingsAckRead(ctx);
    }
    
    private void applyLocalSettings(Http2Settings settings)
      throws Http2Exception
    {
      Boolean pushEnabled = settings.pushEnabled();
      Http2FrameReader.Configuration config = DefaultHttp2ConnectionDecoder.this.frameReader.configuration();
      Http2HeaderTable headerTable = config.headerTable();
      Http2FrameSizePolicy frameSizePolicy = config.frameSizePolicy();
      if (pushEnabled != null)
      {
        if (DefaultHttp2ConnectionDecoder.this.connection.isServer()) {
          throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server sending SETTINGS frame with ENABLE_PUSH specified", new Object[0]);
        }
        DefaultHttp2ConnectionDecoder.this.connection.local().allowPushTo(pushEnabled.booleanValue());
      }
      Long maxConcurrentStreams = settings.maxConcurrentStreams();
      if (maxConcurrentStreams != null)
      {
        int value = (int)Math.min(maxConcurrentStreams.longValue(), 2147483647L);
        DefaultHttp2ConnectionDecoder.this.connection.remote().maxStreams(value);
      }
      Long headerTableSize = settings.headerTableSize();
      if (headerTableSize != null) {
        headerTable.maxHeaderTableSize((int)Math.min(headerTableSize.longValue(), 2147483647L));
      }
      Integer maxHeaderListSize = settings.maxHeaderListSize();
      if (maxHeaderListSize != null) {
        headerTable.maxHeaderListSize(maxHeaderListSize.intValue());
      }
      Integer maxFrameSize = settings.maxFrameSize();
      if (maxFrameSize != null) {
        frameSizePolicy.maxFrameSize(maxFrameSize.intValue());
      }
      Integer initialWindowSize = settings.initialWindowSize();
      if (initialWindowSize != null) {
        DefaultHttp2ConnectionDecoder.this.flowController().initialWindowSize(initialWindowSize.intValue());
      }
    }
    
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings)
      throws Http2Exception
    {
      DefaultHttp2ConnectionDecoder.this.encoder.remoteSettings(settings);
      
      DefaultHttp2ConnectionDecoder.this.encoder.writeSettingsAck(ctx, ctx.newPromise());
      
      DefaultHttp2ConnectionDecoder.this.prefaceReceived = true;
      
      DefaultHttp2ConnectionDecoder.this.listener.onSettingsRead(ctx, settings);
    }
    
    public void onPingRead(ChannelHandlerContext ctx, ByteBuf data)
      throws Http2Exception
    {
      verifyPrefaceReceived();
      
      DefaultHttp2ConnectionDecoder.this.encoder.writePing(ctx, true, data.retain(), ctx.newPromise());
      ctx.flush();
      
      DefaultHttp2ConnectionDecoder.this.listener.onPingRead(ctx, data);
    }
    
    public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data)
      throws Http2Exception
    {
      verifyPrefaceReceived();
      
      DefaultHttp2ConnectionDecoder.this.listener.onPingAckRead(ctx, data);
    }
    
    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding)
      throws Http2Exception
    {
      verifyPrefaceReceived();
      
      Http2Stream parentStream = DefaultHttp2ConnectionDecoder.this.connection.requireStream(streamId);
      verifyGoAwayNotReceived();
      if (shouldIgnoreFrame(parentStream, false)) {
        return;
      }
      switch (DefaultHttp2ConnectionDecoder.1.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[parentStream.state().ordinal()])
      {
      case 1: 
      case 2: 
        break;
      default: 
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream %d in unexpected state for receiving push promise: %s", new Object[] { Integer.valueOf(parentStream.id()), parentStream.state() });
      }
      DefaultHttp2ConnectionDecoder.this.connection.remote().reservePushStream(promisedStreamId, parentStream);
      
      DefaultHttp2ConnectionDecoder.this.listener.onPushPromiseRead(ctx, streamId, promisedStreamId, headers, padding);
    }
    
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData)
      throws Http2Exception
    {
      DefaultHttp2ConnectionDecoder.this.connection.goAwayReceived(lastStreamId);
      
      DefaultHttp2ConnectionDecoder.this.listener.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
    }
    
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement)
      throws Http2Exception
    {
      verifyPrefaceReceived();
      
      Http2Stream stream = DefaultHttp2ConnectionDecoder.this.connection.requireStream(streamId);
      verifyGoAwayNotReceived();
      if ((stream.state() == Http2Stream.State.CLOSED) || (shouldIgnoreFrame(stream, false))) {
        return;
      }
      DefaultHttp2ConnectionDecoder.this.encoder.flowController().incrementWindowSize(ctx, stream, windowSizeIncrement);
      
      DefaultHttp2ConnectionDecoder.this.listener.onWindowUpdateRead(ctx, streamId, windowSizeIncrement);
    }
    
    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload)
    {
      DefaultHttp2ConnectionDecoder.this.listener.onUnknownFrame(ctx, frameType, streamId, flags, payload);
    }
    
    private boolean shouldIgnoreFrame(Http2Stream stream, boolean allowResetSent)
    {
      if ((DefaultHttp2ConnectionDecoder.this.connection.goAwaySent()) && ((stream == null) || (DefaultHttp2ConnectionDecoder.this.connection.remote().lastStreamCreated() <= stream.id()))) {
        return true;
      }
      return (stream != null) && (!allowResetSent) && (stream.isResetSent());
    }
    
    private void verifyGoAwayNotReceived()
      throws Http2Exception
    {
      if (DefaultHttp2ConnectionDecoder.this.connection.goAwayReceived()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Received frames after receiving GO_AWAY", new Object[0]);
      }
    }
  }
}
