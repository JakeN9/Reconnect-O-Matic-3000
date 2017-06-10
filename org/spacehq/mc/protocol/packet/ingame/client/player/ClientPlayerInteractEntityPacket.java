package org.spacehq.mc.protocol.packet.ingame.client.player;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.InteractAction;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientPlayerInteractEntityPacket
  implements Packet
{
  private int entityId;
  private InteractAction action;
  private float targetX;
  private float targetY;
  private float targetZ;
  
  private ClientPlayerInteractEntityPacket() {}
  
  public ClientPlayerInteractEntityPacket(int entityId, InteractAction action)
  {
    this(entityId, action, 0.0F, 0.0F, 0.0F);
  }
  
  public ClientPlayerInteractEntityPacket(int entityId, InteractAction action, float targetX, float targetY, float targetZ)
  {
    this.entityId = entityId;
    this.action = action;
    this.targetX = targetX;
    this.targetY = targetY;
    this.targetZ = targetZ;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public InteractAction getAction()
  {
    return this.action;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.action = ((InteractAction)MagicValues.key(InteractAction.class, Integer.valueOf(in.readVarInt())));
    if (this.action == InteractAction.INTERACT_AT)
    {
      this.targetX = in.readFloat();
      this.targetY = in.readFloat();
      this.targetZ = in.readFloat();
    }
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeVarInt(((Integer)MagicValues.value(Integer.class, this.action)).intValue());
    if (this.action == InteractAction.INTERACT_AT)
    {
      out.writeFloat(this.targetX);
      out.writeFloat(this.targetY);
      out.writeFloat(this.targetZ);
    }
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
