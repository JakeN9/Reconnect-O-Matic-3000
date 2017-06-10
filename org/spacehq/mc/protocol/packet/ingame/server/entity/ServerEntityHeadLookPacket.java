package org.spacehq.mc.protocol.packet.ingame.server.entity;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerEntityHeadLookPacket
  implements Packet
{
  private int entityId;
  private float headYaw;
  
  private ServerEntityHeadLookPacket() {}
  
  public ServerEntityHeadLookPacket(int entityId, float headYaw)
  {
    this.entityId = entityId;
    this.headYaw = headYaw;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public float getHeadYaw()
  {
    return this.headYaw;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.headYaw = (in.readByte() * 360 / 256.0F);
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeByte((byte)(int)(this.headYaw * 256.0F / 360.0F));
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
