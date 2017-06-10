package org.spacehq.packetlib.event.server;

import org.spacehq.packetlib.Server;
import org.spacehq.packetlib.Session;

public class SessionRemovedEvent
  implements ServerEvent
{
  private Server server;
  private Session session;
  
  public SessionRemovedEvent(Server server, Session session)
  {
    this.server = server;
    this.session = session;
  }
  
  public Server getServer()
  {
    return this.server;
  }
  
  public Session getSession()
  {
    return this.session;
  }
  
  public void call(ServerListener listener)
  {
    listener.sessionRemoved(this);
  }
}
