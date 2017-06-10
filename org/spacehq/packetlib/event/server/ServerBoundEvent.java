package org.spacehq.packetlib.event.server;

import org.spacehq.packetlib.Server;

public class ServerBoundEvent
  implements ServerEvent
{
  private Server server;
  
  public ServerBoundEvent(Server server)
  {
    this.server = server;
  }
  
  public Server getServer()
  {
    return this.server;
  }
  
  public void call(ServerListener listener)
  {
    listener.serverBound(this);
  }
}
