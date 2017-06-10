package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.AsciiString;
import io.netty.util.CharsetUtil;

public class HttpRequestEncoder
  extends HttpObjectEncoder<HttpRequest>
{
  private static final char SLASH = '/';
  private static final char QUESTION_MARK = '?';
  private static final byte[] CRLF = { 13, 10 };
  
  public boolean acceptOutboundMessage(Object msg)
    throws Exception
  {
    return (super.acceptOutboundMessage(msg)) && (!(msg instanceof HttpResponse));
  }
  
  protected void encodeInitialLine(ByteBuf buf, HttpRequest request)
    throws Exception
  {
    AsciiString method = request.method().name();
    buf.writeBytes(method.array(), method.arrayOffset(), method.length());
    buf.writeByte(32);
    
    String uri = request.uri();
    if (uri.length() == 0)
    {
      uri = uri + '/';
    }
    else
    {
      int start = uri.indexOf("://");
      if ((start != -1) && (uri.charAt(0) != '/'))
      {
        int startIndex = start + 3;
        
        int index = uri.indexOf('?', startIndex);
        if (index == -1)
        {
          if (uri.lastIndexOf('/') <= startIndex) {
            uri = uri + '/';
          }
        }
        else if (uri.lastIndexOf('/', index) <= startIndex)
        {
          int len = uri.length();
          StringBuilder sb = new StringBuilder(len + 1);
          sb.append(uri, 0, index).append('/').append(uri, index, len);
          
          uri = sb.toString();
        }
      }
    }
    buf.writeBytes(uri.getBytes(CharsetUtil.UTF_8));
    buf.writeByte(32);
    
    AsciiString version = request.protocolVersion().text();
    buf.writeBytes(version.array(), version.arrayOffset(), version.length());
    buf.writeBytes(CRLF);
  }
}
