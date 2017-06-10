package org.spacehq.packetlib.io;

import java.io.IOException;
import java.util.UUID;

public abstract interface NetInput
{
  public abstract boolean readBoolean()
    throws IOException;
  
  public abstract byte readByte()
    throws IOException;
  
  public abstract int readUnsignedByte()
    throws IOException;
  
  public abstract short readShort()
    throws IOException;
  
  public abstract int readUnsignedShort()
    throws IOException;
  
  public abstract char readChar()
    throws IOException;
  
  public abstract int readInt()
    throws IOException;
  
  public abstract int readVarInt()
    throws IOException;
  
  public abstract long readLong()
    throws IOException;
  
  public abstract long readVarLong()
    throws IOException;
  
  public abstract float readFloat()
    throws IOException;
  
  public abstract double readDouble()
    throws IOException;
  
  public abstract byte[] readBytes(int paramInt)
    throws IOException;
  
  public abstract int readBytes(byte[] paramArrayOfByte)
    throws IOException;
  
  public abstract int readBytes(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException;
  
  public abstract short[] readShorts(int paramInt)
    throws IOException;
  
  public abstract int readShorts(short[] paramArrayOfShort)
    throws IOException;
  
  public abstract int readShorts(short[] paramArrayOfShort, int paramInt1, int paramInt2)
    throws IOException;
  
  public abstract int[] readInts(int paramInt)
    throws IOException;
  
  public abstract int readInts(int[] paramArrayOfInt)
    throws IOException;
  
  public abstract int readInts(int[] paramArrayOfInt, int paramInt1, int paramInt2)
    throws IOException;
  
  public abstract long[] readLongs(int paramInt)
    throws IOException;
  
  public abstract int readLongs(long[] paramArrayOfLong)
    throws IOException;
  
  public abstract int readLongs(long[] paramArrayOfLong, int paramInt1, int paramInt2)
    throws IOException;
  
  public abstract String readString()
    throws IOException;
  
  public abstract UUID readUUID()
    throws IOException;
  
  public abstract int available()
    throws IOException;
}
