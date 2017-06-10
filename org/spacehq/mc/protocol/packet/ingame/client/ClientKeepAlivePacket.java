package org.spacehq.mc.protocol.packet.ingame.client;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientKeepAlivePacket
  implements Packet
{
  private int id;
  
  private ClientKeepAlivePacket() {}
  
  public ClientKeepAlivePacket(int id)
  {
    this.id = id;
  }
  
  public int getPingId()
  {
    return this.id;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.id = in.readVarInt();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.id);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
