package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.AsciiString;

public class HttpResponseEncoder
  extends HttpObjectEncoder<HttpResponse>
{
  private static final byte[] CRLF = { 13, 10 };
  
  public boolean acceptOutboundMessage(Object msg)
    throws Exception
  {
    return (super.acceptOutboundMessage(msg)) && (!(msg instanceof HttpRequest));
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
