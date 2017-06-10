package org.spacehq.mc.protocol.packet.ingame.server.scoreboard;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ScoreboardPosition;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerDisplayScoreboardPacket
  implements Packet
{
  private ScoreboardPosition position;
  private String name;
  
  private ServerDisplayScoreboardPacket() {}
  
  public ServerDisplayScoreboardPacket(ScoreboardPosition position, String name)
  {
    this.position = position;
    this.name = name;
  }
  
  public ScoreboardPosition getPosition()
  {
    return this.position;
  }
  
  public String getScoreboardName()
  {
    return this.name;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.position = ((ScoreboardPosition)MagicValues.key(ScoreboardPosition.class, Byte.valueOf(in.readByte())));
    this.name = in.readString();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.position)).intValue());
    out.writeString(this.name);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
