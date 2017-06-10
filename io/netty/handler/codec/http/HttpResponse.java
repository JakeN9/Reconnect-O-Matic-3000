package io.netty.handler.codec.http;

public abstract interface HttpResponse
  extends HttpMessage
{
  public abstract HttpResponseStatus status();
  
  public abstract HttpResponse setStatus(HttpResponseStatus paramHttpResponseStatus);
  
  public abstract HttpResponse setProtocolVersion(HttpVersion paramHttpVersion);
}
