package org.spacehq.packetlib.event.session;

public abstract interface SessionEvent
{
  public abstract void call(SessionListener paramSessionListener);
}
