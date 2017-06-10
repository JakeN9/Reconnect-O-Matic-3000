package org.spacehq.mc.protocol.data.game.values.world.effect;

public class HardLandingEffectData
  implements WorldEffectData
{
  private int damagingDistance;
  
  public HardLandingEffectData(int damagingDistance)
  {
    this.damagingDistance = damagingDistance;
  }
  
  public int getDamagingDistance()
  {
    return this.damagingDistance;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    HardLandingEffectData that = (HardLandingEffectData)o;
    if (this.damagingDistance != that.damagingDistance) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return this.damagingDistance;
  }
}
