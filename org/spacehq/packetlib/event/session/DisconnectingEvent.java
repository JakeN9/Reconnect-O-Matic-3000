package org.spacehq.packetlib.event.session;

import org.spacehq.packetlib.Session;

public class DisconnectingEvent
  implements SessionEvent
{
  private Session session;
  private String reason;
  private Throwable cause;
  
  public DisconnectingEvent(Session session, String reason)
  {
    this(session, reason, null);
  }
  
  public DisconnectingEvent(Session session, String reason, Throwable cause)
  {
    this.session = session;
    this.reason = reason;
    this.cause = cause;
  }
  
  public Session getSession()
  {
    return this.session;
  }
  
  public String getReason()
  {
    return this.reason;
  }
  
  public Throwable getCause()
  {
    return this.cause;
  }
  
  public void call(SessionListener listener)
  {
    listener.disconnecting(this);
  }
}
