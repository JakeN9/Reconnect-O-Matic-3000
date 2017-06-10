package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;

public class DefaultHttp2ConnectionEncoder
  implements Http2ConnectionEncoder
{
  private final Http2FrameWriter frameWriter;
  private final Http2Connection connection;
  private final Http2LifecycleManager lifecycleManager;
  private final ArrayDeque<Http2Settings> outstandingLocalSettingsQueue = new ArrayDeque(4);
  
  public static class Builder
    implements Http2ConnectionEncoder.Builder
  {
    protected Http2FrameWriter frameWriter;
    protected Http2Connection connection;
    protected Http2LifecycleManager lifecycleManager;
    
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
    
    public Builder frameWriter(Http2FrameWriter frameWriter)
    {
      this.frameWriter = frameWriter;
      return this;
    }
    
    public Http2ConnectionEncoder build()
    {
      return new DefaultHttp2ConnectionEncoder(this);
    }
  }
  
  public static Builder newBuilder()
  {
    return new Builder();
  }
  
  protected DefaultHttp2ConnectionEncoder(Builder builder)
  {
    this.connection = ((Http2Connection)ObjectUtil.checkNotNull(builder.connection, "connection"));
    this.frameWriter = ((Http2FrameWriter)ObjectUtil.checkNotNull(builder.frameWriter, "frameWriter"));
    this.lifecycleManager = ((Http2LifecycleManager)ObjectUtil.checkNotNull(builder.lifecycleManager, "lifecycleManager"));
    if (this.connection.remote().flowController() == null) {
      this.connection.remote().flowController(new DefaultHttp2RemoteFlowController(this.connection));
    }
  }
  
  public Http2FrameWriter frameWriter()
  {
    return this.frameWriter;
  }
  
  public Http2Connection connection()
  {
    return this.connection;
  }
  
  public final Http2RemoteFlowController flowController()
  {
    return (Http2RemoteFlowController)connection().remote().flowController();
  }
  
  public void remoteSettings(Http2Settings settings)
    throws Http2Exception
  {
    Boolean pushEnabled = settings.pushEnabled();
    Http2FrameWriter.Configuration config = configuration();
    Http2HeaderTable outboundHeaderTable = config.headerTable();
    Http2FrameSizePolicy outboundFrameSizePolicy = config.frameSizePolicy();
    if (pushEnabled != null)
    {
      if (!this.connection.isServer()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Client received SETTINGS frame with ENABLE_PUSH specified", new Object[0]);
      }
      this.connection.remote().allowPushTo(pushEnabled.booleanValue());
    }
    Long maxConcurrentStreams = settings.maxConcurrentStreams();
    if (maxConcurrentStreams != null) {
      this.connection.local().maxStreams((int)Math.min(maxConcurrentStreams.longValue(), 2147483647L));
    }
    Long headerTableSize = settings.headerTableSize();
    if (headerTableSize != null) {
      outboundHeaderTable.maxHeaderTableSize((int)Math.min(headerTableSize.longValue(), 2147483647L));
    }
    Integer maxHeaderListSize = settings.maxHeaderListSize();
    if (maxHeaderListSize != null) {
      outboundHeaderTable.maxHeaderListSize(maxHeaderListSize.intValue());
    }
    Integer maxFrameSize = settings.maxFrameSize();
    if (maxFrameSize != null) {
      outboundFrameSizePolicy.maxFrameSize(maxFrameSize.intValue());
    }
    Integer initialWindowSize = settings.initialWindowSize();
    if (initialWindowSize != null) {
      flowController().initialWindowSize(initialWindowSize.intValue());
    }
  }
  
  public ChannelFuture writeData(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream, ChannelPromise promise)
  {
    Http2Stream stream;
    try
    {
      if (this.connection.isGoAway()) {
        throw new IllegalStateException("Sending data after connection going away.");
      }
      stream = this.connection.requireStream(streamId);
      switch (stream.state())
      {
      case OPEN: 
      case HALF_CLOSED_REMOTE: 
        break;
      default: 
        throw new IllegalStateException(String.format("Stream %d in unexpected state: %s", new Object[] { Integer.valueOf(stream.id()), stream.state() }));
      }
      if (endOfStream) {
        this.lifecycleManager.closeLocalSide(stream, promise);
      }
    }
    catch (Throwable e)
    {
      data.release();
      return promise.setFailure(e);
    }
    flowController().sendFlowControlled(ctx, stream, new FlowControlledData(ctx, stream, data, padding, endOfStream, promise, null));
    
    return promise;
  }
  
  public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream, ChannelPromise promise)
  {
    return writeHeaders(ctx, streamId, headers, 0, (short)16, false, padding, endStream, promise);
  }
  
  public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise)
  {
    try
    {
      if (this.connection.isGoAway()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending headers after connection going away.", new Object[0]);
      }
      Http2Stream stream = this.connection.stream(streamId);
      if (stream == null) {
        stream = this.connection.createLocalStream(streamId);
      }
      switch (stream.state())
      {
      case RESERVED_LOCAL: 
      case IDLE: 
        stream.open(endOfStream);
        break;
      case OPEN: 
      case HALF_CLOSED_REMOTE: 
        break;
      default: 
        throw new IllegalStateException(String.format("Stream %d in unexpected state: %s", new Object[] { Integer.valueOf(stream.id()), stream.state() }));
      }
      flowController().sendFlowControlled(ctx, stream, new FlowControlledHeaders(ctx, stream, headers, streamDependency, weight, exclusive, padding, endOfStream, promise, null));
      if (endOfStream) {
        this.lifecycleManager.closeLocalSide(stream, promise);
      }
      return promise;
    }
    catch (Http2NoMoreStreamIdsException e)
    {
      this.lifecycleManager.onException(ctx, e);
      return promise.setFailure(e);
    }
    catch (Throwable e)
    {
      return promise.setFailure(e);
    }
  }
  
  public ChannelFuture writePriority(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive, ChannelPromise promise)
  {
    try
    {
      if (this.connection.isGoAway()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending priority after connection going away.", new Object[0]);
      }
      Http2Stream stream = this.connection.stream(streamId);
      if (stream == null) {
        stream = this.connection.createLocalStream(streamId);
      }
      stream.setPriority(streamDependency, weight, exclusive);
    }
    catch (Throwable e)
    {
      return promise.setFailure(e);
    }
    ChannelFuture future = this.frameWriter.writePriority(ctx, streamId, streamDependency, weight, exclusive, promise);
    ctx.flush();
    return future;
  }
  
  public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise)
  {
    return this.lifecycleManager.writeRstStream(ctx, streamId, errorCode, promise);
  }
  
  public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise, boolean writeIfNoStream)
  {
    Http2Stream stream = this.connection.stream(streamId);
    if ((stream == null) && (!writeIfNoStream))
    {
      promise.setSuccess();
      return promise;
    }
    ChannelFuture future = this.frameWriter.writeRstStream(ctx, streamId, errorCode, promise);
    ctx.flush();
    if (stream != null)
    {
      stream.resetSent();
      this.lifecycleManager.closeStream(stream, promise);
    }
    return future;
  }
  
  public ChannelFuture writeSettings(ChannelHandlerContext ctx, Http2Settings settings, ChannelPromise promise)
  {
    this.outstandingLocalSettingsQueue.add(settings);
    try
    {
      if (this.connection.isGoAway()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending settings after connection going away.", new Object[0]);
      }
      Boolean pushEnabled = settings.pushEnabled();
      if ((pushEnabled != null) && (this.connection.isServer())) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server sending SETTINGS frame with ENABLE_PUSH specified", new Object[0]);
      }
    }
    catch (Throwable e)
    {
      return promise.setFailure(e);
    }
    ChannelFuture future = this.frameWriter.writeSettings(ctx, settings, promise);
    ctx.flush();
    return future;
  }
  
  public ChannelFuture writeSettingsAck(ChannelHandlerContext ctx, ChannelPromise promise)
  {
    ChannelFuture future = this.frameWriter.writeSettingsAck(ctx, promise);
    ctx.flush();
    return future;
  }
  
  public ChannelFuture writePing(ChannelHandlerContext ctx, boolean ack, ByteBuf data, ChannelPromise promise)
  {
    if (this.connection.isGoAway())
    {
      data.release();
      return promise.setFailure(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending ping after connection going away.", new Object[0]));
    }
    ChannelFuture future = this.frameWriter.writePing(ctx, ack, data, promise);
    ctx.flush();
    return future;
  }
  
  public ChannelFuture writePushPromise(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding, ChannelPromise promise)
  {
    try
    {
      if (this.connection.isGoAway()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending push promise after connection going away.", new Object[0]);
      }
      Http2Stream stream = this.connection.requireStream(streamId);
      this.connection.local().reservePushStream(promisedStreamId, stream);
    }
    catch (Throwable e)
    {
      return promise.setFailure(e);
    }
    ChannelFuture future = this.frameWriter.writePushPromise(ctx, streamId, promisedStreamId, headers, padding, promise);
    ctx.flush();
    return future;
  }
  
  public ChannelFuture writeGoAway(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData, ChannelPromise promise)
  {
    return this.lifecycleManager.writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);
  }
  
  public ChannelFuture writeWindowUpdate(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement, ChannelPromise promise)
  {
    return promise.setFailure(new UnsupportedOperationException("Use the Http2[Inbound|Outbound]FlowController objects to control window sizes"));
  }
  
  public ChannelFuture writeFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload, ChannelPromise promise)
  {
    return this.frameWriter.writeFrame(ctx, frameType, streamId, flags, payload, promise);
  }
  
  public void close()
  {
    this.frameWriter.close();
  }
  
  public Http2Settings pollSentSettings()
  {
    return (Http2Settings)this.outstandingLocalSettingsQueue.poll();
  }
  
  public Http2FrameWriter.Configuration configuration()
  {
    return this.frameWriter.configuration();
  }
  
  private final class FlowControlledData
    extends DefaultHttp2ConnectionEncoder.FlowControlledBase
  {
    private ByteBuf data;
    private int size;
    
    private FlowControlledData(ChannelHandlerContext ctx, Http2Stream stream, ByteBuf data, int padding, boolean endOfStream, ChannelPromise promise)
    {
      super(ctx, stream, padding, endOfStream, promise);
      this.data = data;
      this.size = (data.readableBytes() + padding);
    }
    
    public int size()
    {
      return this.size;
    }
    
    public void error(Throwable cause)
    {
      ReferenceCountUtil.safeRelease(this.data);
      DefaultHttp2ConnectionEncoder.this.lifecycleManager.onException(this.ctx, cause);
      this.data = null;
      this.size = 0;
      this.promise.tryFailure(cause);
    }
    
    public boolean write(int allowedBytes)
    {
      if (this.data == null) {
        return false;
      }
      if ((allowedBytes == 0) && (size() != 0)) {
        return false;
      }
      int maxFrameSize = DefaultHttp2ConnectionEncoder.this.frameWriter().configuration().frameSizePolicy().maxFrameSize();
      try
      {
        int bytesWritten = 0;
        do
        {
          int allowedFrameSize = Math.min(maxFrameSize, allowedBytes - bytesWritten);
          
          int writeableData = this.data.readableBytes();
          ByteBuf toWrite;
          ByteBuf toWrite;
          if (writeableData > allowedFrameSize)
          {
            writeableData = allowedFrameSize;
            toWrite = this.data.readSlice(writeableData).retain();
          }
          else
          {
            toWrite = this.data;
            this.data = Unpooled.EMPTY_BUFFER;
          }
          int writeablePadding = Math.min(allowedFrameSize - writeableData, this.padding);
          this.padding -= writeablePadding;
          bytesWritten += writeableData + writeablePadding;
          ChannelPromise writePromise;
          ChannelPromise writePromise;
          if (this.size == bytesWritten)
          {
            writePromise = this.promise;
          }
          else
          {
            writePromise = this.ctx.newPromise();
            writePromise.addListener(this);
          }
          DefaultHttp2ConnectionEncoder.this.frameWriter().writeData(this.ctx, this.stream.id(), toWrite, writeablePadding, (this.size == bytesWritten) && (this.endOfStream), writePromise);
        } while ((this.size != bytesWritten) && (allowedBytes > bytesWritten));
        this.size -= bytesWritten;
        return true;
      }
      catch (Throwable e)
      {
        error(e);
      }
      return false;
    }
  }
  
  private final class FlowControlledHeaders
    extends DefaultHttp2ConnectionEncoder.FlowControlledBase
  {
    private final Http2Headers headers;
    private final int streamDependency;
    private final short weight;
    private final boolean exclusive;
    
    private FlowControlledHeaders(ChannelHandlerContext ctx, Http2Stream stream, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise)
    {
      super(ctx, stream, padding, endOfStream, promise);
      this.headers = headers;
      this.streamDependency = streamDependency;
      this.weight = weight;
      this.exclusive = exclusive;
    }
    
    public int size()
    {
      return 0;
    }
    
    public void error(Throwable cause)
    {
      DefaultHttp2ConnectionEncoder.this.lifecycleManager.onException(this.ctx, cause);
      this.promise.tryFailure(cause);
    }
    
    public boolean write(int allowedBytes)
    {
      DefaultHttp2ConnectionEncoder.this.frameWriter().writeHeaders(this.ctx, this.stream.id(), this.headers, this.streamDependency, this.weight, this.exclusive, this.padding, this.endOfStream, this.promise);
      
      return true;
    }
  }
  
  public abstract class FlowControlledBase
    implements Http2RemoteFlowController.FlowControlled, ChannelFutureListener
  {
    protected final ChannelHandlerContext ctx;
    protected final Http2Stream stream;
    protected final ChannelPromise promise;
    protected final boolean endOfStream;
    protected int padding;
    
    public FlowControlledBase(ChannelHandlerContext ctx, Http2Stream stream, int padding, boolean endOfStream, ChannelPromise promise)
    {
      this.ctx = ctx;
      if (padding < 0) {
        throw new IllegalArgumentException("padding must be >= 0");
      }
      this.padding = padding;
      this.endOfStream = endOfStream;
      this.stream = stream;
      this.promise = promise;
      promise.addListener(this);
    }
    
    public void operationComplete(ChannelFuture future)
      throws Exception
    {
      if (!future.isSuccess()) {
        error(future.cause());
      }
    }
  }
}
