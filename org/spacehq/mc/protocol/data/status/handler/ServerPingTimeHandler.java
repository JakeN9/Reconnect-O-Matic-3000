package org.spacehq.mc.protocol.data.status.handler;

import org.spacehq.packetlib.Session;

public abstract interface ServerPingTimeHandler
{
  public abstract void handle(Session paramSession, long paramLong);
}
