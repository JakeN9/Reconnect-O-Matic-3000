package org.spacehq.packetlib.io;

import java.io.IOException;
import java.util.UUID;

public abstract interface NetOutput
{
  public abstract void writeBoolean(boolean paramBoolean)
    throws IOException;
  
  public abstract void writeByte(int paramInt)
    throws IOException;
  
  public abstract void writeShort(int paramInt)
    throws IOException;
  
  public abstract void writeChar(int paramInt)
    throws IOException;
  
  public abstract void writeInt(int paramInt)
    throws IOException;
  
  public abstract void writeVarInt(int paramInt)
    throws IOException;
  
  public abstract void writeLong(long paramLong)
    throws IOException;
  
  public abstract void writeVarLong(long paramLong)
    throws IOException;
  
  public abstract void writeFloat(float paramFloat)
    throws IOException;
  
  public abstract void writeDouble(double paramDouble)
    throws IOException;
  
  public abstract void writeBytes(byte[] paramArrayOfByte)
    throws IOException;
  
  public abstract void writeBytes(byte[] paramArrayOfByte, int paramInt)
    throws IOException;
  
  public abstract void writeShorts(short[] paramArrayOfShort)
    throws IOException;
  
  public abstract void writeShorts(short[] paramArrayOfShort, int paramInt)
    throws IOException;
  
  public abstract void writeInts(int[] paramArrayOfInt)
    throws IOException;
  
  public abstract void writeInts(int[] paramArrayOfInt, int paramInt)
    throws IOException;
  
  public abstract void writeLongs(long[] paramArrayOfLong)
    throws IOException;
  
  public abstract void writeLongs(long[] paramArrayOfLong, int paramInt)
    throws IOException;
  
  public abstract void writeString(String paramString)
    throws IOException;
  
  public abstract void writeUUID(UUID paramUUID)
    throws IOException;
  
  public abstract void flush()
    throws IOException;
}
