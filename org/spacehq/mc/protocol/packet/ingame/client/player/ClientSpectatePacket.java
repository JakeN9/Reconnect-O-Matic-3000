package org.spacehq.mc.protocol.packet.ingame.client.player;

import java.io.IOException;
import java.util.UUID;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientSpectatePacket
  implements Packet
{
  private UUID target;
  
  private ClientSpectatePacket() {}
  
  public ClientSpectatePacket(UUID target)
  {
    this.target = target;
  }
  
  public UUID getTarget()
  {
    return this.target;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.target = in.readUUID();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeUUID(this.target);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
