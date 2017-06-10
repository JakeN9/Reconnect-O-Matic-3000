package org.spacehq.packetlib;

public abstract interface SessionFactory
{
  public abstract Session createClientSession(Client paramClient);
  
  public abstract ConnectionListener createServerListener(Server paramServer);
}
