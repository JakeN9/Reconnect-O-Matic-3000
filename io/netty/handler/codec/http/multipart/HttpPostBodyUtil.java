package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.HttpHeaderValues;

final class HttpPostBodyUtil
{
  public static final int chunkSize = 8096;
  public static final String DEFAULT_BINARY_CONTENT_TYPE = HttpHeaderValues.APPLICATION_OCTET_STREAM.toString();
  public static final String DEFAULT_TEXT_CONTENT_TYPE = HttpHeaderValues.TEXT_PLAIN.toString();
  
  public static enum TransferEncodingMechanism
  {
    BIT7("7bit"),  BIT8("8bit"),  BINARY("binary");
    
    private final String value;
    
    private TransferEncodingMechanism(String value)
    {
      this.value = value;
    }
    
    private TransferEncodingMechanism()
    {
      this.value = name();
    }
    
    public String value()
    {
      return this.value;
    }
    
    public String toString()
    {
      return this.value;
    }
  }
  
  static class SeekAheadNoBackArrayException
    extends Exception
  {
    private static final long serialVersionUID = -630418804938699495L;
  }
  
  static class SeekAheadOptimize
  {
    byte[] bytes;
    int readerIndex;
    int pos;
    int origPos;
    int limit;
    ByteBuf buffer;
    
    SeekAheadOptimize(ByteBuf buffer)
      throws HttpPostBodyUtil.SeekAheadNoBackArrayException
    {
      if (!buffer.hasArray()) {
        throw new HttpPostBodyUtil.SeekAheadNoBackArrayException();
      }
      this.buffer = buffer;
      this.bytes = buffer.array();
      this.readerIndex = buffer.readerIndex();
      this.origPos = (this.pos = buffer.arrayOffset() + this.readerIndex);
      this.limit = (buffer.arrayOffset() + buffer.writerIndex());
    }
    
    void setReadPosition(int minus)
    {
      this.pos -= minus;
      this.readerIndex = getReadPosition(this.pos);
      this.buffer.readerIndex(this.readerIndex);
    }
    
    int getReadPosition(int index)
    {
      return index - this.origPos + this.readerIndex;
    }
    
    void clear()
    {
      this.buffer = null;
      this.bytes = null;
      this.limit = 0;
      this.pos = 0;
      this.readerIndex = 0;
    }
  }
  
  static int findNonWhitespace(String sb, int offset)
  {
    for (int result = offset; result < sb.length(); result++) {
      if (!Character.isWhitespace(sb.charAt(result))) {
        break;
      }
    }
    return result;
  }
  
  static int findWhitespace(String sb, int offset)
  {
    for (int result = offset; result < sb.length(); result++) {
      if (Character.isWhitespace(sb.charAt(result))) {
        break;
      }
    }
    return result;
  }
  
  static int findEndOfString(String sb)
  {
    for (int result = sb.length(); result > 0; result--) {
      if (!Character.isWhitespace(sb.charAt(result - 1))) {
        break;
      }
    }
    return result;
  }
}
