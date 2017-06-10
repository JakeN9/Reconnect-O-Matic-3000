package io.netty.handler.codec.http;

public class DefaultHttpResponse
  extends DefaultHttpMessage
  implements HttpResponse
{
  private static final int HASH_CODE_PRIME = 31;
  private HttpResponseStatus status;
  
  public DefaultHttpResponse(HttpVersion version, HttpResponseStatus status)
  {
    this(version, status, true, false);
  }
  
  public DefaultHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders)
  {
    this(version, status, validateHeaders, false);
  }
  
  public DefaultHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders, boolean singleHeaderFields)
  {
    super(version, validateHeaders, singleHeaderFields);
    if (status == null) {
      throw new NullPointerException("status");
    }
    this.status = status;
  }
  
  public HttpResponseStatus status()
  {
    return this.status;
  }
  
  public HttpResponse setStatus(HttpResponseStatus status)
  {
    if (status == null) {
      throw new NullPointerException("status");
    }
    this.status = status;
    return this;
  }
  
  public HttpResponse setProtocolVersion(HttpVersion version)
  {
    super.setProtocolVersion(version);
    return this;
  }
  
  public int hashCode()
  {
    int result = 1;
    result = 31 * result + this.status.hashCode();
    result = 31 * result + super.hashCode();
    return result;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHttpResponse)) {
      return false;
    }
    DefaultHttpResponse other = (DefaultHttpResponse)o;
    
    return (status().equals(other.status())) && (super.equals(o));
  }
  
  public String toString()
  {
    return HttpMessageUtil.appendResponse(new StringBuilder(256), this).toString();
  }
}
