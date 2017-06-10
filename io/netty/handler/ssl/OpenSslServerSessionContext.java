package io.netty.handler.ssl;

import org.apache.tomcat.jni.SSLContext;

public final class OpenSslServerSessionContext
  extends OpenSslSessionContext
{
  OpenSslServerSessionContext(long context)
  {
    super(context);
  }
  
  public void setSessionTimeout(int seconds)
  {
    if (seconds < 0) {
      throw new IllegalArgumentException();
    }
    SSLContext.setSessionCacheTimeout(this.context, seconds);
  }
  
  public int getSessionTimeout()
  {
    return (int)SSLContext.getSessionCacheTimeout(this.context);
  }
  
  public void setSessionCacheSize(int size)
  {
    if (size < 0) {
      throw new IllegalArgumentException();
    }
    SSLContext.setSessionCacheSize(this.context, size);
  }
  
  public int getSessionCacheSize()
  {
    return (int)SSLContext.getSessionCacheSize(this.context);
  }
  
  public void setSessionCacheEnabled(boolean enabled)
  {
    long mode = enabled ? 2L : 0L;
    SSLContext.setSessionCacheMode(this.context, mode);
  }
  
  public boolean isSessionCacheEnabled()
  {
    return SSLContext.getSessionCacheMode(this.context) == 2L;
  }
  
  public boolean setSessionIdContext(byte[] sidCtx)
  {
    return SSLContext.setSessionIdContext(this.context, sidCtx);
  }
}
