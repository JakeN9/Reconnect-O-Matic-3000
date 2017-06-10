package org.spacehq.mc.protocol.data.game;

import java.util.Arrays;

public class ShortArray3d
{
  private short[] data;
  
  public ShortArray3d(int size)
  {
    this.data = new short[size];
  }
  
  public ShortArray3d(short[] array)
  {
    this.data = array;
  }
  
  public short[] getData()
  {
    return this.data;
  }
  
  public int get(int x, int y, int z)
  {
    return this.data[(y << 8 | z << 4 | x)] & 0xFFFF;
  }
  
  public void set(int x, int y, int z, int val)
  {
    this.data[(y << 8 | z << 4 | x)] = ((short)val);
  }
  
  public int getBlock(int x, int y, int z)
  {
    return get(x, y, z) >> 4;
  }
  
  public void setBlock(int x, int y, int z, int block)
  {
    set(x, y, z, block << 4 | getData(x, y, z));
  }
  
  public int getData(int x, int y, int z)
  {
    return get(x, y, z) & 0xF;
  }
  
  public void setData(int x, int y, int z, int data)
  {
    set(x, y, z, getBlock(x, y, z) << 4 | data);
  }
  
  public void setBlockAndData(int x, int y, int z, int block, int data)
  {
    set(x, y, z, block << 4 | data);
  }
  
  public void fill(int val)
  {
    Arrays.fill(this.data, (short)val);
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    ShortArray3d that = (ShortArray3d)o;
    if (!Arrays.equals(this.data, that.data)) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return Arrays.hashCode(this.data);
  }
}
