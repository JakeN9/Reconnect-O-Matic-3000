package org.spacehq.mc.protocol.util;

import java.util.Arrays;
import org.spacehq.mc.protocol.data.game.Chunk;

public class ParsedChunkData
{
  private Chunk[] chunks;
  private byte[] biomes;
  
  public ParsedChunkData(Chunk[] chunks, byte[] biomes)
  {
    this.chunks = chunks;
    this.biomes = biomes;
  }
  
  public Chunk[] getChunks()
  {
    return this.chunks;
  }
  
  public byte[] getBiomes()
  {
    return this.biomes;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    ParsedChunkData that = (ParsedChunkData)o;
    if (!Arrays.equals(this.biomes, that.biomes)) {
      return false;
    }
    if (!Arrays.equals(this.chunks, that.chunks)) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = Arrays.hashCode(this.chunks);
    result = 31 * result + (this.biomes != null ? Arrays.hashCode(this.biomes) : 0);
    return result;
  }
}
