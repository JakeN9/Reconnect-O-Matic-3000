package org.spacehq.mc.protocol.packet.ingame.server.scoreboard;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ScoreboardAction;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerUpdateScorePacket
  implements Packet
{
  private String entry;
  private ScoreboardAction action;
  private String objective;
  private int value;
  
  private ServerUpdateScorePacket() {}
  
  public ServerUpdateScorePacket(String entry, String objective)
  {
    this.entry = entry;
    this.objective = objective;
    this.action = ScoreboardAction.REMOVE;
  }
  
  public ServerUpdateScorePacket(String entry, String objective, int value)
  {
    this.entry = entry;
    this.objective = objective;
    this.value = value;
    this.action = ScoreboardAction.ADD_OR_UPDATE;
  }
  
  public String getEntry()
  {
    return this.entry;
  }
  
  public ScoreboardAction getAction()
  {
    return this.action;
  }
  
  public String getObjective()
  {
    return this.objective;
  }
  
  public int getValue()
  {
    return this.value;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entry = in.readString();
    this.action = ((ScoreboardAction)MagicValues.key(ScoreboardAction.class, Byte.valueOf(in.readByte())));
    this.objective = in.readString();
    if (this.action == ScoreboardAction.ADD_OR_UPDATE) {
      this.value = in.readVarInt();
    }
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeString(this.entry);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.action)).intValue());
    out.writeString(this.objective);
    if (this.action == ScoreboardAction.ADD_OR_UPDATE) {
      out.writeVarInt(this.value);
    }
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
