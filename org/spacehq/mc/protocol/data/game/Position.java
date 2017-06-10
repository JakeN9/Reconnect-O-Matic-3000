package org.spacehq.mc.protocol.data.game;

public class Position
{
  private int x;
  private int y;
  private int z;
  
  public Position(int x, int y, int z)
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
    Position position = (Position)o;
    if (this.x != position.x) {
      return false;
    }
    if (this.y != position.y) {
      return false;
    }
    if (this.z != position.z) {
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
