package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.internal.ObjectUtil;

public class DelegatingDecompressorFrameListener
  extends Http2FrameListenerDecorator
{
  private static final Http2ConnectionAdapter CLEAN_UP_LISTENER = new Http2ConnectionAdapter()
  {
    public void streamRemoved(Http2Stream stream)
    {
      DelegatingDecompressorFrameListener.Http2Decompressor decompressor = DelegatingDecompressorFrameListener.decompressor(stream);
      if (decompressor != null) {
        DelegatingDecompressorFrameListener.cleanup(stream, decompressor);
      }
    }
  };
  private final Http2Connection connection;
  private final boolean strict;
  private boolean flowControllerInitialized;
  
  public DelegatingDecompressorFrameListener(Http2Connection connection, Http2FrameListener listener)
  {
    this(connection, listener, true);
  }
  
  public DelegatingDecompressorFrameListener(Http2Connection connection, Http2FrameListener listener, boolean strict)
  {
    super(listener);
    this.connection = connection;
    this.strict = strict;
    
    connection.addListener(CLEAN_UP_LISTENER);
  }
  
  public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
    throws Http2Exception
  {
    Http2Stream stream = this.connection.stream(streamId);
    Http2Decompressor decompressor = decompressor(stream);
    if (decompressor == null) {
      return this.listener.onDataRead(ctx, streamId, data, padding, endOfStream);
    }
    EmbeddedChannel channel = decompressor.decompressor();
    int compressedBytes = data.readableBytes() + padding;
    int processedBytes = 0;
    decompressor.incrementCompressedBytes(compressedBytes);
    try
    {
      channel.writeInbound(new Object[] { data.retain() });
      ByteBuf buf = nextReadableBuf(channel);
      if ((buf == null) && (endOfStream) && (channel.finish())) {
        buf = nextReadableBuf(channel);
      }
      if (buf == null)
      {
        if (endOfStream) {
          this.listener.onDataRead(ctx, streamId, Unpooled.EMPTY_BUFFER, padding, true);
        }
        decompressor.incrementDecompressedByes(compressedBytes);
        processedBytes = compressedBytes;
      }
      else
      {
        try
        {
          decompressor.incrementDecompressedByes(padding);
          for (;;)
          {
            ByteBuf nextBuf = nextReadableBuf(channel);
            boolean decompressedEndOfStream = (nextBuf == null) && (endOfStream);
            if ((decompressedEndOfStream) && (channel.finish()))
            {
              nextBuf = nextReadableBuf(channel);
              decompressedEndOfStream = nextBuf == null;
            }
            decompressor.incrementDecompressedByes(buf.readableBytes());
            processedBytes += this.listener.onDataRead(ctx, streamId, buf, padding, decompressedEndOfStream);
            if (nextBuf == null) {
              break;
            }
            padding = 0;
            buf.release();
            buf = nextBuf;
          }
        }
        finally
        {
          buf.release();
        }
      }
      decompressor.incrementProcessedBytes(processedBytes);
      
      return processedBytes;
    }
    catch (Http2Exception e)
    {
      decompressor.incrementProcessedBytes(compressedBytes);
      throw e;
    }
    catch (Throwable t)
    {
      decompressor.incrementProcessedBytes(compressedBytes);
      throw Http2Exception.streamError(stream.id(), Http2Error.INTERNAL_ERROR, t, "Decompressor error detected while delegating data read on streamId %d", new Object[] { Integer.valueOf(stream.id()) });
    }
  }
  
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream)
    throws Http2Exception
  {
    initDecompressor(streamId, headers, endStream);
    this.listener.onHeadersRead(ctx, streamId, headers, padding, endStream);
  }
  
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream)
    throws Http2Exception
  {
    initDecompressor(streamId, headers, endStream);
    this.listener.onHeadersRead(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);
  }
  
  protected EmbeddedChannel newContentDecompressor(AsciiString contentEncoding)
    throws Http2Exception
  {
    if ((HttpHeaderValues.GZIP.equalsIgnoreCase(contentEncoding)) || (HttpHeaderValues.X_GZIP.equalsIgnoreCase(contentEncoding))) {
      return new EmbeddedChannel(new ChannelHandler[] { ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP) });
    }
    if ((HttpHeaderValues.DEFLATE.equalsIgnoreCase(contentEncoding)) || (HttpHeaderValues.X_DEFLATE.equalsIgnoreCase(contentEncoding)))
    {
      ZlibWrapper wrapper = this.strict ? ZlibWrapper.ZLIB : ZlibWrapper.ZLIB_OR_NONE;
      
      return new EmbeddedChannel(new ChannelHandler[] { ZlibCodecFactory.newZlibDecoder(wrapper) });
    }
    return null;
  }
  
  protected AsciiString getTargetContentEncoding(AsciiString contentEncoding)
    throws Http2Exception
  {
    return HttpHeaderValues.IDENTITY;
  }
  
  private void initDecompressor(int streamId, Http2Headers headers, boolean endOfStream)
    throws Http2Exception
  {
    Http2Stream stream = this.connection.stream(streamId);
    if (stream == null) {
      return;
    }
    Http2Decompressor decompressor = decompressor(stream);
    if ((decompressor == null) && (!endOfStream))
    {
      AsciiString contentEncoding = (AsciiString)headers.get(HttpHeaderNames.CONTENT_ENCODING);
      if (contentEncoding == null) {
        contentEncoding = HttpHeaderValues.IDENTITY;
      }
      EmbeddedChannel channel = newContentDecompressor(contentEncoding);
      if (channel != null)
      {
        decompressor = new Http2Decompressor(channel);
        stream.setProperty(Http2Decompressor.class, decompressor);
        
        AsciiString targetContentEncoding = getTargetContentEncoding(contentEncoding);
        if (HttpHeaderValues.IDENTITY.equalsIgnoreCase(targetContentEncoding)) {
          headers.remove(HttpHeaderNames.CONTENT_ENCODING);
        } else {
          headers.set(HttpHeaderNames.CONTENT_ENCODING, targetContentEncoding);
        }
      }
    }
    if (decompressor != null)
    {
      headers.remove(HttpHeaderNames.CONTENT_LENGTH);
      if (!this.flowControllerInitialized)
      {
        this.flowControllerInitialized = true;
        this.connection.local().flowController(new ConsumedBytesConverter((Http2LocalFlowController)this.connection.local().flowController()));
      }
    }
  }
  
  private static Http2Decompressor decompressor(Http2Stream stream)
  {
    return (Http2Decompressor)(stream == null ? null : stream.getProperty(Http2Decompressor.class));
  }
  
  private static void cleanup(Http2Stream stream, Http2Decompressor decompressor)
  {
    EmbeddedChannel channel = decompressor.decompressor();
    if (channel.finish()) {
      for (;;)
      {
        ByteBuf buf = (ByteBuf)channel.readInbound();
        if (buf == null) {
          break;
        }
        buf.release();
      }
    }
    decompressor = (Http2Decompressor)stream.removeProperty(Http2Decompressor.class);
  }
  
  private static ByteBuf nextReadableBuf(EmbeddedChannel decompressor)
  {
    ByteBuf buf;
    for (;;)
    {
      buf = (ByteBuf)decompressor.readInbound();
      if (buf == null) {
        return null;
      }
      if (buf.isReadable()) {
        break;
      }
      buf.release();
    }
    return buf;
  }
  
  private static final class ConsumedBytesConverter
    implements Http2LocalFlowController
  {
    private final Http2LocalFlowController flowController;
    
    ConsumedBytesConverter(Http2LocalFlowController flowController)
    {
      this.flowController = ((Http2LocalFlowController)ObjectUtil.checkNotNull(flowController, "flowController"));
    }
    
    public void initialWindowSize(int newWindowSize)
      throws Http2Exception
    {
      this.flowController.initialWindowSize(newWindowSize);
    }
    
    public int initialWindowSize()
    {
      return this.flowController.initialWindowSize();
    }
    
    public int windowSize(Http2Stream stream)
    {
      return this.flowController.windowSize(stream);
    }
    
    public void incrementWindowSize(ChannelHandlerContext ctx, Http2Stream stream, int delta)
      throws Http2Exception
    {
      this.flowController.incrementWindowSize(ctx, stream, delta);
    }
    
    public void receiveFlowControlledFrame(ChannelHandlerContext ctx, Http2Stream stream, ByteBuf data, int padding, boolean endOfStream)
      throws Http2Exception
    {
      this.flowController.receiveFlowControlledFrame(ctx, stream, data, padding, endOfStream);
    }
    
    public void consumeBytes(ChannelHandlerContext ctx, Http2Stream stream, int numBytes)
      throws Http2Exception
    {
      DelegatingDecompressorFrameListener.Http2Decompressor decompressor = DelegatingDecompressorFrameListener.decompressor(stream);
      DelegatingDecompressorFrameListener.Http2Decompressor copy = null;
      try
      {
        if (decompressor != null)
        {
          copy = new DelegatingDecompressorFrameListener.Http2Decompressor(decompressor);
          
          numBytes = decompressor.consumeProcessedBytes(numBytes);
        }
        this.flowController.consumeBytes(ctx, stream, numBytes);
      }
      catch (Http2Exception e)
      {
        if (copy != null) {
          stream.setProperty(DelegatingDecompressorFrameListener.Http2Decompressor.class, copy);
        }
        throw e;
      }
      catch (Throwable t)
      {
        if (copy != null) {
          stream.setProperty(DelegatingDecompressorFrameListener.Http2Decompressor.class, copy);
        }
        throw new Http2Exception(Http2Error.INTERNAL_ERROR, "Error while returning bytes to flow control window", t);
      }
    }
    
    public int unconsumedBytes(Http2Stream stream)
    {
      return this.flowController.unconsumedBytes(stream);
    }
  }
  
  private static final class Http2Decompressor
  {
    private final EmbeddedChannel decompressor;
    private int processed;
    private int compressed;
    private int decompressed;
    
    Http2Decompressor(Http2Decompressor rhs)
    {
      this(rhs.decompressor);
      this.processed = rhs.processed;
      this.compressed = rhs.compressed;
      this.decompressed = rhs.decompressed;
    }
    
    Http2Decompressor(EmbeddedChannel decompressor)
    {
      this.decompressor = decompressor;
    }
    
    EmbeddedChannel decompressor()
    {
      return this.decompressor;
    }
    
    void incrementProcessedBytes(int delta)
    {
      if (this.processed + delta < 0) {
        throw new IllegalArgumentException("processed bytes cannot be negative");
      }
      this.processed += delta;
    }
    
    void incrementCompressedBytes(int delta)
    {
      if (this.compressed + delta < 0) {
        throw new IllegalArgumentException("compressed bytes cannot be negative");
      }
      this.compressed += delta;
    }
    
    void incrementDecompressedByes(int delta)
    {
      if (this.decompressed + delta < 0) {
        throw new IllegalArgumentException("decompressed bytes cannot be negative");
      }
      this.decompressed += delta;
    }
    
    int consumeProcessedBytes(int processedBytes)
    {
      incrementProcessedBytes(-processedBytes);
      
      double consumedRatio = processedBytes / this.decompressed;
      int consumedCompressed = Math.min(this.compressed, (int)Math.ceil(this.compressed * consumedRatio));
      incrementDecompressedByes(-Math.min(this.decompressed, (int)Math.ceil(this.decompressed * consumedRatio)));
      incrementCompressedBytes(-consumedCompressed);
      
      return consumedCompressed;
    }
  }
}
