package org.spacehq.mc.protocol.packet.ingame.server.world;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.world.block.value.BlockValue;
import org.spacehq.mc.protocol.data.game.values.world.block.value.BlockValueType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.ChestValue;
import org.spacehq.mc.protocol.data.game.values.world.block.value.ChestValueType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.GenericBlockValue;
import org.spacehq.mc.protocol.data.game.values.world.block.value.GenericBlockValueType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.MobSpawnerValue;
import org.spacehq.mc.protocol.data.game.values.world.block.value.MobSpawnerValueType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.NoteBlockValue;
import org.spacehq.mc.protocol.data.game.values.world.block.value.NoteBlockValueType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.PistonValue;
import org.spacehq.mc.protocol.data.game.values.world.block.value.PistonValueType;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerBlockValuePacket
  implements Packet
{
  private static final int NOTE_BLOCK = 25;
  private static final int STICKY_PISTON = 29;
  private static final int PISTON = 33;
  private static final int MOB_SPAWNER = 52;
  private static final int CHEST = 54;
  private static final int ENDER_CHEST = 130;
  private static final int TRAPPED_CHEST = 146;
  private Position position;
  private BlockValueType type;
  private BlockValue value;
  private int blockId;
  
  private ServerBlockValuePacket() {}
  
  public ServerBlockValuePacket(Position position, BlockValueType type, BlockValue value, int blockId)
  {
    this.position = position;
    this.type = type;
    this.value = value;
    this.blockId = blockId;
  }
  
  public Position getPosition()
  {
    return this.position;
  }
  
  public BlockValueType getType()
  {
    return this.type;
  }
  
  public BlockValue getValue()
  {
    return this.value;
  }
  
  public int getBlockId()
  {
    return this.blockId;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.position = NetUtil.readPosition(in);
    int type = in.readUnsignedByte();
    if (this.blockId == 25) {
      this.type = ((BlockValueType)MagicValues.key(NoteBlockValueType.class, Integer.valueOf(type)));
    } else if ((this.blockId == 29) || (this.blockId == 33)) {
      this.type = ((BlockValueType)MagicValues.key(PistonValueType.class, Integer.valueOf(type)));
    } else if (this.blockId == 52) {
      this.type = ((BlockValueType)MagicValues.key(MobSpawnerValueType.class, Integer.valueOf(type)));
    } else if ((this.blockId == 54) || (this.blockId == 130) || (this.blockId == 146)) {
      this.type = ((BlockValueType)MagicValues.key(ChestValueType.class, Integer.valueOf(type)));
    } else {
      this.type = ((BlockValueType)MagicValues.key(GenericBlockValueType.class, Integer.valueOf(type)));
    }
    int value = in.readUnsignedByte();
    if (this.blockId == 25) {
      this.value = new NoteBlockValue(value);
    } else if ((this.blockId == 29) || (this.blockId == 33)) {
      this.value = ((BlockValue)MagicValues.key(PistonValue.class, Integer.valueOf(value)));
    } else if (this.blockId == 52) {
      this.value = new MobSpawnerValue();
    } else if ((this.blockId == 54) || (this.blockId == 130) || (this.blockId == 146)) {
      this.value = new ChestValue(value);
    } else {
      this.value = new GenericBlockValue(value);
    }
    this.blockId = (in.readVarInt() & 0xFFF);
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    NetUtil.writePosition(out, this.position);
    int type = 0;
    if ((this.type instanceof NoteBlockValueType)) {
      type = ((Integer)MagicValues.value(Integer.class, (NoteBlockValueType)this.type)).intValue();
    } else if ((this.type instanceof PistonValueType)) {
      type = ((Integer)MagicValues.value(Integer.class, (PistonValueType)this.type)).intValue();
    } else if ((this.type instanceof MobSpawnerValueType)) {
      type = ((Integer)MagicValues.value(Integer.class, (MobSpawnerValueType)this.type)).intValue();
    } else if ((this.type instanceof ChestValueType)) {
      type = ((Integer)MagicValues.value(Integer.class, (ChestValueType)this.type)).intValue();
    } else if ((this.type instanceof GenericBlockValueType)) {
      type = ((Integer)MagicValues.value(Integer.class, (GenericBlockValueType)this.type)).intValue();
    }
    out.writeByte(type);
    int val = 0;
    if ((this.value instanceof NoteBlockValue)) {
      val = ((NoteBlockValue)this.value).getPitch();
    } else if ((this.value instanceof PistonValue)) {
      val = ((Integer)MagicValues.value(Integer.class, (PistonValue)this.value)).intValue();
    } else if ((this.value instanceof MobSpawnerValue)) {
      val = 0;
    } else if ((this.value instanceof ChestValue)) {
      val = ((ChestValue)this.value).getViewers();
    } else if ((this.value instanceof GenericBlockValue)) {
      val = ((GenericBlockValue)this.value).getValue();
    }
    out.writeByte(val);
    out.writeVarInt(this.blockId & 0xFFF);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
