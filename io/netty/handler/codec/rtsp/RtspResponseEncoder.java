package io.netty.handler.codec.rtsp;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class RtspResponseEncoder
  extends RtspObjectEncoder<HttpResponse>
{
  private static final byte[] CRLF = { 13, 10 };
  
  public boolean acceptOutboundMessage(Object msg)
    throws Exception
  {
    return msg instanceof FullHttpResponse;
  }
  
  protected void encodeInitialLine(ByteBuf buf, HttpResponse response)
    throws Exception
  {
    AsciiString version = response.protocolVersion().text();
    buf.writeBytes(version.array(), version.arrayOffset(), version.length());
    buf.writeByte(32);
    
    AsciiString code = response.status().codeAsText();
    buf.writeBytes(code.array(), code.arrayOffset(), code.length());
    buf.writeByte(32);
    
    AsciiString reasonPhrase = response.status().reasonPhrase();
    buf.writeBytes(reasonPhrase.array(), reasonPhrase.arrayOffset(), reasonPhrase.length());
    buf.writeBytes(CRLF);
  }
}
