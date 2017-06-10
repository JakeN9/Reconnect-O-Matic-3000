package org.spacehq.packetlib.event.server;

public abstract interface ServerEvent
{
  public abstract void call(ServerListener paramServerListener);
}
