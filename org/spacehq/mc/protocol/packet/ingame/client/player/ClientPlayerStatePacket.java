package org.spacehq.mc.protocol.packet.ingame.client.player;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.PlayerState;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientPlayerStatePacket
  implements Packet
{
  private int entityId;
  private PlayerState state;
  private int jumpBoost;
  
  private ClientPlayerStatePacket() {}
  
  public ClientPlayerStatePacket(int entityId, PlayerState state)
  {
    this(entityId, state, 0);
  }
  
  public ClientPlayerStatePacket(int entityId, PlayerState state, int jumpBoost)
  {
    this.entityId = entityId;
    this.state = state;
    this.jumpBoost = jumpBoost;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public PlayerState getState()
  {
    return this.state;
  }
  
  public int getJumpBoost()
  {
    return this.jumpBoost;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.state = ((PlayerState)MagicValues.key(PlayerState.class, Integer.valueOf(in.readUnsignedByte())));
    this.jumpBoost = in.readVarInt();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.state)).intValue());
    out.writeVarInt(this.jumpBoost);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
