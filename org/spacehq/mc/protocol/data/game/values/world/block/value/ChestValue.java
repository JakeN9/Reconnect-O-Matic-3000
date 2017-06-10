package org.spacehq.mc.protocol.data.game.values.world.block.value;

public class ChestValue
  implements BlockValue
{
  private int viewers;
  
  public ChestValue(int viewers)
  {
    this.viewers = viewers;
  }
  
  public int getViewers()
  {
    return this.viewers;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    ChestValue that = (ChestValue)o;
    if (this.viewers != that.viewers) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return this.viewers;
  }
}
