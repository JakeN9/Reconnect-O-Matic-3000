package org.spacehq.mc.protocol.data.game.values.entity;

public class SplashPotionData
  implements ObjectData
{
  private int potionData;
  
  public SplashPotionData(int potionData)
  {
    this.potionData = potionData;
  }
  
  public int getPotionData()
  {
    return this.potionData;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    SplashPotionData that = (SplashPotionData)o;
    if (this.potionData != that.potionData) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return this.potionData;
  }
}
