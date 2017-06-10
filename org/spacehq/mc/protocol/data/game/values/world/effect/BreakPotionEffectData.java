package org.spacehq.mc.protocol.data.game.values.world.effect;

public class BreakPotionEffectData
  implements WorldEffectData
{
  private int potionId;
  
  public BreakPotionEffectData(int potionId)
  {
    this.potionId = potionId;
  }
  
  public int getPotionId()
  {
    return this.potionId;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    BreakPotionEffectData that = (BreakPotionEffectData)o;
    if (this.potionId != that.potionId) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return this.potionId;
  }
}
