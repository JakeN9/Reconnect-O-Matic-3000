package org.spacehq.packetlib;

public abstract interface ConnectionListener
{
  public abstract String getHost();
  
  public abstract int getPort();
  
  public abstract boolean isListening();
  
  public abstract void bind();
  
  public abstract void bind(boolean paramBoolean);
  
  public abstract void bind(boolean paramBoolean, Runnable paramRunnable);
  
  public abstract void close();
  
  public abstract void close(boolean paramBoolean);
  
  public abstract void close(boolean paramBoolean, Runnable paramRunnable);
}
