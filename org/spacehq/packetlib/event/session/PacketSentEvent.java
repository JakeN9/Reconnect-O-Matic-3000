package org.spacehq.packetlib.event.session;

import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.packet.Packet;

public class PacketSentEvent
  implements SessionEvent
{
  private Session session;
  private Packet packet;
  
  public PacketSentEvent(Session session, Packet packet)
  {
    this.session = session;
    this.packet = packet;
  }
  
  public Session getSession()
  {
    return this.session;
  }
  
  public <T extends Packet> T getPacket()
  {
    try
    {
      return this.packet;
    }
    catch (ClassCastException e)
    {
      throw new IllegalStateException("Tried to get packet as the wrong type. Actual type: " + this.packet.getClass().getName());
    }
  }
  
  public void call(SessionListener listener)
  {
    listener.packetSent(this);
  }
}
