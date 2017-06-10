package org.spacehq.mc.protocol.data.game;

import java.util.Arrays;

public class NibbleArray3d
{
  private byte[] data;
  
  public NibbleArray3d(int size)
  {
    this.data = new byte[size >> 1];
  }
  
  public NibbleArray3d(byte[] array)
  {
    this.data = array;
  }
  
  public byte[] getData()
  {
    return this.data;
  }
  
  public int get(int x, int y, int z)
  {
    int key = y << 8 | z << 4 | x;
    int index = key >> 1;
    int part = key & 0x1;
    return part == 0 ? this.data[index] & 0xF : this.data[index] >> 4 & 0xF;
  }
  
  public void set(int x, int y, int z, int val)
  {
    int key = y << 8 | z << 4 | x;
    int index = key >> 1;
    int part = key & 0x1;
    if (part == 0) {
      this.data[index] = ((byte)(this.data[index] & 0xF0 | val & 0xF));
    } else {
      this.data[index] = ((byte)(this.data[index] & 0xF | (val & 0xF) << 4));
    }
  }
  
  public void fill(int val)
  {
    for (int index = 0; index < this.data.length << 1; index++)
    {
      int ind = index >> 1;
      int part = index & 0x1;
      if (part == 0) {
        this.data[ind] = ((byte)(this.data[ind] & 0xF0 | val & 0xF));
      } else {
        this.data[ind] = ((byte)(this.data[ind] & 0xF | (val & 0xF) << 4));
      }
    }
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    NibbleArray3d that = (NibbleArray3d)o;
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
