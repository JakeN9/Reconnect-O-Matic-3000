package org.spacehq.mc.protocol;

import org.spacehq.packetlib.Session;

public abstract interface ServerLoginHandler
{
  public abstract void loggedIn(Session paramSession);
}
