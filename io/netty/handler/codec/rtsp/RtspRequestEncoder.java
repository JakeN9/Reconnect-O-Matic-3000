package io.netty.handler.codec.rtsp;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class RtspRequestEncoder
  extends RtspObjectEncoder<HttpRequest>
{
  private static final byte[] CRLF = { 13, 10 };
  
  public boolean acceptOutboundMessage(Object msg)
    throws Exception
  {
    return msg instanceof FullHttpRequest;
  }
  
  protected void encodeInitialLine(ByteBuf buf, HttpRequest request)
    throws Exception
  {
    AsciiString method = request.method().name();
    buf.writeBytes(method.array(), method.arrayOffset(), method.length());
    buf.writeByte(32);
    
    buf.writeBytes(request.uri().getBytes(CharsetUtil.UTF_8));
    buf.writeByte(32);
    
    AsciiString version = request.protocolVersion().text();
    buf.writeBytes(version.array(), version.arrayOffset(), version.length());
    buf.writeBytes(CRLF);
  }
}
