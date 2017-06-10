package org.spacehq.mc.protocol.packet.ingame.server.entity;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.Effect;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerEntityRemoveEffectPacket
  implements Packet
{
  private int entityId;
  private Effect effect;
  
  private ServerEntityRemoveEffectPacket() {}
  
  public ServerEntityRemoveEffectPacket(int entityId, Effect effect)
  {
    this.entityId = entityId;
    this.effect = effect;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public Effect getEffect()
  {
    return this.effect;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.effect = ((Effect)MagicValues.key(Effect.class, Byte.valueOf(in.readByte())));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.effect)).intValue());
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
