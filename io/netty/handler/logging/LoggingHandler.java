package io.netty.handler.logging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;

@ChannelHandler.Sharable
public class LoggingHandler
  extends ChannelHandlerAdapter
{
  private static final LogLevel DEFAULT_LEVEL = LogLevel.DEBUG;
  private static final String NEWLINE = StringUtil.NEWLINE;
  private static final String[] BYTE2HEX = new String['Ā'];
  private static final String[] HEXPADDING = new String[16];
  private static final String[] BYTEPADDING = new String[16];
  private static final char[] BYTE2CHAR = new char['Ā'];
  private static final String[] HEXDUMP_ROWPREFIXES = new String['က'];
  protected final InternalLogger logger;
  protected final InternalLogLevel internalLevel;
  private final LogLevel level;
  
  static
  {
    for (int i = 0; i < BYTE2HEX.length; i++) {
      BYTE2HEX[i] = (' ' + StringUtil.byteToHexStringPadded(i));
    }
    for (i = 0; i < HEXPADDING.length; i++)
    {
      int padding = HEXPADDING.length - i;
      StringBuilder buf = new StringBuilder(padding * 3);
      for (int j = 0; j < padding; j++) {
        buf.append("   ");
      }
      HEXPADDING[i] = buf.toString();
    }
    for (i = 0; i < BYTEPADDING.length; i++)
    {
      int padding = BYTEPADDING.length - i;
      StringBuilder buf = new StringBuilder(padding);
      for (int j = 0; j < padding; j++) {
        buf.append(' ');
      }
      BYTEPADDING[i] = buf.toString();
    }
    for (i = 0; i < BYTE2CHAR.length; i++) {
      if ((i <= 31) || (i >= 127)) {
        BYTE2CHAR[i] = '.';
      } else {
        BYTE2CHAR[i] = ((char)i);
      }
    }
    for (i = 0; i < HEXDUMP_ROWPREFIXES.length; i++)
    {
      StringBuilder buf = new StringBuilder(12);
      buf.append(NEWLINE);
      buf.append(Long.toHexString(i << 4 & 0xFFFFFFFF | 0x100000000));
      buf.setCharAt(buf.length() - 9, '|');
      buf.append('|');
      HEXDUMP_ROWPREFIXES[i] = buf.toString();
    }
  }
  
  public LoggingHandler()
  {
    this(DEFAULT_LEVEL);
  }
  
  public LoggingHandler(LogLevel level)
  {
    if (level == null) {
      throw new NullPointerException("level");
    }
    this.logger = InternalLoggerFactory.getInstance(getClass());
    this.level = level;
    this.internalLevel = level.toInternalLevel();
  }
  
  public LoggingHandler(Class<?> clazz)
  {
    this(clazz, DEFAULT_LEVEL);
  }
  
  public LoggingHandler(Class<?> clazz, LogLevel level)
  {
    if (clazz == null) {
      throw new NullPointerException("clazz");
    }
    if (level == null) {
      throw new NullPointerException("level");
    }
    this.logger = InternalLoggerFactory.getInstance(clazz);
    this.level = level;
    this.internalLevel = level.toInternalLevel();
  }
  
  public LoggingHandler(String name)
  {
    this(name, DEFAULT_LEVEL);
  }
  
  public LoggingHandler(String name, LogLevel level)
  {
    if (name == null) {
      throw new NullPointerException("name");
    }
    if (level == null) {
      throw new NullPointerException("level");
    }
    this.logger = InternalLoggerFactory.getInstance(name);
    this.level = level;
    this.internalLevel = level.toInternalLevel();
  }
  
  public LogLevel level()
  {
    return this.level;
  }
  
  public void channelRegistered(ChannelHandlerContext ctx)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "REGISTERED"));
    }
    ctx.fireChannelRegistered();
  }
  
  public void channelUnregistered(ChannelHandlerContext ctx)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "UNREGISTERED"));
    }
    ctx.fireChannelUnregistered();
  }
  
  public void channelActive(ChannelHandlerContext ctx)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "ACTIVE"));
    }
    ctx.fireChannelActive();
  }
  
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "INACTIVE"));
    }
    ctx.fireChannelInactive();
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "EXCEPTION", cause), cause);
    }
    ctx.fireExceptionCaught(cause);
  }
  
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "USER_EVENT", evt));
    }
    ctx.fireUserEventTriggered(evt);
  }
  
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "BIND", localAddress));
    }
    ctx.bind(localAddress, promise);
  }
  
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "CONNECT", remoteAddress, localAddress));
    }
    ctx.connect(remoteAddress, localAddress, promise);
  }
  
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "DISCONNECT"));
    }
    ctx.disconnect(promise);
  }
  
  public void close(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "CLOSE"));
    }
    ctx.close(promise);
  }
  
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "DEREGISTER"));
    }
    ctx.deregister(promise);
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "RECEIVED", msg));
    }
    ctx.fireChannelRead(msg);
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "WRITE", msg));
    }
    ctx.write(msg, promise);
  }
  
  public void flush(ChannelHandlerContext ctx)
    throws Exception
  {
    if (this.logger.isEnabled(this.internalLevel)) {
      this.logger.log(this.internalLevel, format(ctx, "FLUSH"));
    }
    ctx.flush();
  }
  
  protected String format(ChannelHandlerContext ctx, String eventName)
  {
    String chStr = ctx.channel().toString();
    return chStr.length() + 1 + eventName.length() + chStr + ' ' + eventName;
  }
  
  protected String format(ChannelHandlerContext ctx, String eventName, Object arg)
  {
    if ((arg instanceof ByteBuf)) {
      return formatByteBuf(ctx, eventName, (ByteBuf)arg);
    }
    if ((arg instanceof ByteBufHolder)) {
      return formatByteBufHolder(ctx, eventName, (ByteBufHolder)arg);
    }
    return formatSimple(ctx, eventName, arg);
  }
  
  protected String format(ChannelHandlerContext ctx, String eventName, Object firstArg, Object secondArg)
  {
    if (secondArg == null) {
      return formatSimple(ctx, eventName, firstArg);
    }
    String chStr = ctx.channel().toString();
    String arg1Str = String.valueOf(firstArg);
    String arg2Str = secondArg.toString();
    StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName + 2 + arg1Str.length() + 2 + arg2Str.length());
    
    buf.append(chStr).append(' ').append(eventName).append(": ").append(arg1Str).append(", ").append(arg2Str);
    return buf.toString();
  }
  
  private static String formatByteBuf(ChannelHandlerContext ctx, String eventName, ByteBuf msg)
  {
    String chStr = ctx.channel().toString();
    int length = msg.readableBytes();
    if (length == 0)
    {
      StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 4);
      buf.append(chStr).append(' ').append(eventName).append(": 0B");
      return buf.toString();
    }
    int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
    StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + 10 + 1 + 2 + rows * 80);
    
    buf.append(chStr).append(' ').append(eventName).append(": ").append(length).append('B');
    appendHexDump(buf, msg);
    
    return buf.toString();
  }
  
  private static String formatByteBufHolder(ChannelHandlerContext ctx, String eventName, ByteBufHolder msg)
  {
    String chStr = ctx.channel().toString();
    String msgStr = msg.toString();
    ByteBuf content = msg.content();
    int length = content.readableBytes();
    if (length == 0)
    {
      StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + msgStr.length() + 4);
      buf.append(chStr).append(' ').append(eventName).append(", ").append(msgStr).append(", 0B");
      return buf.toString();
    }
    int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
    StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + msgStr.length() + 2 + 10 + 1 + 2 + rows * 80);
    
    buf.append(chStr).append(' ').append(eventName).append(": ").append(msgStr).append(", ").append(length).append('B');
    
    appendHexDump(buf, content);
    
    return buf.toString();
  }
  
  protected static void appendHexDump(StringBuilder dump, ByteBuf buf)
  {
    dump.append(NEWLINE + "         +-------------------------------------------------+" + NEWLINE + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |" + NEWLINE + "+--------+-------------------------------------------------+----------------+");
    
    int startIndex = buf.readerIndex();
    int endIndex = buf.writerIndex();
    int length = endIndex - startIndex;
    int fullRows = length >>> 4;
    int remainder = length & 0xF;
    for (int row = 0; row < fullRows; row++)
    {
      int rowStartIndex = row << 4;
      
      appendHexDumpRowPrefix(dump, row, rowStartIndex);
      
      int rowEndIndex = rowStartIndex + 16;
      for (int j = rowStartIndex; j < rowEndIndex; j++) {
        dump.append(BYTE2HEX[buf.getUnsignedByte(j)]);
      }
      dump.append(" |");
      for (int j = rowStartIndex; j < rowEndIndex; j++) {
        dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
      }
      dump.append('|');
    }
    if (remainder != 0)
    {
      int rowStartIndex = fullRows << 4;
      appendHexDumpRowPrefix(dump, fullRows, rowStartIndex);
      
      int rowEndIndex = rowStartIndex + remainder;
      for (int j = rowStartIndex; j < rowEndIndex; j++) {
        dump.append(BYTE2HEX[buf.getUnsignedByte(j)]);
      }
      dump.append(HEXPADDING[remainder]);
      dump.append(" |");
      for (int j = rowStartIndex; j < rowEndIndex; j++) {
        dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
      }
      dump.append(BYTEPADDING[remainder]);
      dump.append('|');
    }
    dump.append(NEWLINE + "+--------+-------------------------------------------------+----------------+");
  }
  
  private static void appendHexDumpRowPrefix(StringBuilder dump, int row, int rowStartIndex)
  {
    if (row < HEXDUMP_ROWPREFIXES.length)
    {
      dump.append(HEXDUMP_ROWPREFIXES[row]);
    }
    else
    {
      dump.append(NEWLINE);
      dump.append(Long.toHexString(rowStartIndex & 0xFFFFFFFF | 0x100000000));
      dump.setCharAt(dump.length() - 9, '|');
      dump.append('|');
    }
  }
  
  private static String formatSimple(ChannelHandlerContext ctx, String eventName, Object msg)
  {
    String chStr = ctx.channel().toString();
    String msgStr = String.valueOf(msg);
    StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + msgStr.length());
    return chStr + ' ' + eventName + ": " + msgStr;
  }
}
