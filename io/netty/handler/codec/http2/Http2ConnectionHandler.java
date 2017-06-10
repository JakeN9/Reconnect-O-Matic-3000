package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.ObjectUtil;
import java.util.Collection;
import java.util.List;

public class Http2ConnectionHandler
  extends ByteToMessageDecoder
  implements Http2LifecycleManager
{
  private final Http2ConnectionDecoder decoder;
  private final Http2ConnectionEncoder encoder;
  private ByteBuf clientPrefaceString;
  private boolean prefaceSent;
  private ChannelFutureListener closeListener;
  
  public Http2ConnectionHandler(boolean server, Http2FrameListener listener)
  {
    this(new DefaultHttp2Connection(server), listener);
  }
  
  public Http2ConnectionHandler(Http2Connection connection, Http2FrameListener listener)
  {
    this(connection, new DefaultHttp2FrameReader(), new DefaultHttp2FrameWriter(), listener);
  }
  
  public Http2ConnectionHandler(Http2Connection connection, Http2FrameReader frameReader, Http2FrameWriter frameWriter, Http2FrameListener listener)
  {
    this(DefaultHttp2ConnectionDecoder.newBuilder().connection(connection).frameReader(frameReader).listener(listener), DefaultHttp2ConnectionEncoder.newBuilder().connection(connection).frameWriter(frameWriter));
  }
  
  public Http2ConnectionHandler(Http2ConnectionDecoder.Builder decoderBuilder, Http2ConnectionEncoder.Builder encoderBuilder)
  {
    ObjectUtil.checkNotNull(decoderBuilder, "decoderBuilder");
    ObjectUtil.checkNotNull(encoderBuilder, "encoderBuilder");
    if (encoderBuilder.lifecycleManager() != decoderBuilder.lifecycleManager()) {
      throw new IllegalArgumentException("Encoder and Decoder must share a lifecycle manager");
    }
    if (encoderBuilder.lifecycleManager() == null)
    {
      encoderBuilder.lifecycleManager(this);
      decoderBuilder.lifecycleManager(this);
    }
    this.encoder = ((Http2ConnectionEncoder)ObjectUtil.checkNotNull(encoderBuilder.build(), "encoder"));
    
    decoderBuilder.encoder(this.encoder);
    this.decoder = ((Http2ConnectionDecoder)ObjectUtil.checkNotNull(decoderBuilder.build(), "decoder"));
    
    ObjectUtil.checkNotNull(this.encoder.connection(), "encoder.connection");
    if (this.encoder.connection() != this.decoder.connection()) {
      throw new IllegalArgumentException("Encoder and Decoder do not share the same connection object");
    }
    this.clientPrefaceString = clientPrefaceString(this.encoder.connection());
  }
  
  public Http2Connection connection()
  {
    return this.encoder.connection();
  }
  
  public Http2ConnectionDecoder decoder()
  {
    return this.decoder;
  }
  
  public Http2ConnectionEncoder encoder()
  {
    return this.encoder;
  }
  
  public void onHttpClientUpgrade()
    throws Http2Exception
  {
    if (connection().isServer()) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Client-side HTTP upgrade requested for a server", new Object[0]);
    }
    if ((this.prefaceSent) || (this.decoder.prefaceReceived())) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP upgrade must occur before HTTP/2 preface is sent or received", new Object[0]);
    }
    connection().createLocalStream(1).open(true);
  }
  
  public void onHttpServerUpgrade(Http2Settings settings)
    throws Http2Exception
  {
    if (!connection().isServer()) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server-side HTTP upgrade requested for a client", new Object[0]);
    }
    if ((this.prefaceSent) || (this.decoder.prefaceReceived())) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP upgrade must occur before HTTP/2 preface is sent or received", new Object[0]);
    }
    this.encoder.remoteSettings(settings);
    
    connection().createRemoteStream(1).open(true);
  }
  
  public void channelActive(ChannelHandlerContext ctx)
    throws Exception
  {
    sendPreface(ctx);
    super.channelActive(ctx);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {
    sendPreface(ctx);
  }
  
  protected void handlerRemoved0(ChannelHandlerContext ctx)
    throws Exception
  {
    dispose();
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    if (!ctx.channel().isActive())
    {
      ctx.close(promise);
      return;
    }
    ChannelFuture future = writeGoAway(ctx, null);
    if (connection().numActiveStreams() == 0) {
      future.addListener(new ClosingChannelFutureListener(ctx, promise));
    } else {
      this.closeListener = new ClosingChannelFutureListener(ctx, promise);
    }
  }
  
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    ChannelFuture future = ctx.newSucceededFuture();
    Collection<Http2Stream> streams = connection().activeStreams();
    for (Http2Stream s : (Http2Stream[])streams.toArray(new Http2Stream[streams.size()])) {
      closeStream(s, future);
    }
    super.channelInactive(ctx);
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    if (Http2CodecUtil.getEmbeddedHttp2Exception(cause) != null) {
      onException(ctx, cause);
    } else {
      super.exceptionCaught(ctx, cause);
    }
  }
  
  public void closeLocalSide(Http2Stream stream, ChannelFuture future)
  {
    switch (stream.state())
    {
    case HALF_CLOSED_LOCAL: 
    case OPEN: 
      stream.closeLocalSide();
      break;
    default: 
      closeStream(stream, future);
    }
  }
  
  public void closeRemoteSide(Http2Stream stream, ChannelFuture future)
  {
    switch (stream.state())
    {
    case OPEN: 
    case HALF_CLOSED_REMOTE: 
      stream.closeRemoteSide();
      break;
    default: 
      closeStream(stream, future);
    }
  }
  
  public void closeStream(final Http2Stream stream, ChannelFuture future)
  {
    stream.close();
    
    future.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future)
        throws Exception
      {
        Http2ConnectionHandler.this.connection().deactivate(stream);
        if ((Http2ConnectionHandler.this.closeListener != null) && (Http2ConnectionHandler.this.connection().numActiveStreams() == 0)) {
          Http2ConnectionHandler.this.closeListener.operationComplete(future);
        }
      }
    });
  }
  
  public void onException(ChannelHandlerContext ctx, Throwable cause)
  {
    Http2Exception embedded = Http2CodecUtil.getEmbeddedHttp2Exception(cause);
    if (Http2Exception.isStreamError(embedded))
    {
      onStreamError(ctx, cause, (Http2Exception.StreamException)embedded);
    }
    else if ((embedded instanceof Http2Exception.CompositeStreamException))
    {
      Http2Exception.CompositeStreamException compositException = (Http2Exception.CompositeStreamException)embedded;
      for (Http2Exception.StreamException streamException : compositException) {
        onStreamError(ctx, cause, streamException);
      }
    }
    else
    {
      onConnectionError(ctx, cause, embedded);
    }
  }
  
  protected void onConnectionError(ChannelHandlerContext ctx, Throwable cause, Http2Exception http2Ex)
  {
    if (http2Ex == null) {
      http2Ex = new Http2Exception(Http2Error.INTERNAL_ERROR, cause.getMessage(), cause);
    }
    writeGoAway(ctx, http2Ex).addListener(new ClosingChannelFutureListener(ctx, ctx.newPromise()));
  }
  
  protected void onStreamError(ChannelHandlerContext ctx, Throwable cause, Http2Exception.StreamException http2Ex)
  {
    writeRstStream(ctx, http2Ex.streamId(), http2Ex.error().code(), ctx.newPromise());
  }
  
  protected Http2FrameWriter frameWriter()
  {
    return encoder().frameWriter();
  }
  
  public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise)
  {
    Http2Stream stream = connection().stream(streamId);
    ChannelFuture future = frameWriter().writeRstStream(ctx, streamId, errorCode, promise);
    ctx.flush();
    if (stream != null)
    {
      stream.resetSent();
      closeStream(stream, promise);
    }
    return future;
  }
  
  public ChannelFuture writeGoAway(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData, ChannelPromise promise)
  {
    Http2Connection connection = connection();
    if (connection.isGoAway())
    {
      debugData.release();
      return ctx.newSucceededFuture();
    }
    ChannelFuture future = frameWriter().writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);
    ctx.flush();
    
    connection.goAwaySent(lastStreamId);
    return future;
  }
  
  private ChannelFuture writeGoAway(ChannelHandlerContext ctx, Http2Exception cause)
  {
    Http2Connection connection = connection();
    if (connection.isGoAway()) {
      return ctx.newSucceededFuture();
    }
    long errorCode = cause != null ? cause.error().code() : Http2Error.NO_ERROR.code();
    ByteBuf debugData = Http2CodecUtil.toByteBuf(ctx, cause);
    int lastKnownStream = connection.remote().lastStreamCreated();
    return writeGoAway(ctx, lastKnownStream, errorCode, debugData, ctx.newPromise());
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    try
    {
      if (!readClientPrefaceString(in)) {
        return;
      }
      this.decoder.decodeFrame(ctx, in, out);
    }
    catch (Throwable e)
    {
      onException(ctx, e);
    }
  }
  
  private void sendPreface(ChannelHandlerContext ctx)
  {
    if ((this.prefaceSent) || (!ctx.channel().isActive())) {
      return;
    }
    this.prefaceSent = true;
    if (!connection().isServer()) {
      ctx.write(Http2CodecUtil.connectionPrefaceBuf()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }
    this.encoder.writeSettings(ctx, this.decoder.localSettings(), ctx.newPromise()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
  }
  
  private void dispose()
  {
    this.encoder.close();
    this.decoder.close();
    if (this.clientPrefaceString != null)
    {
      this.clientPrefaceString.release();
      this.clientPrefaceString = null;
    }
  }
  
  private boolean readClientPrefaceString(ByteBuf in)
    throws Http2Exception
  {
    if (this.clientPrefaceString == null) {
      return true;
    }
    int prefaceRemaining = this.clientPrefaceString.readableBytes();
    int bytesRead = Math.min(in.readableBytes(), prefaceRemaining);
    
    ByteBuf sourceSlice = in.readSlice(bytesRead);
    
    ByteBuf prefaceSlice = this.clientPrefaceString.readSlice(bytesRead);
    if ((bytesRead == 0) || (!prefaceSlice.equals(sourceSlice))) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP/2 client preface string missing or corrupt.", new Object[0]);
    }
    if (!this.clientPrefaceString.isReadable())
    {
      this.clientPrefaceString.release();
      this.clientPrefaceString = null;
      return true;
    }
    return false;
  }
  
  private static ByteBuf clientPrefaceString(Http2Connection connection)
  {
    return connection.isServer() ? Http2CodecUtil.connectionPrefaceBuf() : null;
  }
  
  private static final class ClosingChannelFutureListener
    implements ChannelFutureListener
  {
    private final ChannelHandlerContext ctx;
    private final ChannelPromise promise;
    
    ClosingChannelFutureListener(ChannelHandlerContext ctx, ChannelPromise promise)
    {
      this.ctx = ctx;
      this.promise = promise;
    }
    
    public void operationComplete(ChannelFuture sentGoAwayFuture)
      throws Exception
    {
      this.ctx.close(this.promise);
    }
  }
}
