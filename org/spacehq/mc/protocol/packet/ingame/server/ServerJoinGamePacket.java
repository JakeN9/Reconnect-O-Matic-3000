package org.spacehq.mc.protocol.packet.ingame.server;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.data.game.values.setting.Difficulty;
import org.spacehq.mc.protocol.data.game.values.world.WorldType;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerJoinGamePacket
  implements Packet
{
  private int entityId;
  private boolean hardcore;
  private GameMode gamemode;
  private int dimension;
  private Difficulty difficulty;
  private int maxPlayers;
  private WorldType worldType;
  private boolean reducedDebugInfo;
  
  private ServerJoinGamePacket() {}
  
  public ServerJoinGamePacket(int entityId, boolean hardcore, GameMode gamemode, int dimension, Difficulty difficulty, int maxPlayers, WorldType worldType, boolean reducedDebugInfo)
  {
    this.entityId = entityId;
    this.hardcore = hardcore;
    this.gamemode = gamemode;
    this.dimension = dimension;
    this.difficulty = difficulty;
    this.maxPlayers = maxPlayers;
    this.worldType = worldType;
    this.reducedDebugInfo = reducedDebugInfo;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public boolean getHardcore()
  {
    return this.hardcore;
  }
  
  public GameMode getGameMode()
  {
    return this.gamemode;
  }
  
  public int getDimension()
  {
    return this.dimension;
  }
  
  public Difficulty getDifficulty()
  {
    return this.difficulty;
  }
  
  public int getMaxPlayers()
  {
    return this.maxPlayers;
  }
  
  public WorldType getWorldType()
  {
    return this.worldType;
  }
  
  public boolean getReducedDebugInfo()
  {
    return this.reducedDebugInfo;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readInt();
    int gamemode = in.readUnsignedByte();
    this.hardcore = ((gamemode & 0x8) == 8);
    gamemode &= 0xFFFFFFF7;
    this.gamemode = ((GameMode)MagicValues.key(GameMode.class, Integer.valueOf(gamemode)));
    this.dimension = in.readByte();
    this.difficulty = ((Difficulty)MagicValues.key(Difficulty.class, Integer.valueOf(in.readUnsignedByte())));
    this.maxPlayers = in.readUnsignedByte();
    this.worldType = ((WorldType)MagicValues.key(WorldType.class, in.readString().toLowerCase()));
    this.reducedDebugInfo = in.readBoolean();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeInt(this.entityId);
    int gamemode = ((Integer)MagicValues.value(Integer.class, this.gamemode)).intValue();
    if (this.hardcore) {
      gamemode |= 0x8;
    }
    out.writeByte(gamemode);
    out.writeByte(this.dimension);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.difficulty)).intValue());
    out.writeByte(this.maxPlayers);
    out.writeString((String)MagicValues.value(String.class, this.worldType));
    out.writeBoolean(this.reducedDebugInfo);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
