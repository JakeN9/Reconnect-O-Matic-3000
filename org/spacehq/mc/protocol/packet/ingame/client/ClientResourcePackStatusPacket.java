package org.spacehq.mc.protocol.packet.ingame.client;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.ResourcePackStatus;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientResourcePackStatusPacket
  implements Packet
{
  private String hash;
  private ResourcePackStatus status;
  
  private ClientResourcePackStatusPacket() {}
  
  public ClientResourcePackStatusPacket(String hash, ResourcePackStatus status)
  {
    this.hash = hash;
    this.status = status;
  }
  
  public String getHash()
  {
    return this.hash;
  }
  
  public ResourcePackStatus getStatus()
  {
    return this.status;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.hash = in.readString();
    this.status = ((ResourcePackStatus)MagicValues.key(ResourcePackStatus.class, Integer.valueOf(in.readVarInt())));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeString(this.hash);
    out.writeVarInt(((Integer)MagicValues.value(Integer.class, this.status)).intValue());
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
