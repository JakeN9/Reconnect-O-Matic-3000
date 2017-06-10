package org.spacehq.mc.protocol.data.status.handler;

import org.spacehq.mc.protocol.data.status.ServerStatusInfo;
import org.spacehq.packetlib.Session;

public abstract interface ServerInfoHandler
{
  public abstract void handle(Session paramSession, ServerStatusInfo paramServerStatusInfo);
}
