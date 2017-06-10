package io.netty.handler.codec.http;

public abstract class DefaultHttpMessage
  extends DefaultHttpObject
  implements HttpMessage
{
  private static final int HASH_CODE_PRIME = 31;
  private HttpVersion version;
  private final HttpHeaders headers;
  
  protected DefaultHttpMessage(HttpVersion version)
  {
    this(version, true, false);
  }
  
  protected DefaultHttpMessage(HttpVersion version, boolean validateHeaders, boolean singleHeaderFields)
  {
    if (version == null) {
      throw new NullPointerException("version");
    }
    this.version = version;
    this.headers = new DefaultHttpHeaders(validateHeaders, singleHeaderFields);
  }
  
  public HttpHeaders headers()
  {
    return this.headers;
  }
  
  public HttpVersion protocolVersion()
  {
    return this.version;
  }
  
  public int hashCode()
  {
    int result = 1;
    result = 31 * result + this.headers.hashCode();
    result = 31 * result + this.version.hashCode();
    result = 31 * result + super.hashCode();
    return result;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHttpMessage)) {
      return false;
    }
    DefaultHttpMessage other = (DefaultHttpMessage)o;
    
    return (headers().equals(other.headers())) && (protocolVersion().equals(other.protocolVersion())) && (super.equals(o));
  }
  
  public HttpMessage setProtocolVersion(HttpVersion version)
  {
    if (version == null) {
      throw new NullPointerException("version");
    }
    this.version = version;
    return this;
  }
}
