package org.spacehq.mc.protocol.packet.ingame.server.world;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.Chunk;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.mc.protocol.util.NetworkChunkData;
import org.spacehq.mc.protocol.util.ParsedChunkData;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerChunkDataPacket
  implements Packet
{
  private int x;
  private int z;
  private Chunk[] chunks;
  private byte[] biomeData;
  
  private ServerChunkDataPacket() {}
  
  public ServerChunkDataPacket(int x, int z)
  {
    this(x, z, new Chunk[16], new byte['Ā']);
  }
  
  public ServerChunkDataPacket(int x, int z, Chunk[] chunks)
  {
    this(x, z, chunks, null);
  }
  
  public ServerChunkDataPacket(int x, int z, Chunk[] chunks, byte[] biomeData)
  {
    if (chunks.length != 16) {
      throw new IllegalArgumentException("Chunks length must be 16.");
    }
    boolean noSkylight = false;
    boolean skylight = false;
    for (int index = 0; index < chunks.length; index++) {
      if (chunks[index] != null) {
        if (chunks[index].getSkyLight() == null) {
          noSkylight = true;
        } else {
          skylight = true;
        }
      }
    }
    if ((noSkylight) && (skylight)) {
      throw new IllegalArgumentException("Either all chunks must have skylight values or none must have them.");
    }
    this.x = x;
    this.z = z;
    this.chunks = chunks;
    this.biomeData = biomeData;
  }
  
  public int getX()
  {
    return this.x;
  }
  
  public int getZ()
  {
    return this.z;
  }
  
  public Chunk[] getChunks()
  {
    return this.chunks;
  }
  
  public byte[] getBiomeData()
  {
    return this.biomeData;
  }
  
  public boolean isFullChunk()
  {
    return this.biomeData != null;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.x = in.readInt();
    this.z = in.readInt();
    boolean fullChunk = in.readBoolean();
    int chunkMask = in.readUnsignedShort();
    byte[] data = in.readBytes(in.readVarInt());
    ParsedChunkData chunkData = NetUtil.dataToChunks(new NetworkChunkData(chunkMask, fullChunk, false, data), true);
    this.chunks = chunkData.getChunks();
    this.biomeData = chunkData.getBiomes();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    NetworkChunkData data = NetUtil.chunksToData(new ParsedChunkData(this.chunks, this.biomeData));
    out.writeInt(this.x);
    out.writeInt(this.z);
    out.writeBoolean(data.isFullChunk());
    out.writeShort(data.getMask());
    out.writeVarInt(data.getData().length);
    out.writeBytes(data.getData(), data.getData().length);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
