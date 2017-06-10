package org.spacehq.packetlib.event.session;

import org.spacehq.packetlib.Session;

public class ConnectedEvent
  implements SessionEvent
{
  private Session session;
  
  public ConnectedEvent(Session session)
  {
    this.session = session;
  }
  
  public Session getSession()
  {
    return this.session;
  }
  
  public void call(SessionListener listener)
  {
    listener.connected(this);
  }
}
