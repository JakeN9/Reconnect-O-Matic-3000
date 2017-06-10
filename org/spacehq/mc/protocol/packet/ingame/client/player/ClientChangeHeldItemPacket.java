package org.spacehq.mc.protocol.packet.ingame.client.player;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientChangeHeldItemPacket
  implements Packet
{
  private int slot;
  
  private ClientChangeHeldItemPacket() {}
  
  public ClientChangeHeldItemPacket(int slot)
  {
    this.slot = slot;
  }
  
  public int getSlot()
  {
    return this.slot;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.slot = in.readShort();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeShort(this.slot);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
