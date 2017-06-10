package io.netty.handler.codec.http;

public abstract interface HttpMessage
  extends HttpObject
{
  public abstract HttpVersion protocolVersion();
  
  public abstract HttpMessage setProtocolVersion(HttpVersion paramHttpVersion);
  
  public abstract HttpHeaders headers();
}
