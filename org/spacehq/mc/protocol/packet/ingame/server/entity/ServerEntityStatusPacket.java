package org.spacehq.mc.protocol.packet.ingame.server.entity;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.EntityStatus;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerEntityStatusPacket
  implements Packet
{
  protected int entityId;
  protected EntityStatus status;
  
  private ServerEntityStatusPacket() {}
  
  public ServerEntityStatusPacket(int entityId, EntityStatus status)
  {
    this.entityId = entityId;
    this.status = status;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public EntityStatus getStatus()
  {
    return this.status;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readInt();
    this.status = ((EntityStatus)MagicValues.key(EntityStatus.class, Byte.valueOf(in.readByte())));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeInt(this.entityId);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.status)).intValue());
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
