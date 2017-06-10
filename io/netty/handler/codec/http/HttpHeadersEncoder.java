package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.TextHeaders.EntryVisitor;
import java.util.Map.Entry;

final class HttpHeadersEncoder
  implements TextHeaders.EntryVisitor
{
  private final ByteBuf buf;
  
  HttpHeadersEncoder(ByteBuf buf)
  {
    this.buf = buf;
  }
  
  public boolean visit(Map.Entry<CharSequence, CharSequence> entry)
    throws Exception
  {
    CharSequence name = (CharSequence)entry.getKey();
    CharSequence value = (CharSequence)entry.getValue();
    ByteBuf buf = this.buf;
    int nameLen = name.length();
    int valueLen = value.length();
    int entryLen = nameLen + valueLen + 4;
    int offset = buf.writerIndex();
    buf.ensureWritable(entryLen);
    writeAscii(buf, offset, name, nameLen);
    offset += nameLen;
    buf.setByte(offset++, 58);
    buf.setByte(offset++, 32);
    writeAscii(buf, offset, value, valueLen);
    offset += valueLen;
    buf.setByte(offset++, 13);
    buf.setByte(offset++, 10);
    buf.writerIndex(offset);
    return true;
  }
  
  private static void writeAscii(ByteBuf buf, int offset, CharSequence value, int valueLen)
  {
    if ((value instanceof AsciiString)) {
      writeAsciiString(buf, offset, (AsciiString)value, valueLen);
    } else {
      writeCharSequence(buf, offset, value, valueLen);
    }
  }
  
  private static void writeAsciiString(ByteBuf buf, int offset, AsciiString value, int valueLen)
  {
    value.copy(0, buf, offset, valueLen);
  }
  
  private static void writeCharSequence(ByteBuf buf, int offset, CharSequence value, int valueLen)
  {
    for (int i = 0; i < valueLen; i++) {
      buf.setByte(offset++, c2b(value.charAt(i)));
    }
  }
  
  private static int c2b(char ch)
  {
    return ch < 'Ā' ? (byte)ch : 63;
  }
}
