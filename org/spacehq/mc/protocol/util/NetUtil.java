package org.spacehq.mc.protocol.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import org.spacehq.mc.protocol.data.game.Chunk;
import org.spacehq.mc.protocol.data.game.EntityMetadata;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.mc.protocol.data.game.NibbleArray3d;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.Rotation;
import org.spacehq.mc.protocol.data.game.ShortArray3d;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.MetadataType;
import org.spacehq.opennbt.NBTIO;
import org.spacehq.opennbt.tag.builtin.CompoundTag;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;

public class NetUtil
{
  private static final int POSITION_X_SIZE = 38;
  private static final int POSITION_Y_SIZE = 26;
  private static final int POSITION_Z_SIZE = 38;
  private static final int POSITION_Y_SHIFT = 4095;
  private static final int POSITION_WRITE_SHIFT = 67108863;
  
  public static CompoundTag readNBT(NetInput in)
    throws IOException
  {
    byte b = in.readByte();
    if (b == 0) {
      return null;
    }
    return (CompoundTag)NBTIO.readTag(new DataInputStream(new NetInputStream(in, b)));
  }
  
  public static void writeNBT(NetOutput out, CompoundTag tag)
    throws IOException
  {
    if (tag == null) {
      out.writeByte(0);
    } else {
      NBTIO.writeTag(new DataOutputStream(new NetOutputStream(out)), tag);
    }
  }
  
  public static Position readPosition(NetInput in)
    throws IOException
  {
    long val = in.readLong();
    
    int x = (int)(val >> 38);
    int y = (int)(val >> 26 & 0xFFF);
    int z = (int)(val << 38 >> 38);
    
    return new Position(x, y, z);
  }
  
  public static void writePosition(NetOutput out, Position pos)
    throws IOException
  {
    long x = pos.getX() & 0x3FFFFFF;
    long y = pos.getY() & 0xFFF;
    long z = pos.getZ() & 0x3FFFFFF;
    
    out.writeLong(x << 38 | y << 26 | z);
  }
  
  public static ItemStack readItem(NetInput in)
    throws IOException
  {
    short item = in.readShort();
    if (item < 0) {
      return null;
    }
    return new ItemStack(item, in.readByte(), in.readShort(), readNBT(in));
  }
  
  public static void writeItem(NetOutput out, ItemStack item)
    throws IOException
  {
    if (item == null)
    {
      out.writeShort(-1);
    }
    else
    {
      out.writeShort(item.getId());
      out.writeByte(item.getAmount());
      out.writeShort(item.getData());
      writeNBT(out, item.getNBT());
    }
  }
  
  public static EntityMetadata[] readEntityMetadata(NetInput in)
    throws IOException
  {
    List<EntityMetadata> ret = new ArrayList();
    byte b;
    while ((b = in.readByte()) != Byte.MAX_VALUE)
    {
      int typeId = (b & 0xE0) >> 5;
      int id = b & 0x1F;
      MetadataType type = (MetadataType)MagicValues.key(MetadataType.class, Integer.valueOf(typeId));
      Object value;
      Object value;
      Object value;
      Object value;
      Object value;
      Object value;
      Object value;
      Object value;
      switch (type)
      {
      case BYTE: 
        value = Byte.valueOf(in.readByte());
        break;
      case SHORT: 
        value = Short.valueOf(in.readShort());
        break;
      case INT: 
        value = Integer.valueOf(in.readInt());
        break;
      case FLOAT: 
        value = Float.valueOf(in.readFloat());
        break;
      case STRING: 
        value = in.readString();
        break;
      case ITEM: 
        value = readItem(in);
        break;
      case POSITION: 
        value = new Position(in.readInt(), in.readInt(), in.readInt());
        break;
      case ROTATION: 
        value = new Rotation(in.readFloat(), in.readFloat(), in.readFloat());
        break;
      default: 
        throw new IOException("Unknown metadata type id: " + typeId);
      }
      Object value;
      ret.add(new EntityMetadata(id, type, value));
    }
    return (EntityMetadata[])ret.toArray(new EntityMetadata[ret.size()]);
  }
  
  public static void writeEntityMetadata(NetOutput out, EntityMetadata[] metadata)
    throws IOException
  {
    for (EntityMetadata meta : metadata)
    {
      int id = ((Integer)MagicValues.value(Integer.class, meta.getType())).intValue() << 5 | meta.getId() & 0x1F;
      out.writeByte(id);
      switch (meta.getType())
      {
      case BYTE: 
        out.writeByte(((Byte)meta.getValue()).byteValue());
        break;
      case SHORT: 
        out.writeShort(((Short)meta.getValue()).shortValue());
        break;
      case INT: 
        out.writeInt(((Integer)meta.getValue()).intValue());
        break;
      case FLOAT: 
        out.writeFloat(((Float)meta.getValue()).floatValue());
        break;
      case STRING: 
        out.writeString((String)meta.getValue());
        break;
      case ITEM: 
        writeItem(out, (ItemStack)meta.getValue());
        break;
      case POSITION: 
        Position pos = (Position)meta.getValue();
        out.writeInt(pos.getX());
        out.writeInt(pos.getY());
        out.writeInt(pos.getZ());
        break;
      case ROTATION: 
        Rotation rot = (Rotation)meta.getValue();
        out.writeFloat(rot.getPitch());
        out.writeFloat(rot.getYaw());
        out.writeFloat(rot.getRoll());
        break;
      default: 
        throw new IOException("Unmapped metadata type: " + meta.getType());
      }
    }
    out.writeByte(127);
  }
  
  public static ParsedChunkData dataToChunks(NetworkChunkData data, boolean checkForSky)
  {
    Chunk[] chunks = new Chunk[16];
    int pos = 0;
    int expected = 0;
    boolean sky = false;
    ShortBuffer buf = ByteBuffer.wrap(data.getData()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    for (int pass = 0; pass < 4; pass++)
    {
      for (int ind = 0; ind < 16; ind++) {
        if ((data.getMask() & 1 << ind) != 0)
        {
          if (pass == 0) {
            expected += 10240;
          }
          if (pass == 1)
          {
            chunks[ind] = new Chunk((sky) || (data.hasSkyLight()));
            ShortArray3d blocks = chunks[ind].getBlocks();
            buf.position(pos / 2);
            buf.get(blocks.getData(), 0, blocks.getData().length);
            pos += blocks.getData().length * 2;
          }
          if (pass == 2)
          {
            NibbleArray3d blocklight = chunks[ind].getBlockLight();
            System.arraycopy(data.getData(), pos, blocklight.getData(), 0, blocklight.getData().length);
            pos += blocklight.getData().length;
          }
          if ((pass == 3) && ((sky) || (data.hasSkyLight())))
          {
            NibbleArray3d skylight = chunks[ind].getSkyLight();
            System.arraycopy(data.getData(), pos, skylight.getData(), 0, skylight.getData().length);
            pos += skylight.getData().length;
          }
        }
      }
      if (pass == 0) {
        if (data.getData().length > expected) {
          sky = checkForSky;
        }
      }
    }
    byte[] biomeData = null;
    if (data.isFullChunk())
    {
      biomeData = new byte['Ā'];
      System.arraycopy(data.getData(), pos, biomeData, 0, biomeData.length);
      pos += biomeData.length;
    }
    return new ParsedChunkData(chunks, biomeData);
  }
  
  public static NetworkChunkData chunksToData(ParsedChunkData chunks)
  {
    int chunkMask = 0;
    boolean fullChunk = chunks.getBiomes() != null;
    boolean sky = false;
    int length = fullChunk ? chunks.getBiomes().length : 0;
    byte[] data = null;
    int pos = 0;
    ShortBuffer buf = null;
    for (int pass = 0; pass < 4; pass++)
    {
      for (int ind = 0; ind < chunks.getChunks().length; ind++)
      {
        Chunk chunk = chunks.getChunks()[ind];
        if ((chunk != null) && ((!fullChunk) || (!chunk.isEmpty())))
        {
          if (pass == 0)
          {
            chunkMask |= 1 << ind;
            length += chunk.getBlocks().getData().length * 2;
            length += chunk.getBlockLight().getData().length;
            if (chunk.getSkyLight() != null) {
              length += chunk.getSkyLight().getData().length;
            }
          }
          if (pass == 1)
          {
            short[] blocks = chunk.getBlocks().getData();
            buf.position(pos / 2);
            buf.put(blocks, 0, blocks.length);
            pos += blocks.length * 2;
          }
          if (pass == 2)
          {
            byte[] blocklight = chunk.getBlockLight().getData();
            System.arraycopy(blocklight, 0, data, pos, blocklight.length);
            pos += blocklight.length;
          }
          if ((pass == 3) && (chunk.getSkyLight() != null))
          {
            byte[] skylight = chunk.getSkyLight().getData();
            System.arraycopy(skylight, 0, data, pos, skylight.length);
            pos += skylight.length;
            sky = true;
          }
        }
      }
      if (pass == 0)
      {
        data = new byte[length];
        buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
      }
    }
    if (fullChunk)
    {
      System.arraycopy(chunks.getBiomes(), 0, data, pos, chunks.getBiomes().length);
      pos += chunks.getBiomes().length;
    }
    return new NetworkChunkData(chunkMask, fullChunk, sky, data);
  }
  
  private static class NetInputStream
    extends InputStream
  {
    private NetInput in;
    private boolean readFirst;
    private byte firstByte;
    
    public NetInputStream(NetInput in, byte firstByte)
    {
      this.in = in;
      this.firstByte = firstByte;
    }
    
    public int read()
      throws IOException
    {
      if (!this.readFirst)
      {
        this.readFirst = true;
        return this.firstByte;
      }
      return this.in.readUnsignedByte();
    }
  }
  
  private static class NetOutputStream
    extends OutputStream
  {
    private NetOutput out;
    
    public NetOutputStream(NetOutput out)
    {
      this.out = out;
    }
    
    public void write(int b)
      throws IOException
    {
      this.out.writeByte(b);
    }
  }
}
