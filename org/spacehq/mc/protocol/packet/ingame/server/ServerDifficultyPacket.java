package org.spacehq.mc.protocol.packet.ingame.server;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.setting.Difficulty;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerDifficultyPacket
  implements Packet
{
  private Difficulty difficulty;
  
  private ServerDifficultyPacket() {}
  
  public ServerDifficultyPacket(Difficulty difficulty)
  {
    this.difficulty = difficulty;
  }
  
  public Difficulty getDifficulty()
  {
    return this.difficulty;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.difficulty = ((Difficulty)MagicValues.key(Difficulty.class, Integer.valueOf(in.readUnsignedByte())));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.difficulty)).intValue());
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
