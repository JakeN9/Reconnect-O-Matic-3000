package org.spacehq.mc.protocol.packet.ingame.server.scoreboard;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ObjectiveAction;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ScoreType;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerScoreboardObjectivePacket
  implements Packet
{
  private String name;
  private ObjectiveAction action;
  private String displayName;
  private ScoreType type;
  
  private ServerScoreboardObjectivePacket() {}
  
  public ServerScoreboardObjectivePacket(String name)
  {
    this.name = name;
    this.action = ObjectiveAction.REMOVE;
  }
  
  public ServerScoreboardObjectivePacket(String name, ObjectiveAction action, String displayName, ScoreType type)
  {
    if ((action != ObjectiveAction.ADD) && (action != ObjectiveAction.UPDATE)) {
      throw new IllegalArgumentException("(name, action, displayName) constructor only valid for adding and updating objectives.");
    }
    this.name = name;
    this.action = action;
    this.displayName = displayName;
    this.type = type;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public ObjectiveAction getAction()
  {
    return this.action;
  }
  
  public String getDisplayName()
  {
    return this.displayName;
  }
  
  public ScoreType getType()
  {
    return this.type;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.name = in.readString();
    this.action = ((ObjectiveAction)MagicValues.key(ObjectiveAction.class, Byte.valueOf(in.readByte())));
    if ((this.action == ObjectiveAction.ADD) || (this.action == ObjectiveAction.UPDATE))
    {
      this.displayName = in.readString();
      this.type = ((ScoreType)MagicValues.key(ScoreType.class, in.readString()));
    }
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeString(this.name);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.action)).intValue());
    if ((this.action == ObjectiveAction.ADD) || (this.action == ObjectiveAction.UPDATE))
    {
      out.writeString(this.displayName);
      out.writeString((String)MagicValues.value(String.class, this.type));
    }
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
