package org.spacehq.packetlib.event.session;

public abstract interface SessionListener
{
  public abstract void packetReceived(PacketReceivedEvent paramPacketReceivedEvent);
  
  public abstract void packetSent(PacketSentEvent paramPacketSentEvent);
  
  public abstract void connected(ConnectedEvent paramConnectedEvent);
  
  public abstract void disconnecting(DisconnectingEvent paramDisconnectingEvent);
  
  public abstract void disconnected(DisconnectedEvent paramDisconnectedEvent);
}
