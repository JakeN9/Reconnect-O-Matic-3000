package org.spacehq.mc.protocol.packet.ingame.server.entity;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.Effect;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerEntityEffectPacket
  implements Packet
{
  private int entityId;
  private Effect effect;
  private int amplifier;
  private int duration;
  private boolean hideParticles;
  
  private ServerEntityEffectPacket() {}
  
  public ServerEntityEffectPacket(int entityId, Effect effect, int amplifier, int duration, boolean hideParticles)
  {
    this.entityId = entityId;
    this.effect = effect;
    this.amplifier = amplifier;
    this.duration = duration;
    this.hideParticles = hideParticles;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public Effect getEffect()
  {
    return this.effect;
  }
  
  public int getAmplifier()
  {
    return this.amplifier;
  }
  
  public int getDuration()
  {
    return this.duration;
  }
  
  public boolean getHideParticles()
  {
    return this.hideParticles;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.effect = ((Effect)MagicValues.key(Effect.class, Byte.valueOf(in.readByte())));
    this.amplifier = in.readByte();
    this.duration = in.readVarInt();
    this.hideParticles = in.readBoolean();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.effect)).intValue());
    out.writeByte(this.amplifier);
    out.writeVarInt(this.duration);
    out.writeBoolean(this.hideParticles);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
