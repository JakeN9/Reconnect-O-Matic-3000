package org.spacehq.mc.protocol.packet.ingame.server.world;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.BlockBreakStage;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerBlockBreakAnimPacket
  implements Packet
{
  private int breakerEntityId;
  private Position position;
  private BlockBreakStage stage;
  
  private ServerBlockBreakAnimPacket() {}
  
  public ServerBlockBreakAnimPacket(int breakerEntityId, Position position, BlockBreakStage stage)
  {
    this.breakerEntityId = breakerEntityId;
    this.position = position;
    this.stage = stage;
  }
  
  public int getBreakerEntityId()
  {
    return this.breakerEntityId;
  }
  
  public Position getPosition()
  {
    return this.position;
  }
  
  public BlockBreakStage getStage()
  {
    return this.stage;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.breakerEntityId = in.readVarInt();
    this.position = NetUtil.readPosition(in);
    this.stage = ((BlockBreakStage)MagicValues.key(BlockBreakStage.class, Integer.valueOf(in.readUnsignedByte())));
    if (this.stage == null) {
      this.stage = BlockBreakStage.RESET;
    }
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.breakerEntityId);
    NetUtil.writePosition(out, this.position);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.stage)).intValue());
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
