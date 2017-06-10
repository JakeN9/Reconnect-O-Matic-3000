package org.spacehq.packetlib.event.server;

import org.spacehq.packetlib.Server;

public class ServerClosingEvent
  implements ServerEvent
{
  private Server server;
  
  public ServerClosingEvent(Server server)
  {
    this.server = server;
  }
  
  public Server getServer()
  {
    return this.server;
  }
  
  public void call(ServerListener listener)
  {
    listener.serverClosing(this);
  }
}
