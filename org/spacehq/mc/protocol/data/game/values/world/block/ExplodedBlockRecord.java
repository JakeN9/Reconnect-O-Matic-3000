package org.spacehq.mc.protocol.data.game.values.world.block;

public class ExplodedBlockRecord
{
  private int x;
  private int y;
  private int z;
  
  public ExplodedBlockRecord(int x, int y, int z)
  {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  public int getX()
  {
    return this.x;
  }
  
  public int getY()
  {
    return this.y;
  }
  
  public int getZ()
  {
    return this.z;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    ExplodedBlockRecord that = (ExplodedBlockRecord)o;
    if (this.x != that.x) {
      return false;
    }
    if (this.y != that.y) {
      return false;
    }
    if (this.z != that.z) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = this.x;
    result = 31 * result + this.y;
    result = 31 * result + this.z;
    return result;
  }
}
