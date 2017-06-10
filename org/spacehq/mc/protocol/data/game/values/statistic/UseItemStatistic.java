package org.spacehq.mc.protocol.data.game.values.statistic;

public class UseItemStatistic
  implements Statistic
{
  private int id;
  
  public UseItemStatistic(int id)
  {
    this.id = id;
  }
  
  public int getId()
  {
    return this.id;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    UseItemStatistic that = (UseItemStatistic)o;
    if (this.id != that.id) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return this.id;
  }
}
