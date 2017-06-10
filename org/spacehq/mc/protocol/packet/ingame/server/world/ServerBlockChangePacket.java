package org.spacehq.mc.protocol.packet.ingame.server.world;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.values.world.block.BlockChangeRecord;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerBlockChangePacket
  implements Packet
{
  private BlockChangeRecord record;
  
  private ServerBlockChangePacket() {}
  
  public ServerBlockChangePacket(BlockChangeRecord record)
  {
    this.record = record;
  }
  
  public BlockChangeRecord getRecord()
  {
    return this.record;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    Position position = NetUtil.readPosition(in);
    int block = in.readVarInt();
    
    this.record = new BlockChangeRecord(position, block >> 4, block & 0xF);
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    NetUtil.writePosition(out, this.record.getPosition());
    out.writeVarInt(this.record.getId() << 4 | this.record.getData() & 0xF);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
