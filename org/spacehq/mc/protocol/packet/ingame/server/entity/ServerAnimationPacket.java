package org.spacehq.mc.protocol.packet.ingame.server.entity;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.Animation;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerAnimationPacket
  implements Packet
{
  private int entityId;
  private Animation animation;
  
  private ServerAnimationPacket() {}
  
  public ServerAnimationPacket(int entityId, Animation animation)
  {
    this.entityId = entityId;
    this.animation = animation;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public Animation getAnimation()
  {
    return this.animation;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.animation = ((Animation)MagicValues.key(Animation.class, Byte.valueOf(in.readByte())));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.animation)).intValue());
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
