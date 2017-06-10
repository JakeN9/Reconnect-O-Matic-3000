package org.spacehq.mc.auth.exception.request;

public class InvalidCredentialsException
  extends RequestException
{
  private static final long serialVersionUID = 1L;
  
  public InvalidCredentialsException() {}
  
  public InvalidCredentialsException(String message)
  {
    super(message);
  }
  
  public InvalidCredentialsException(String message, Throwable cause)
  {
    super(message, cause);
  }
  
  public InvalidCredentialsException(Throwable cause)
  {
    super(cause);
  }
}
