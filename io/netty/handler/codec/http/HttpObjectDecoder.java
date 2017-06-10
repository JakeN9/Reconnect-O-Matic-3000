package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;
import java.util.List;

public abstract class HttpObjectDecoder
  extends ByteToMessageDecoder
{
  private static final String EMPTY_VALUE = "";
  private final int maxChunkSize;
  private final boolean chunkedSupported;
  protected final boolean validateHeaders;
  private final HeaderParser headerParser;
  private final LineParser lineParser;
  private HttpMessage message;
  private long chunkSize;
  private long contentLength = Long.MIN_VALUE;
  private volatile boolean resetRequested;
  private CharSequence name;
  private CharSequence value;
  private LastHttpContent trailer;
  
  private static enum State
  {
    SKIP_CONTROL_CHARS,  READ_INITIAL,  READ_HEADER,  READ_VARIABLE_LENGTH_CONTENT,  READ_FIXED_LENGTH_CONTENT,  READ_CHUNK_SIZE,  READ_CHUNKED_CONTENT,  READ_CHUNK_DELIMITER,  READ_CHUNK_FOOTER,  BAD_MESSAGE,  UPGRADED;
    
    private State() {}
  }
  
  private State currentState = State.SKIP_CONTROL_CHARS;
  
  protected HttpObjectDecoder()
  {
    this(4096, 8192, 8192, true);
  }
  
  protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported)
  {
    this(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported, true);
  }
  
  protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported, boolean validateHeaders)
  {
    if (maxInitialLineLength <= 0) {
      throw new IllegalArgumentException("maxInitialLineLength must be a positive integer: " + maxInitialLineLength);
    }
    if (maxHeaderSize <= 0) {
      throw new IllegalArgumentException("maxHeaderSize must be a positive integer: " + maxHeaderSize);
    }
    if (maxChunkSize <= 0) {
      throw new IllegalArgumentException("maxChunkSize must be a positive integer: " + maxChunkSize);
    }
    this.maxChunkSize = maxChunkSize;
    this.chunkedSupported = chunkedSupported;
    this.validateHeaders = validateHeaders;
    AppendableCharSequence seq = new AppendableCharSequence(128);
    this.lineParser = new LineParser(seq, maxInitialLineLength);
    this.headerParser = new HeaderParser(seq, maxHeaderSize);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
    throws Exception
  {
    if (this.resetRequested) {
      resetNow();
    }
    switch (this.currentState)
    {
    case SKIP_CONTROL_CHARS: 
      if (!skipControlCharacters(buffer)) {
        return;
      }
      this.currentState = State.READ_INITIAL;
    case READ_INITIAL: 
      try
      {
        AppendableCharSequence line = this.lineParser.parse(buffer);
        if (line == null) {
          return;
        }
        String[] initialLine = splitInitialLine(line);
        if (initialLine.length < 3)
        {
          this.currentState = State.SKIP_CONTROL_CHARS;
          return;
        }
        this.message = createMessage(initialLine);
        this.currentState = State.READ_HEADER;
      }
      catch (Exception e)
      {
        out.add(invalidMessage(e));
        return;
      }
    case READ_HEADER: 
      try
      {
        State nextState = readHeaders(buffer);
        if (nextState == null) {
          return;
        }
        this.currentState = nextState;
        switch (nextState)
        {
        case SKIP_CONTROL_CHARS: 
          out.add(this.message);
          out.add(LastHttpContent.EMPTY_LAST_CONTENT);
          resetNow();
          return;
        case READ_CHUNK_SIZE: 
          if (!this.chunkedSupported) {
            throw new IllegalArgumentException("Chunked messages not supported");
          }
          out.add(this.message);
          return;
        }
        long contentLength = contentLength();
        if ((contentLength == 0L) || ((contentLength == -1L) && (isDecodingRequest())))
        {
          out.add(this.message);
          out.add(LastHttpContent.EMPTY_LAST_CONTENT);
          resetNow();
          return;
        }
        assert ((nextState == State.READ_FIXED_LENGTH_CONTENT) || (nextState == State.READ_VARIABLE_LENGTH_CONTENT));
        
        out.add(this.message);
        if (nextState == State.READ_FIXED_LENGTH_CONTENT) {
          this.chunkSize = contentLength;
        }
        return;
      }
      catch (Exception e)
      {
        out.add(invalidMessage(e));
        return;
      }
    case READ_VARIABLE_LENGTH_CONTENT: 
      int toRead = Math.min(buffer.readableBytes(), this.maxChunkSize);
      if (toRead > 0)
      {
        ByteBuf content = buffer.readSlice(toRead).retain();
        out.add(new DefaultHttpContent(content));
      }
      return;
    case READ_FIXED_LENGTH_CONTENT: 
      int readLimit = buffer.readableBytes();
      if (readLimit == 0) {
        return;
      }
      int toRead = Math.min(readLimit, this.maxChunkSize);
      if (toRead > this.chunkSize) {
        toRead = (int)this.chunkSize;
      }
      ByteBuf content = buffer.readSlice(toRead).retain();
      this.chunkSize -= toRead;
      if (this.chunkSize == 0L)
      {
        out.add(new DefaultLastHttpContent(content, this.validateHeaders));
        resetNow();
      }
      else
      {
        out.add(new DefaultHttpContent(content));
      }
      return;
    case READ_CHUNK_SIZE: 
      try
      {
        AppendableCharSequence line = this.lineParser.parse(buffer);
        if (line == null) {
          return;
        }
        int chunkSize = getChunkSize(line.toString());
        this.chunkSize = chunkSize;
        if (chunkSize == 0)
        {
          this.currentState = State.READ_CHUNK_FOOTER;
          return;
        }
        this.currentState = State.READ_CHUNKED_CONTENT;
      }
      catch (Exception e)
      {
        out.add(invalidChunk(e));
        return;
      }
    case READ_CHUNKED_CONTENT: 
      assert (this.chunkSize <= 2147483647L);
      int toRead = Math.min((int)this.chunkSize, this.maxChunkSize);
      toRead = Math.min(toRead, buffer.readableBytes());
      if (toRead == 0) {
        return;
      }
      HttpContent chunk = new DefaultHttpContent(buffer.readSlice(toRead).retain());
      this.chunkSize -= toRead;
      
      out.add(chunk);
      if (this.chunkSize != 0L) {
        return;
      }
      this.currentState = State.READ_CHUNK_DELIMITER;
    case READ_CHUNK_DELIMITER: 
      int wIdx = buffer.writerIndex();
      int rIdx = buffer.readerIndex();
      while (wIdx > rIdx)
      {
        byte next = buffer.getByte(rIdx++);
        if (next == 10)
        {
          this.currentState = State.READ_CHUNK_SIZE;
          break;
        }
      }
      buffer.readerIndex(rIdx);
      return;
    case READ_CHUNK_FOOTER: 
      try
      {
        LastHttpContent trailer = readTrailingHeaders(buffer);
        if (trailer == null) {
          return;
        }
        out.add(trailer);
        resetNow();
        return;
      }
      catch (Exception e)
      {
        out.add(invalidChunk(e));
        return;
      }
    case BAD_MESSAGE: 
      buffer.skipBytes(buffer.readableBytes());
      break;
    case UPGRADED: 
      int readableBytes = buffer.readableBytes();
      if (readableBytes > 0) {
        out.add(buffer.readBytes(readableBytes));
      }
      break;
    }
  }
  
  protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    decode(ctx, in, out);
    if (this.message != null)
    {
      boolean chunked = HttpHeaderUtil.isTransferEncodingChunked(this.message);
      if ((this.currentState == State.READ_VARIABLE_LENGTH_CONTENT) && (!in.isReadable()) && (!chunked))
      {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
        reset(); return;
      }
      boolean prematureClosure;
      boolean prematureClosure;
      if ((isDecodingRequest()) || (chunked)) {
        prematureClosure = true;
      } else {
        prematureClosure = contentLength() > 0L;
      }
      resetNow();
      if (!prematureClosure) {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
      }
    }
  }
  
  protected boolean isContentAlwaysEmpty(HttpMessage msg)
  {
    if ((msg instanceof HttpResponse))
    {
      HttpResponse res = (HttpResponse)msg;
      int code = res.status().code();
      if ((code >= 100) && (code < 200)) {
        return (code != 101) || (res.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT));
      }
      switch (code)
      {
      case 204: 
      case 205: 
      case 304: 
        return true;
      }
    }
    return false;
  }
  
  public void reset()
  {
    this.resetRequested = true;
  }
  
  private void resetNow()
  {
    HttpMessage message = this.message;
    this.message = null;
    this.name = null;
    this.value = null;
    this.contentLength = Long.MIN_VALUE;
    this.lineParser.reset();
    this.headerParser.reset();
    this.trailer = null;
    if (!isDecodingRequest())
    {
      HttpResponse res = (HttpResponse)message;
      if ((res != null) && (res.status().code() == 101))
      {
        this.currentState = State.UPGRADED;
        return;
      }
    }
    this.currentState = State.SKIP_CONTROL_CHARS;
  }
  
  private HttpMessage invalidMessage(Exception cause)
  {
    this.currentState = State.BAD_MESSAGE;
    if (this.message != null)
    {
      this.message.setDecoderResult(DecoderResult.failure(cause));
    }
    else
    {
      this.message = createInvalidMessage();
      this.message.setDecoderResult(DecoderResult.failure(cause));
    }
    HttpMessage ret = this.message;
    this.message = null;
    return ret;
  }
  
  private HttpContent invalidChunk(Exception cause)
  {
    this.currentState = State.BAD_MESSAGE;
    HttpContent chunk = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
    chunk.setDecoderResult(DecoderResult.failure(cause));
    this.message = null;
    this.trailer = null;
    return chunk;
  }
  
  private static boolean skipControlCharacters(ByteBuf buffer)
  {
    boolean skiped = false;
    int wIdx = buffer.writerIndex();
    int rIdx = buffer.readerIndex();
    while (wIdx > rIdx)
    {
      int c = buffer.getUnsignedByte(rIdx++);
      if ((!Character.isISOControl(c)) && (!Character.isWhitespace(c)))
      {
        rIdx--;
        skiped = true;
        break;
      }
    }
    buffer.readerIndex(rIdx);
    return skiped;
  }
  
  private State readHeaders(ByteBuf buffer)
  {
    HttpMessage message = this.message;
    HttpHeaders headers = message.headers();
    
    AppendableCharSequence line = this.headerParser.parse(buffer);
    if (line == null) {
      return null;
    }
    if (line.length() > 0) {
      do
      {
        char firstChar = line.charAt(0);
        if ((this.name != null) && ((firstChar == ' ') || (firstChar == '\t')))
        {
          StringBuilder buf = new StringBuilder(this.value.length() + line.length() + 1);
          buf.append(this.value).append(' ').append(line.toString().trim());
          
          this.value = buf.toString();
        }
        else
        {
          if (this.name != null) {
            headers.add(this.name, this.value);
          }
          splitHeader(line);
        }
        line = this.headerParser.parse(buffer);
        if (line == null) {
          return null;
        }
      } while (line.length() > 0);
    }
    if (this.name != null) {
      headers.add(this.name, this.value);
    }
    this.name = null;
    this.value = null;
    State nextState;
    State nextState;
    if (isContentAlwaysEmpty(message))
    {
      HttpHeaderUtil.setTransferEncodingChunked(message, false);
      nextState = State.SKIP_CONTROL_CHARS;
    }
    else
    {
      State nextState;
      if (HttpHeaderUtil.isTransferEncodingChunked(message))
      {
        nextState = State.READ_CHUNK_SIZE;
      }
      else
      {
        State nextState;
        if (contentLength() >= 0L) {
          nextState = State.READ_FIXED_LENGTH_CONTENT;
        } else {
          nextState = State.READ_VARIABLE_LENGTH_CONTENT;
        }
      }
    }
    return nextState;
  }
  
  private long contentLength()
  {
    if (this.contentLength == Long.MIN_VALUE) {
      this.contentLength = HttpHeaderUtil.getContentLength(this.message, -1L);
    }
    return this.contentLength;
  }
  
  private LastHttpContent readTrailingHeaders(ByteBuf buffer)
  {
    AppendableCharSequence line = this.headerParser.parse(buffer);
    if (line == null) {
      return null;
    }
    CharSequence lastHeader = null;
    if (line.length() > 0)
    {
      LastHttpContent trailer = this.trailer;
      if (trailer == null) {
        trailer = this.trailer = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER, this.validateHeaders);
      }
      do
      {
        char firstChar = line.charAt(0);
        if ((lastHeader != null) && ((firstChar == ' ') || (firstChar == '\t')))
        {
          List<CharSequence> current = trailer.trailingHeaders().getAll(lastHeader);
          if (!current.isEmpty())
          {
            int lastPos = current.size() - 1;
            String lineTrimmed = line.toString().trim();
            CharSequence currentLastPos = (CharSequence)current.get(lastPos);
            StringBuilder b = new StringBuilder(currentLastPos.length() + lineTrimmed.length());
            b.append(currentLastPos).append(lineTrimmed);
            
            current.set(lastPos, b.toString());
          }
        }
        else
        {
          splitHeader(line);
          CharSequence headerName = this.name;
          if ((!HttpHeaderNames.CONTENT_LENGTH.equalsIgnoreCase(headerName)) && (!HttpHeaderNames.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) && (!HttpHeaderNames.TRAILER.equalsIgnoreCase(headerName))) {
            trailer.trailingHeaders().add(headerName, this.value);
          }
          lastHeader = this.name;
          
          this.name = null;
          this.value = null;
        }
        line = this.headerParser.parse(buffer);
        if (line == null) {
          return null;
        }
      } while (line.length() > 0);
      this.trailer = null;
      return trailer;
    }
    return LastHttpContent.EMPTY_LAST_CONTENT;
  }
  
  protected abstract boolean isDecodingRequest();
  
  protected abstract HttpMessage createMessage(String[] paramArrayOfString)
    throws Exception;
  
  protected abstract HttpMessage createInvalidMessage();
  
  private static int getChunkSize(String hex)
  {
    hex = hex.trim();
    for (int i = 0; i < hex.length(); i++)
    {
      char c = hex.charAt(i);
      if ((c == ';') || (Character.isWhitespace(c)) || (Character.isISOControl(c)))
      {
        hex = hex.substring(0, i);
        break;
      }
    }
    return Integer.parseInt(hex, 16);
  }
  
  private static String[] splitInitialLine(AppendableCharSequence sb)
  {
    int aStart = findNonWhitespace(sb, 0);
    int aEnd = findWhitespace(sb, aStart);
    
    int bStart = findNonWhitespace(sb, aEnd);
    int bEnd = findWhitespace(sb, bStart);
    
    int cStart = findNonWhitespace(sb, bEnd);
    int cEnd = findEndOfString(sb);
    
    return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), cStart < cEnd ? sb.substring(cStart, cEnd) : "" };
  }
  
  private void splitHeader(AppendableCharSequence sb)
  {
    int length = sb.length();
    
    int nameStart = findNonWhitespace(sb, 0);
    for (int nameEnd = nameStart; nameEnd < length; nameEnd++)
    {
      char ch = sb.charAt(nameEnd);
      if ((ch == ':') || (Character.isWhitespace(ch))) {
        break;
      }
    }
    for (int colonEnd = nameEnd; colonEnd < length; colonEnd++) {
      if (sb.charAt(colonEnd) == ':')
      {
        colonEnd++;
        break;
      }
    }
    this.name = sb.substring(nameStart, nameEnd);
    int valueStart = findNonWhitespace(sb, colonEnd);
    if (valueStart == length)
    {
      this.value = "";
    }
    else
    {
      int valueEnd = findEndOfString(sb);
      this.value = sb.substring(valueStart, valueEnd);
    }
  }
  
  private static int findNonWhitespace(CharSequence sb, int offset)
  {
    for (int result = offset; result < sb.length(); result++) {
      if (!Character.isWhitespace(sb.charAt(result))) {
        break;
      }
    }
    return result;
  }
  
  private static int findWhitespace(CharSequence sb, int offset)
  {
    for (int result = offset; result < sb.length(); result++) {
      if (Character.isWhitespace(sb.charAt(result))) {
        break;
      }
    }
    return result;
  }
  
  private static int findEndOfString(CharSequence sb)
  {
    for (int result = sb.length(); result > 0; result--) {
      if (!Character.isWhitespace(sb.charAt(result - 1))) {
        break;
      }
    }
    return result;
  }
  
  private static class HeaderParser
    implements ByteBufProcessor
  {
    private final AppendableCharSequence seq;
    private final int maxLength;
    private int size;
    
    HeaderParser(AppendableCharSequence seq, int maxLength)
    {
      this.seq = seq;
      this.maxLength = maxLength;
    }
    
    public AppendableCharSequence parse(ByteBuf buffer)
    {
      this.seq.reset();
      int i = buffer.forEachByte(this);
      if (i == -1) {
        return null;
      }
      buffer.readerIndex(i + 1);
      return this.seq;
    }
    
    public void reset()
    {
      this.size = 0;
    }
    
    public boolean process(byte value)
      throws Exception
    {
      char nextByte = (char)value;
      if (nextByte == '\r') {
        return true;
      }
      if (nextByte == '\n') {
        return false;
      }
      if (this.size >= this.maxLength) {
        throw newException(this.maxLength);
      }
      this.size += 1;
      this.seq.append(nextByte);
      return true;
    }
    
    protected TooLongFrameException newException(int maxLength)
    {
      return new TooLongFrameException("HTTP header is larger than " + maxLength + " bytes.");
    }
  }
  
  private static final class LineParser
    extends HttpObjectDecoder.HeaderParser
  {
    LineParser(AppendableCharSequence seq, int maxLength)
    {
      super(maxLength);
    }
    
    public AppendableCharSequence parse(ByteBuf buffer)
    {
      reset();
      return super.parse(buffer);
    }
    
    protected TooLongFrameException newException(int maxLength)
    {
      return new TooLongFrameException("An HTTP line is larger than " + maxLength + " bytes.");
    }
  }
}
