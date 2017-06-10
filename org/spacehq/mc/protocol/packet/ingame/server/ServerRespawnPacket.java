package org.spacehq.mc.protocol.packet.ingame.server;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.data.game.values.setting.Difficulty;
import org.spacehq.mc.protocol.data.game.values.world.WorldType;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerRespawnPacket
  implements Packet
{
  private int dimension;
  private Difficulty difficulty;
  private GameMode gamemode;
  private WorldType worldType;
  
  private ServerRespawnPacket() {}
  
  public ServerRespawnPacket(int dimension, Difficulty difficulty, GameMode gamemode, WorldType worldType)
  {
    this.dimension = dimension;
    this.difficulty = difficulty;
    this.gamemode = gamemode;
    this.worldType = worldType;
  }
  
  public int getDimension()
  {
    return this.dimension;
  }
  
  public Difficulty getDifficulty()
  {
    return this.difficulty;
  }
  
  public GameMode getGameMode()
  {
    return this.gamemode;
  }
  
  public WorldType getWorldType()
  {
    return this.worldType;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.dimension = in.readInt();
    this.difficulty = ((Difficulty)MagicValues.key(Difficulty.class, Integer.valueOf(in.readUnsignedByte())));
    this.gamemode = ((GameMode)MagicValues.key(GameMode.class, Integer.valueOf(in.readUnsignedByte())));
    this.worldType = ((WorldType)MagicValues.key(WorldType.class, in.readString().toLowerCase()));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeInt(this.dimension);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.difficulty)).intValue());
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.gamemode)).intValue());
    out.writeString((String)MagicValues.value(String.class, this.worldType));
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
