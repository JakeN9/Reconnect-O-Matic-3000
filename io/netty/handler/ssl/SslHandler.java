package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.PendingWriteQueue;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.OneTimeTask;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

public class SslHandler
  extends ByteToMessageDecoder
{
  private static final InternalLogger logger;
  private static final Pattern IGNORABLE_CLASS_IN_STACK;
  private static final Pattern IGNORABLE_ERROR_MESSAGE;
  private static final SSLException SSLENGINE_CLOSED;
  private static final SSLException HANDSHAKE_TIMED_OUT;
  private static final ClosedChannelException CHANNEL_CLOSED;
  private volatile ChannelHandlerContext ctx;
  private final SSLEngine engine;
  private final int maxPacketBufferSize;
  
  static
  {
    logger = InternalLoggerFactory.getInstance(SslHandler.class);
    
    IGNORABLE_CLASS_IN_STACK = Pattern.compile("^.*(?:Socket|Datagram|Sctp|Udt)Channel.*$");
    
    IGNORABLE_ERROR_MESSAGE = Pattern.compile("^.*(?:connection.*(?:reset|closed|abort|broken)|broken.*pipe).*$", 2);
    
    SSLENGINE_CLOSED = new SSLException("SSLEngine closed already");
    HANDSHAKE_TIMED_OUT = new SSLException("handshake timed out");
    CHANNEL_CLOSED = new ClosedChannelException();
    
    SSLENGINE_CLOSED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    HANDSHAKE_TIMED_OUT.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    CHANNEL_CLOSED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
  }
  
  private final ByteBuffer[] singleBuffer = new ByteBuffer[1];
  private final boolean wantsDirectBuffer;
  private final boolean wantsLargeOutboundNetworkBuffer;
  private boolean wantsInboundHeapBuffer;
  private final boolean startTls;
  private boolean sentFirstMessage;
  private boolean flushedBeforeHandshake;
  private boolean readDuringHandshake;
  private PendingWriteQueue pendingUnencryptedWrites;
  private Promise<Channel> handshakePromise = new LazyChannelPromise(null);
  private final LazyChannelPromise sslCloseFuture = new LazyChannelPromise(null);
  private boolean needsFlush;
  private int packetLength;
  private volatile long handshakeTimeoutMillis = 10000L;
  private volatile long closeNotifyTimeoutMillis = 3000L;
  
  public SslHandler(SSLEngine engine)
  {
    this(engine, false);
  }
  
  public SslHandler(SSLEngine engine, boolean startTls)
  {
    if (engine == null) {
      throw new NullPointerException("engine");
    }
    this.engine = engine;
    this.startTls = startTls;
    this.maxPacketBufferSize = engine.getSession().getPacketBufferSize();
    
    boolean opensslEngine = engine instanceof OpenSslEngine;
    this.wantsDirectBuffer = opensslEngine;
    this.wantsLargeOutboundNetworkBuffer = (!opensslEngine);
    
    setCumulator(opensslEngine ? COMPOSITE_CUMULATOR : MERGE_CUMULATOR);
  }
  
  public long getHandshakeTimeoutMillis()
  {
    return this.handshakeTimeoutMillis;
  }
  
  public void setHandshakeTimeout(long handshakeTimeout, TimeUnit unit)
  {
    if (unit == null) {
      throw new NullPointerException("unit");
    }
    setHandshakeTimeoutMillis(unit.toMillis(handshakeTimeout));
  }
  
  public void setHandshakeTimeoutMillis(long handshakeTimeoutMillis)
  {
    if (handshakeTimeoutMillis < 0L) {
      throw new IllegalArgumentException("handshakeTimeoutMillis: " + handshakeTimeoutMillis + " (expected: >= 0)");
    }
    this.handshakeTimeoutMillis = handshakeTimeoutMillis;
  }
  
  public long getCloseNotifyTimeoutMillis()
  {
    return this.closeNotifyTimeoutMillis;
  }
  
  public void setCloseNotifyTimeout(long closeNotifyTimeout, TimeUnit unit)
  {
    if (unit == null) {
      throw new NullPointerException("unit");
    }
    setCloseNotifyTimeoutMillis(unit.toMillis(closeNotifyTimeout));
  }
  
  public void setCloseNotifyTimeoutMillis(long closeNotifyTimeoutMillis)
  {
    if (closeNotifyTimeoutMillis < 0L) {
      throw new IllegalArgumentException("closeNotifyTimeoutMillis: " + closeNotifyTimeoutMillis + " (expected: >= 0)");
    }
    this.closeNotifyTimeoutMillis = closeNotifyTimeoutMillis;
  }
  
  public SSLEngine engine()
  {
    return this.engine;
  }
  
  public Future<Channel> handshakeFuture()
  {
    return this.handshakePromise;
  }
  
  public ChannelFuture close()
  {
    return close(this.ctx.newPromise());
  }
  
  public ChannelFuture close(final ChannelPromise future)
  {
    final ChannelHandlerContext ctx = this.ctx;
    ctx.executor().execute(new Runnable()
    {
      public void run()
      {
        SslHandler.this.engine.closeOutbound();
        try
        {
          SslHandler.this.write(ctx, Unpooled.EMPTY_BUFFER, future);
          SslHandler.this.flush(ctx);
        }
        catch (Exception e)
        {
          if (!future.tryFailure(e)) {
            SslHandler.logger.warn("{} flush() raised a masked exception.", ctx.channel(), e);
          }
        }
      }
    });
    return future;
  }
  
  public Future<Channel> sslCloseFuture()
  {
    return this.sslCloseFuture;
  }
  
  public void handlerRemoved0(ChannelHandlerContext ctx)
    throws Exception
  {
    if (!this.pendingUnencryptedWrites.isEmpty()) {
      this.pendingUnencryptedWrites.removeAndFailAll(new ChannelException("Pending write on removal of SslHandler"));
    }
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    closeOutboundAndChannel(ctx, promise, true);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    closeOutboundAndChannel(ctx, promise, false);
  }
  
  public void read(ChannelHandlerContext ctx)
    throws Exception
  {
    if (!this.handshakePromise.isDone()) {
      this.readDuringHandshake = true;
    }
    ctx.read();
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    this.pendingUnencryptedWrites.add(msg, promise);
  }
  
  public void flush(ChannelHandlerContext ctx)
    throws Exception
  {
    if ((this.startTls) && (!this.sentFirstMessage))
    {
      this.sentFirstMessage = true;
      this.pendingUnencryptedWrites.removeAndWriteAll();
      ctx.flush();
      return;
    }
    if (this.pendingUnencryptedWrites.isEmpty()) {
      this.pendingUnencryptedWrites.add(Unpooled.EMPTY_BUFFER, ctx.newPromise());
    }
    if (!this.handshakePromise.isDone()) {
      this.flushedBeforeHandshake = true;
    }
    wrap(ctx, false);
    ctx.flush();
  }
  
  private void wrap(ChannelHandlerContext ctx, boolean inUnwrap)
    throws SSLException
  {
    ByteBuf out = null;
    ChannelPromise promise = null;
    ByteBufAllocator alloc = ctx.alloc();
    try
    {
      for (;;)
      {
        Object msg = this.pendingUnencryptedWrites.current();
        if (msg == null) {
          break;
        }
        if (!(msg instanceof ByteBuf))
        {
          this.pendingUnencryptedWrites.removeAndWrite();
        }
        else
        {
          ByteBuf buf = (ByteBuf)msg;
          if (out == null) {
            out = allocateOutNetBuf(ctx, buf.readableBytes());
          }
          SSLEngineResult result = wrap(alloc, this.engine, buf, out);
          if (!buf.isReadable()) {
            promise = this.pendingUnencryptedWrites.remove();
          } else {
            promise = null;
          }
          if (result.getStatus() == SSLEngineResult.Status.CLOSED)
          {
            this.pendingUnencryptedWrites.removeAndFailAll(SSLENGINE_CLOSED); return;
          }
          switch (result.getHandshakeStatus())
          {
          case NEED_TASK: 
            runDelegatedTasks();
            break;
          case FINISHED: 
            setHandshakeSuccess();
          case NOT_HANDSHAKING: 
            setHandshakeSuccessIfStillHandshaking();
          case NEED_WRAP: 
            finishWrap(ctx, out, promise, inUnwrap);
            promise = null;
            out = null;
            break;
          case NEED_UNWRAP: 
            return;
          default: 
            throw new IllegalStateException("Unknown handshake status: " + result.getHandshakeStatus());
          }
        }
      }
    }
    catch (SSLException e)
    {
      setHandshakeFailure(ctx, e);
      throw e;
    }
    finally
    {
      finishWrap(ctx, out, promise, inUnwrap);
    }
  }
  
  private void finishWrap(ChannelHandlerContext ctx, ByteBuf out, ChannelPromise promise, boolean inUnwrap)
  {
    if (out == null)
    {
      out = Unpooled.EMPTY_BUFFER;
    }
    else if (!out.isReadable())
    {
      out.release();
      out = Unpooled.EMPTY_BUFFER;
    }
    if (promise != null) {
      ctx.write(out, promise);
    } else {
      ctx.write(out);
    }
    if (inUnwrap) {
      this.needsFlush = true;
    }
  }
  
  private void wrapNonAppData(ChannelHandlerContext ctx, boolean inUnwrap)
    throws SSLException
  {
    ByteBuf out = null;
    ByteBufAllocator alloc = ctx.alloc();
    try
    {
      for (;;)
      {
        if (out == null) {
          out = allocateOutNetBuf(ctx, 0);
        }
        SSLEngineResult result = wrap(alloc, this.engine, Unpooled.EMPTY_BUFFER, out);
        if (result.bytesProduced() > 0)
        {
          ctx.write(out);
          if (inUnwrap) {
            this.needsFlush = true;
          }
          out = null;
        }
        switch (result.getHandshakeStatus())
        {
        case FINISHED: 
          setHandshakeSuccess();
          break;
        case NEED_TASK: 
          runDelegatedTasks();
          break;
        case NEED_UNWRAP: 
          if (!inUnwrap) {
            unwrapNonAppData(ctx);
          }
          break;
        case NEED_WRAP: 
          break;
        case NOT_HANDSHAKING: 
          setHandshakeSuccessIfStillHandshaking();
          if (!inUnwrap) {
            unwrapNonAppData(ctx);
          }
          break;
        default: 
          throw new IllegalStateException("Unknown handshake status: " + result.getHandshakeStatus());
        }
        if (result.bytesProduced() == 0) {
          break;
        }
      }
    }
    catch (SSLException e)
    {
      setHandshakeFailure(ctx, e);
      throw e;
    }
    finally
    {
      if (out != null) {
        out.release();
      }
    }
  }
  
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    setHandshakeFailure(ctx, CHANNEL_CLOSED);
    super.channelInactive(ctx);
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    if (ignoreException(cause))
    {
      if (logger.isDebugEnabled()) {
        logger.debug("{} Swallowing a harmless 'connection reset by peer / broken pipe' error that occurred while writing close_notify in response to the peer's close_notify", ctx.channel(), cause);
      }
      if (ctx.channel().isActive()) {
        ctx.close();
      }
    }
    else
    {
      ctx.fireExceptionCaught(cause);
    }
  }
  
  private boolean ignoreException(Throwable t)
  {
    if ((!(t instanceof SSLException)) && ((t instanceof IOException)) && (this.sslCloseFuture.isDone()))
    {
      String message = String.valueOf(t.getMessage()).toLowerCase();
      if (IGNORABLE_ERROR_MESSAGE.matcher(message).matches()) {
        return true;
      }
      StackTraceElement[] elements = t.getStackTrace();
      for (StackTraceElement element : elements)
      {
        String classname = element.getClassName();
        String methodname = element.getMethodName();
        if (!classname.startsWith("io.netty.")) {
          if ("read".equals(methodname))
          {
            if (IGNORABLE_CLASS_IN_STACK.matcher(classname).matches()) {
              return true;
            }
            try
            {
              Class<?> clazz = PlatformDependent.getClassLoader(getClass()).loadClass(classname);
              if ((SocketChannel.class.isAssignableFrom(clazz)) || (DatagramChannel.class.isAssignableFrom(clazz))) {
                return true;
              }
              if ((PlatformDependent.javaVersion() >= 7) && ("com.sun.nio.sctp.SctpChannel".equals(clazz.getSuperclass().getName()))) {
                return true;
              }
            }
            catch (ClassNotFoundException e) {}
          }
        }
      }
    }
    return false;
  }
  
  public static boolean isEncrypted(ByteBuf buffer)
  {
    if (buffer.readableBytes() < 5) {
      throw new IllegalArgumentException("buffer must have at least 5 readable bytes");
    }
    return getEncryptedPacketLength(buffer, buffer.readerIndex()) != -1;
  }
  
  private static int getEncryptedPacketLength(ByteBuf buffer, int offset)
  {
    int packetLength = 0;
    boolean tls;
    switch (buffer.getUnsignedByte(offset))
    {
    case 20: 
    case 21: 
    case 22: 
    case 23: 
      tls = true;
      break;
    default: 
      tls = false;
    }
    if (tls)
    {
      int majorVersion = buffer.getUnsignedByte(offset + 1);
      if (majorVersion == 3)
      {
        packetLength = buffer.getUnsignedShort(offset + 3) + 5;
        if (packetLength <= 5) {
          tls = false;
        }
      }
      else
      {
        tls = false;
      }
    }
    if (!tls)
    {
      boolean sslv2 = true;
      int headerLength = (buffer.getUnsignedByte(offset) & 0x80) != 0 ? 2 : 3;
      int majorVersion = buffer.getUnsignedByte(offset + headerLength + 1);
      if ((majorVersion == 2) || (majorVersion == 3))
      {
        if (headerLength == 2) {
          packetLength = (buffer.getShort(offset) & 0x7FFF) + 2;
        } else {
          packetLength = (buffer.getShort(offset) & 0x3FFF) + 3;
        }
        if (packetLength <= headerLength) {
          sslv2 = false;
        }
      }
      else
      {
        sslv2 = false;
      }
      if (!sslv2) {
        return -1;
      }
    }
    return packetLength;
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws SSLException
  {
    int startOffset = in.readerIndex();
    int endOffset = in.writerIndex();
    int offset = startOffset;
    int totalLength = 0;
    if (this.packetLength > 0)
    {
      if (endOffset - startOffset < this.packetLength) {
        return;
      }
      offset += this.packetLength;
      totalLength = this.packetLength;
      this.packetLength = 0;
    }
    boolean nonSslRecord = false;
    while (totalLength < 18713)
    {
      int readableBytes = endOffset - offset;
      if (readableBytes < 5) {
        break;
      }
      int packetLength = getEncryptedPacketLength(in, offset);
      if (packetLength == -1)
      {
        nonSslRecord = true;
        break;
      }
      assert (packetLength > 0);
      if (packetLength > readableBytes)
      {
        this.packetLength = packetLength;
        break;
      }
      int newTotalLength = totalLength + packetLength;
      if (newTotalLength > 18713) {
        break;
      }
      offset += packetLength;
      totalLength = newTotalLength;
    }
    if (totalLength > 0)
    {
      in.skipBytes(totalLength);
      if ((in.isDirect()) && (this.wantsInboundHeapBuffer))
      {
        ByteBuf copy = ctx.alloc().heapBuffer(totalLength);
        try
        {
          copy.writeBytes(in, startOffset, totalLength);
          unwrap(ctx, copy, 0, totalLength);
        }
        finally
        {
          copy.release();
        }
      }
      else
      {
        unwrap(ctx, in, startOffset, totalLength);
      }
    }
    if (nonSslRecord)
    {
      NotSslRecordException e = new NotSslRecordException("not an SSL/TLS record: " + ByteBufUtil.hexDump(in));
      
      in.skipBytes(in.readableBytes());
      ctx.fireExceptionCaught(e);
      setHandshakeFailure(ctx, e);
    }
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception
  {
    if (this.needsFlush)
    {
      this.needsFlush = false;
      ctx.flush();
    }
    if ((!this.handshakePromise.isDone()) && (!ctx.channel().config().isAutoRead())) {
      ctx.read();
    }
    ctx.fireChannelReadComplete();
  }
  
  private void unwrapNonAppData(ChannelHandlerContext ctx)
    throws SSLException
  {
    unwrap(ctx, Unpooled.EMPTY_BUFFER, 0, 0);
  }
  
  private void unwrap(ChannelHandlerContext ctx, ByteBuf packet, int offset, int length)
    throws SSLException
  {
    boolean wrapLater = false;
    boolean notifyClosure = false;
    ByteBuf decodeOut = allocate(ctx, length);
    try
    {
      for (;;)
      {
        SSLEngineResult result = unwrap(this.engine, packet, offset, length, decodeOut);
        SSLEngineResult.Status status = result.getStatus();
        SSLEngineResult.HandshakeStatus handshakeStatus = result.getHandshakeStatus();
        int produced = result.bytesProduced();
        int consumed = result.bytesConsumed();
        
        offset += consumed;
        length -= consumed;
        if (status == SSLEngineResult.Status.CLOSED) {
          notifyClosure = true;
        }
        switch (handshakeStatus)
        {
        case NEED_UNWRAP: 
          break;
        case NEED_WRAP: 
          wrapNonAppData(ctx, true);
          break;
        case NEED_TASK: 
          runDelegatedTasks();
          break;
        case FINISHED: 
          setHandshakeSuccess();
          wrapLater = true;
          break;
        case NOT_HANDSHAKING: 
          if (setHandshakeSuccessIfStillHandshaking())
          {
            wrapLater = true;
          }
          else if (this.flushedBeforeHandshake)
          {
            this.flushedBeforeHandshake = false;
            wrapLater = true;
          }
          break;
        default: 
          throw new IllegalStateException("unknown handshake status: " + handshakeStatus);
          if ((status == SSLEngineResult.Status.BUFFER_UNDERFLOW) || ((consumed == 0) && (produced == 0))) {
            break label236;
          }
        }
      }
      label236:
      if (wrapLater) {
        wrap(ctx, true);
      }
      if (notifyClosure) {
        this.sslCloseFuture.trySuccess(ctx.channel());
      }
    }
    catch (SSLException e)
    {
      setHandshakeFailure(ctx, e);
      throw e;
    }
    finally
    {
      if (decodeOut.isReadable()) {
        ctx.fireChannelRead(decodeOut);
      } else {
        decodeOut.release();
      }
    }
  }
  
  private void runDelegatedTasks()
  {
    for (;;)
    {
      Runnable task = this.engine.getDelegatedTask();
      if (task == null) {
        break;
      }
      task.run();
    }
  }
  
  private boolean setHandshakeSuccessIfStillHandshaking()
  {
    if (!this.handshakePromise.isDone())
    {
      setHandshakeSuccess();
      return true;
    }
    return false;
  }
  
  private void setHandshakeSuccess()
  {
    String cipherSuite = String.valueOf(this.engine.getSession().getCipherSuite());
    if ((!this.wantsDirectBuffer) && ((cipherSuite.contains("_GCM_")) || (cipherSuite.contains("-GCM-")))) {
      this.wantsInboundHeapBuffer = true;
    }
    this.handshakePromise.trySuccess(this.ctx.channel());
    if (logger.isDebugEnabled()) {
      logger.debug("{} HANDSHAKEN: {}", this.ctx.channel(), this.engine.getSession().getCipherSuite());
    }
    this.ctx.fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);
    if ((this.readDuringHandshake) && (!this.ctx.channel().config().isAutoRead()))
    {
      this.readDuringHandshake = false;
      this.ctx.read();
    }
  }
  
  private void setHandshakeFailure(ChannelHandlerContext ctx, Throwable cause)
  {
    this.engine.closeOutbound();
    try
    {
      this.engine.closeInbound();
    }
    catch (SSLException e)
    {
      String msg = e.getMessage();
      if ((msg == null) || (!msg.contains("possible truncation attack"))) {
        logger.debug("{} SSLEngine.closeInbound() raised an exception.", ctx.channel(), e);
      }
    }
    notifyHandshakeFailure(cause);
    this.pendingUnencryptedWrites.removeAndFailAll(cause);
  }
  
  private void notifyHandshakeFailure(Throwable cause)
  {
    if (this.handshakePromise.tryFailure(cause))
    {
      this.ctx.fireUserEventTriggered(new SslHandshakeCompletionEvent(cause));
      this.ctx.close();
    }
  }
  
  private void closeOutboundAndChannel(ChannelHandlerContext ctx, ChannelPromise promise, boolean disconnect)
    throws Exception
  {
    if (!ctx.channel().isActive())
    {
      if (disconnect) {
        ctx.disconnect(promise);
      } else {
        ctx.close(promise);
      }
      return;
    }
    this.engine.closeOutbound();
    
    ChannelPromise closeNotifyFuture = ctx.newPromise();
    write(ctx, Unpooled.EMPTY_BUFFER, closeNotifyFuture);
    flush(ctx);
    safeClose(ctx, closeNotifyFuture, promise);
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {
    this.ctx = ctx;
    this.pendingUnencryptedWrites = new PendingWriteQueue(ctx);
    if ((ctx.channel().isActive()) && (this.engine.getUseClientMode())) {
      handshake(null);
    }
  }
  
  public Future<Channel> renegotiate()
  {
    ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      throw new IllegalStateException();
    }
    return renegotiate(ctx.executor().newPromise());
  }
  
  public Future<Channel> renegotiate(final Promise<Channel> promise)
  {
    if (promise == null) {
      throw new NullPointerException("promise");
    }
    ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      throw new IllegalStateException();
    }
    EventExecutor executor = ctx.executor();
    if (!executor.inEventLoop())
    {
      executor.execute(new OneTimeTask()
      {
        public void run()
        {
          SslHandler.this.handshake(promise);
        }
      });
      return promise;
    }
    handshake(promise);
    return promise;
  }
  
  private void handshake(final Promise<Channel> newHandshakePromise)
  {
    final Promise<Channel> p;
    if (newHandshakePromise != null)
    {
      Promise<Channel> oldHandshakePromise = this.handshakePromise;
      if (!oldHandshakePromise.isDone())
      {
        oldHandshakePromise.addListener(new FutureListener()
        {
          public void operationComplete(Future<Channel> future)
            throws Exception
          {
            if (future.isSuccess()) {
              newHandshakePromise.setSuccess(future.getNow());
            } else {
              newHandshakePromise.setFailure(future.cause());
            }
          }
        }); return;
      }
      Promise<Channel> p;
      this.handshakePromise = (p = newHandshakePromise);
    }
    else
    {
      p = this.handshakePromise;
      assert (!p.isDone());
    }
    ChannelHandlerContext ctx = this.ctx;
    try
    {
      this.engine.beginHandshake();
      wrapNonAppData(ctx, false);
      ctx.flush();
    }
    catch (Exception e)
    {
      notifyHandshakeFailure(e);
    }
    long handshakeTimeoutMillis = this.handshakeTimeoutMillis;
    if ((handshakeTimeoutMillis <= 0L) || (p.isDone())) {
      return;
    }
    final ScheduledFuture<?> timeoutFuture = ctx.executor().schedule(new Runnable()
    {
      public void run()
      {
        if (p.isDone()) {
          return;
        }
        SslHandler.this.notifyHandshakeFailure(SslHandler.HANDSHAKE_TIMED_OUT);
      }
    }, handshakeTimeoutMillis, TimeUnit.MILLISECONDS);
    
    p.addListener(new FutureListener()
    {
      public void operationComplete(Future<Channel> f)
        throws Exception
      {
        timeoutFuture.cancel(false);
      }
    });
  }
  
  public void channelActive(ChannelHandlerContext ctx)
    throws Exception
  {
    if ((!this.startTls) && (this.engine.getUseClientMode())) {
      handshake(null);
    }
    ctx.fireChannelActive();
  }
  
  private void safeClose(final ChannelHandlerContext ctx, ChannelFuture flushFuture, final ChannelPromise promise)
  {
    if (!ctx.channel().isActive())
    {
      ctx.close(promise); return;
    }
    ScheduledFuture<?> timeoutFuture;
    final ScheduledFuture<?> timeoutFuture;
    if (this.closeNotifyTimeoutMillis > 0L) {
      timeoutFuture = ctx.executor().schedule(new Runnable()
      {
        public void run()
        {
          SslHandler.logger.warn("{} Last write attempt timed out; force-closing the connection.", ctx.channel());
          ctx.close(promise);
        }
      }, this.closeNotifyTimeoutMillis, TimeUnit.MILLISECONDS);
    } else {
      timeoutFuture = null;
    }
    flushFuture.addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture f)
        throws Exception
      {
        if (timeoutFuture != null) {
          timeoutFuture.cancel(false);
        }
        ctx.close(promise);
      }
    });
  }
  
  private ByteBuf allocate(ChannelHandlerContext ctx, int capacity)
  {
    ByteBufAllocator alloc = ctx.alloc();
    if (this.wantsDirectBuffer) {
      return alloc.directBuffer(capacity);
    }
    return alloc.buffer(capacity);
  }
  
  private ByteBuf allocateOutNetBuf(ChannelHandlerContext ctx, int pendingBytes)
  {
    if (this.wantsLargeOutboundNetworkBuffer) {
      return allocate(ctx, this.maxPacketBufferSize);
    }
    return allocate(ctx, Math.min(pendingBytes + 2329, this.maxPacketBufferSize));
  }
  
  /* Error */
  private SSLEngineResult wrap(ByteBufAllocator alloc, SSLEngine engine, ByteBuf in, ByteBuf out)
    throws SSLException
  {
    // Byte code:
    //   0: aconst_null
    //   1: astore 5
    //   3: aload_3
    //   4: invokevirtual 102	io/netty/buffer/ByteBuf:readerIndex	()I
    //   7: istore 6
    //   9: aload_3
    //   10: invokevirtual 75	io/netty/buffer/ByteBuf:readableBytes	()I
    //   13: istore 7
    //   15: aload_3
    //   16: invokevirtual 103	io/netty/buffer/ByteBuf:isDirect	()Z
    //   19: ifne +10 -> 29
    //   22: aload_0
    //   23: getfield 29	io/netty/handler/ssl/SslHandler:wantsDirectBuffer	Z
    //   26: ifne +48 -> 74
    //   29: aload_3
    //   30: instanceof 104
    //   33: ifne +32 -> 65
    //   36: aload_3
    //   37: invokevirtual 105	io/netty/buffer/ByteBuf:nioBufferCount	()I
    //   40: iconst_1
    //   41: if_icmpne +24 -> 65
    //   44: aload_0
    //   45: getfield 10	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   48: astore 8
    //   50: aload 8
    //   52: iconst_0
    //   53: aload_3
    //   54: iload 6
    //   56: iload 7
    //   58: invokevirtual 106	io/netty/buffer/ByteBuf:internalNioBuffer	(II)Ljava/nio/ByteBuffer;
    //   61: aastore
    //   62: goto +51 -> 113
    //   65: aload_3
    //   66: invokevirtual 107	io/netty/buffer/ByteBuf:nioBuffers	()[Ljava/nio/ByteBuffer;
    //   69: astore 8
    //   71: goto +42 -> 113
    //   74: aload_1
    //   75: iload 7
    //   77: invokeinterface 108 2 0
    //   82: astore 5
    //   84: aload 5
    //   86: aload_3
    //   87: iload 6
    //   89: iload 7
    //   91: invokevirtual 109	io/netty/buffer/ByteBuf:writeBytes	(Lio/netty/buffer/ByteBuf;II)Lio/netty/buffer/ByteBuf;
    //   94: pop
    //   95: aload_0
    //   96: getfield 10	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   99: astore 8
    //   101: aload 8
    //   103: iconst_0
    //   104: aload 5
    //   106: iconst_0
    //   107: iload 7
    //   109: invokevirtual 106	io/netty/buffer/ByteBuf:internalNioBuffer	(II)Ljava/nio/ByteBuffer;
    //   112: aastore
    //   113: aload 4
    //   115: aload 4
    //   117: invokevirtual 110	io/netty/buffer/ByteBuf:writerIndex	()I
    //   120: aload 4
    //   122: invokevirtual 111	io/netty/buffer/ByteBuf:writableBytes	()I
    //   125: invokevirtual 112	io/netty/buffer/ByteBuf:nioBuffer	(II)Ljava/nio/ByteBuffer;
    //   128: astore 9
    //   130: aload_2
    //   131: aload 8
    //   133: aload 9
    //   135: invokevirtual 113	javax/net/ssl/SSLEngine:wrap	([Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Ljavax/net/ssl/SSLEngineResult;
    //   138: astore 10
    //   140: aload_3
    //   141: aload 10
    //   143: invokevirtual 114	javax/net/ssl/SSLEngineResult:bytesConsumed	()I
    //   146: invokevirtual 115	io/netty/buffer/ByteBuf:skipBytes	(I)Lio/netty/buffer/ByteBuf;
    //   149: pop
    //   150: aload 4
    //   152: aload 4
    //   154: invokevirtual 110	io/netty/buffer/ByteBuf:writerIndex	()I
    //   157: aload 10
    //   159: invokevirtual 100	javax/net/ssl/SSLEngineResult:bytesProduced	()I
    //   162: iadd
    //   163: invokevirtual 116	io/netty/buffer/ByteBuf:writerIndex	(I)Lio/netty/buffer/ByteBuf;
    //   166: pop
    //   167: getstatic 117	io/netty/handler/ssl/SslHandler$8:$SwitchMap$javax$net$ssl$SSLEngineResult$Status	[I
    //   170: aload 10
    //   172: invokevirtual 80	javax/net/ssl/SSLEngineResult:getStatus	()Ljavax/net/ssl/SSLEngineResult$Status;
    //   175: invokevirtual 118	javax/net/ssl/SSLEngineResult$Status:ordinal	()I
    //   178: iaload
    //   179: lookupswitch	default:+30->209, 1:+17->196
    //   196: aload 4
    //   198: aload_0
    //   199: getfield 27	io/netty/handler/ssl/SslHandler:maxPacketBufferSize	I
    //   202: invokevirtual 119	io/netty/buffer/ByteBuf:ensureWritable	(I)Lio/netty/buffer/ByteBuf;
    //   205: pop
    //   206: goto +28 -> 234
    //   209: aload 10
    //   211: astore 11
    //   213: aload_0
    //   214: getfield 10	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   217: iconst_0
    //   218: aconst_null
    //   219: aastore
    //   220: aload 5
    //   222: ifnull +9 -> 231
    //   225: aload 5
    //   227: invokevirtual 96	io/netty/buffer/ByteBuf:release	()Z
    //   230: pop
    //   231: aload 11
    //   233: areturn
    //   234: goto -121 -> 113
    //   237: astore 12
    //   239: aload_0
    //   240: getfield 10	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   243: iconst_0
    //   244: aconst_null
    //   245: aastore
    //   246: aload 5
    //   248: ifnull +9 -> 257
    //   251: aload 5
    //   253: invokevirtual 96	io/netty/buffer/ByteBuf:release	()Z
    //   256: pop
    //   257: aload 12
    //   259: athrow
    // Line number table:
    //   Java source line #574	-> byte code offset #0
    //   Java source line #576	-> byte code offset #3
    //   Java source line #577	-> byte code offset #9
    //   Java source line #582	-> byte code offset #15
    //   Java source line #587	-> byte code offset #29
    //   Java source line #588	-> byte code offset #44
    //   Java source line #591	-> byte code offset #50
    //   Java source line #593	-> byte code offset #65
    //   Java source line #599	-> byte code offset #74
    //   Java source line #600	-> byte code offset #84
    //   Java source line #601	-> byte code offset #95
    //   Java source line #602	-> byte code offset #101
    //   Java source line #606	-> byte code offset #113
    //   Java source line #607	-> byte code offset #130
    //   Java source line #608	-> byte code offset #140
    //   Java source line #609	-> byte code offset #150
    //   Java source line #611	-> byte code offset #167
    //   Java source line #613	-> byte code offset #196
    //   Java source line #614	-> byte code offset #206
    //   Java source line #616	-> byte code offset #209
    //   Java source line #621	-> byte code offset #213
    //   Java source line #623	-> byte code offset #220
    //   Java source line #624	-> byte code offset #225
    //   Java source line #618	-> byte code offset #234
    //   Java source line #621	-> byte code offset #237
    //   Java source line #623	-> byte code offset #246
    //   Java source line #624	-> byte code offset #251
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	260	0	this	SslHandler
    //   0	260	1	alloc	ByteBufAllocator
    //   0	260	2	engine	SSLEngine
    //   0	260	3	in	ByteBuf
    //   0	260	4	out	ByteBuf
    //   1	251	5	newDirectIn	ByteBuf
    //   7	81	6	readerIndex	int
    //   13	95	7	readableBytes	int
    //   48	3	8	in0	ByteBuffer[]
    //   69	3	8	in0	ByteBuffer[]
    //   99	33	8	in0	ByteBuffer[]
    //   128	6	9	out0	ByteBuffer
    //   138	72	10	result	SSLEngineResult
    //   211	21	11	localSSLEngineResult1	SSLEngineResult
    //   237	21	12	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   3	213	237	finally
    //   234	239	237	finally
  }
  
  /* Error */
  private SSLEngineResult unwrap(SSLEngine engine, ByteBuf in, int readerIndex, int len, ByteBuf out)
    throws SSLException
  {
    // Byte code:
    //   0: aload_2
    //   1: invokevirtual 105	io/netty/buffer/ByteBuf:nioBufferCount	()I
    //   4: istore 6
    //   6: aload_1
    //   7: instanceof 28
    //   10: ifeq +236 -> 246
    //   13: iload 6
    //   15: iconst_1
    //   16: if_icmple +230 -> 246
    //   19: aload_1
    //   20: checkcast 28	io/netty/handler/ssl/OpenSslEngine
    //   23: astore 7
    //   25: iconst_0
    //   26: istore 8
    //   28: aload_2
    //   29: iload_3
    //   30: iload 4
    //   32: invokevirtual 183	io/netty/buffer/ByteBuf:nioBuffers	(II)[Ljava/nio/ByteBuffer;
    //   35: astore 9
    //   37: aload 5
    //   39: invokevirtual 110	io/netty/buffer/ByteBuf:writerIndex	()I
    //   42: istore 10
    //   44: aload 5
    //   46: invokevirtual 111	io/netty/buffer/ByteBuf:writableBytes	()I
    //   49: istore 11
    //   51: aload 5
    //   53: invokevirtual 105	io/netty/buffer/ByteBuf:nioBufferCount	()I
    //   56: iconst_1
    //   57: if_icmpne +17 -> 74
    //   60: aload 5
    //   62: iload 10
    //   64: iload 11
    //   66: invokevirtual 106	io/netty/buffer/ByteBuf:internalNioBuffer	(II)Ljava/nio/ByteBuffer;
    //   69: astore 12
    //   71: goto +14 -> 85
    //   74: aload 5
    //   76: iload 10
    //   78: iload 11
    //   80: invokevirtual 112	io/netty/buffer/ByteBuf:nioBuffer	(II)Ljava/nio/ByteBuffer;
    //   83: astore 12
    //   85: aload_0
    //   86: getfield 10	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   89: iconst_0
    //   90: aload 12
    //   92: aastore
    //   93: aload 7
    //   95: aload 9
    //   97: aload_0
    //   98: getfield 10	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   101: invokevirtual 184	io/netty/handler/ssl/OpenSslEngine:unwrap	([Ljava/nio/ByteBuffer;[Ljava/nio/ByteBuffer;)Ljavax/net/ssl/SSLEngineResult;
    //   104: astore 13
    //   106: aload 5
    //   108: aload 5
    //   110: invokevirtual 110	io/netty/buffer/ByteBuf:writerIndex	()I
    //   113: aload 13
    //   115: invokevirtual 100	javax/net/ssl/SSLEngineResult:bytesProduced	()I
    //   118: iadd
    //   119: invokevirtual 116	io/netty/buffer/ByteBuf:writerIndex	(I)Lio/netty/buffer/ByteBuf;
    //   122: pop
    //   123: getstatic 117	io/netty/handler/ssl/SslHandler$8:$SwitchMap$javax$net$ssl$SSLEngineResult$Status	[I
    //   126: aload 13
    //   128: invokevirtual 80	javax/net/ssl/SSLEngineResult:getStatus	()Ljavax/net/ssl/SSLEngineResult$Status;
    //   131: invokevirtual 118	javax/net/ssl/SSLEngineResult$Status:ordinal	()I
    //   134: iaload
    //   135: lookupswitch	default:+82->217, 1:+17->152
    //   152: aload_1
    //   153: invokevirtual 25	javax/net/ssl/SSLEngine:getSession	()Ljavax/net/ssl/SSLSession;
    //   156: invokeinterface 185 1 0
    //   161: istore 14
    //   163: iload 8
    //   165: iinc 8 1
    //   168: lookupswitch	default:+38->206, 0:+20->188
    //   188: aload 5
    //   190: iload 14
    //   192: aload_2
    //   193: invokevirtual 75	io/netty/buffer/ByteBuf:readableBytes	()I
    //   196: invokestatic 186	java/lang/Math:min	(II)I
    //   199: invokevirtual 119	io/netty/buffer/ByteBuf:ensureWritable	(I)Lio/netty/buffer/ByteBuf;
    //   202: pop
    //   203: goto +28 -> 231
    //   206: aload 5
    //   208: iload 14
    //   210: invokevirtual 119	io/netty/buffer/ByteBuf:ensureWritable	(I)Lio/netty/buffer/ByteBuf;
    //   213: pop
    //   214: goto +17 -> 231
    //   217: aload 13
    //   219: astore 15
    //   221: aload_0
    //   222: getfield 10	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   225: iconst_0
    //   226: aconst_null
    //   227: aastore
    //   228: aload 15
    //   230: areturn
    //   231: goto -194 -> 37
    //   234: astore 16
    //   236: aload_0
    //   237: getfield 10	io/netty/handler/ssl/SslHandler:singleBuffer	[Ljava/nio/ByteBuffer;
    //   240: iconst_0
    //   241: aconst_null
    //   242: aastore
    //   243: aload 16
    //   245: athrow
    //   246: iconst_0
    //   247: istore 7
    //   249: iload 6
    //   251: iconst_1
    //   252: if_icmpne +15 -> 267
    //   255: aload_2
    //   256: iload_3
    //   257: iload 4
    //   259: invokevirtual 106	io/netty/buffer/ByteBuf:internalNioBuffer	(II)Ljava/nio/ByteBuffer;
    //   262: astore 8
    //   264: goto +12 -> 276
    //   267: aload_2
    //   268: iload_3
    //   269: iload 4
    //   271: invokevirtual 112	io/netty/buffer/ByteBuf:nioBuffer	(II)Ljava/nio/ByteBuffer;
    //   274: astore 8
    //   276: aload 5
    //   278: invokevirtual 110	io/netty/buffer/ByteBuf:writerIndex	()I
    //   281: istore 9
    //   283: aload 5
    //   285: invokevirtual 111	io/netty/buffer/ByteBuf:writableBytes	()I
    //   288: istore 10
    //   290: aload 5
    //   292: invokevirtual 105	io/netty/buffer/ByteBuf:nioBufferCount	()I
    //   295: iconst_1
    //   296: if_icmpne +17 -> 313
    //   299: aload 5
    //   301: iload 9
    //   303: iload 10
    //   305: invokevirtual 106	io/netty/buffer/ByteBuf:internalNioBuffer	(II)Ljava/nio/ByteBuffer;
    //   308: astore 11
    //   310: goto +14 -> 324
    //   313: aload 5
    //   315: iload 9
    //   317: iload 10
    //   319: invokevirtual 112	io/netty/buffer/ByteBuf:nioBuffer	(II)Ljava/nio/ByteBuffer;
    //   322: astore 11
    //   324: aload_1
    //   325: aload 8
    //   327: aload 11
    //   329: invokevirtual 187	javax/net/ssl/SSLEngine:unwrap	(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Ljavax/net/ssl/SSLEngineResult;
    //   332: astore 12
    //   334: aload 5
    //   336: aload 5
    //   338: invokevirtual 110	io/netty/buffer/ByteBuf:writerIndex	()I
    //   341: aload 12
    //   343: invokevirtual 100	javax/net/ssl/SSLEngineResult:bytesProduced	()I
    //   346: iadd
    //   347: invokevirtual 116	io/netty/buffer/ByteBuf:writerIndex	(I)Lio/netty/buffer/ByteBuf;
    //   350: pop
    //   351: getstatic 117	io/netty/handler/ssl/SslHandler$8:$SwitchMap$javax$net$ssl$SSLEngineResult$Status	[I
    //   354: aload 12
    //   356: invokevirtual 80	javax/net/ssl/SSLEngineResult:getStatus	()Ljavax/net/ssl/SSLEngineResult$Status;
    //   359: invokevirtual 118	javax/net/ssl/SSLEngineResult$Status:ordinal	()I
    //   362: iaload
    //   363: lookupswitch	default:+82->445, 1:+17->380
    //   380: aload_1
    //   381: invokevirtual 25	javax/net/ssl/SSLEngine:getSession	()Ljavax/net/ssl/SSLSession;
    //   384: invokeinterface 185 1 0
    //   389: istore 13
    //   391: iload 7
    //   393: iinc 7 1
    //   396: lookupswitch	default:+38->434, 0:+20->416
    //   416: aload 5
    //   418: iload 13
    //   420: aload_2
    //   421: invokevirtual 75	io/netty/buffer/ByteBuf:readableBytes	()I
    //   424: invokestatic 186	java/lang/Math:min	(II)I
    //   427: invokevirtual 119	io/netty/buffer/ByteBuf:ensureWritable	(I)Lio/netty/buffer/ByteBuf;
    //   430: pop
    //   431: goto +17 -> 448
    //   434: aload 5
    //   436: iload 13
    //   438: invokevirtual 119	io/netty/buffer/ByteBuf:ensureWritable	(I)Lio/netty/buffer/ByteBuf;
    //   441: pop
    //   442: goto +6 -> 448
    //   445: aload 12
    //   447: areturn
    //   448: goto -172 -> 276
    // Line number table:
    //   Java source line #1010	-> byte code offset #0
    //   Java source line #1011	-> byte code offset #6
    //   Java source line #1017	-> byte code offset #19
    //   Java source line #1018	-> byte code offset #25
    //   Java source line #1019	-> byte code offset #28
    //   Java source line #1022	-> byte code offset #37
    //   Java source line #1023	-> byte code offset #44
    //   Java source line #1025	-> byte code offset #51
    //   Java source line #1026	-> byte code offset #60
    //   Java source line #1028	-> byte code offset #74
    //   Java source line #1030	-> byte code offset #85
    //   Java source line #1031	-> byte code offset #93
    //   Java source line #1032	-> byte code offset #106
    //   Java source line #1033	-> byte code offset #123
    //   Java source line #1035	-> byte code offset #152
    //   Java source line #1036	-> byte code offset #163
    //   Java source line #1038	-> byte code offset #188
    //   Java source line #1039	-> byte code offset #203
    //   Java source line #1041	-> byte code offset #206
    //   Java source line #1043	-> byte code offset #214
    //   Java source line #1045	-> byte code offset #217
    //   Java source line #1049	-> byte code offset #221
    //   Java source line #1047	-> byte code offset #231
    //   Java source line #1049	-> byte code offset #234
    //   Java source line #1052	-> byte code offset #246
    //   Java source line #1054	-> byte code offset #249
    //   Java source line #1056	-> byte code offset #255
    //   Java source line #1060	-> byte code offset #267
    //   Java source line #1063	-> byte code offset #276
    //   Java source line #1064	-> byte code offset #283
    //   Java source line #1066	-> byte code offset #290
    //   Java source line #1067	-> byte code offset #299
    //   Java source line #1069	-> byte code offset #313
    //   Java source line #1071	-> byte code offset #324
    //   Java source line #1072	-> byte code offset #334
    //   Java source line #1073	-> byte code offset #351
    //   Java source line #1075	-> byte code offset #380
    //   Java source line #1076	-> byte code offset #391
    //   Java source line #1078	-> byte code offset #416
    //   Java source line #1079	-> byte code offset #431
    //   Java source line #1081	-> byte code offset #434
    //   Java source line #1083	-> byte code offset #442
    //   Java source line #1085	-> byte code offset #445
    //   Java source line #1087	-> byte code offset #448
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	451	0	this	SslHandler
    //   0	451	1	engine	SSLEngine
    //   0	451	2	in	ByteBuf
    //   0	451	3	readerIndex	int
    //   0	451	4	len	int
    //   0	451	5	out	ByteBuf
    //   4	246	6	nioBufferCount	int
    //   23	71	7	opensslEngine	OpenSslEngine
    //   247	145	7	overflows	int
    //   26	138	8	overflows	int
    //   262	3	8	in0	ByteBuffer
    //   274	52	8	in0	ByteBuffer
    //   35	61	9	in0	ByteBuffer[]
    //   281	35	9	writerIndex	int
    //   42	35	10	writerIndex	int
    //   288	30	10	writableBytes	int
    //   49	30	11	writableBytes	int
    //   308	3	11	out0	ByteBuffer
    //   322	6	11	out0	ByteBuffer
    //   69	3	12	out0	ByteBuffer
    //   83	8	12	out0	ByteBuffer
    //   332	114	12	result	SSLEngineResult
    //   104	114	13	result	SSLEngineResult
    //   389	48	13	max	int
    //   161	48	14	max	int
    //   219	10	15	localSSLEngineResult1	SSLEngineResult
    //   234	10	16	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   37	221	234	finally
    //   231	236	234	finally
  }
  
  private final class LazyChannelPromise
    extends DefaultPromise<Channel>
  {
    private LazyChannelPromise() {}
    
    protected EventExecutor executor()
    {
      if (SslHandler.this.ctx == null) {
        throw new IllegalStateException();
      }
      return SslHandler.this.ctx.executor();
    }
  }
}
