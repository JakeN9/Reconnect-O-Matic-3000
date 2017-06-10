package io.netty.handler.codec.http2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Http2Exception
  extends Exception
{
  private static final long serialVersionUID = -6943456574080986447L;
  private final Http2Error error;
  
  public Http2Exception(Http2Error error)
  {
    this.error = error;
  }
  
  public Http2Exception(Http2Error error, String message)
  {
    super(message);
    this.error = error;
  }
  
  public Http2Exception(Http2Error error, String message, Throwable cause)
  {
    super(message, cause);
    this.error = error;
  }
  
  public Http2Error error()
  {
    return this.error;
  }
  
  public static Http2Exception connectionError(Http2Error error, String fmt, Object... args)
  {
    return new Http2Exception(error, String.format(fmt, args));
  }
  
  public static Http2Exception connectionError(Http2Error error, Throwable cause, String fmt, Object... args)
  {
    return new Http2Exception(error, String.format(fmt, args), cause);
  }
  
  public static Http2Exception streamError(int id, Http2Error error, String fmt, Object... args)
  {
    return 0 == id ? connectionError(error, fmt, args) : new StreamException(id, error, String.format(fmt, args));
  }
  
  public static Http2Exception streamError(int id, Http2Error error, Throwable cause, String fmt, Object... args)
  {
    return 0 == id ? connectionError(error, cause, fmt, args) : new StreamException(id, error, String.format(fmt, args), cause);
  }
  
  public static boolean isStreamError(Http2Exception e)
  {
    return e instanceof StreamException;
  }
  
  public static int streamId(Http2Exception e)
  {
    return isStreamError(e) ? ((StreamException)e).streamId() : 0;
  }
  
  public static final class StreamException
    extends Http2Exception
  {
    private static final long serialVersionUID = 462766352505067095L;
    private final int streamId;
    
    StreamException(int streamId, Http2Error error, String message)
    {
      super(message);
      this.streamId = streamId;
    }
    
    StreamException(int streamId, Http2Error error, String message, Throwable cause)
    {
      super(message, cause);
      this.streamId = streamId;
    }
    
    public int streamId()
    {
      return this.streamId;
    }
  }
  
  public static final class CompositeStreamException
    extends Http2Exception
    implements Iterable<Http2Exception.StreamException>
  {
    private static final long serialVersionUID = -434398146294199889L;
    private final List<Http2Exception.StreamException> exceptions;
    
    public CompositeStreamException(Http2Error error, int initialCapacity)
    {
      super();
      this.exceptions = new ArrayList(initialCapacity);
    }
    
    public void add(Http2Exception.StreamException e)
    {
      this.exceptions.add(e);
    }
    
    public Iterator<Http2Exception.StreamException> iterator()
    {
      return this.exceptions.iterator();
    }
  }
}
