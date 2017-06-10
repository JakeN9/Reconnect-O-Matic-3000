package org.spacehq.mc.protocol.data.game.values.world.notify;

public class ThunderStrengthValue
  implements ClientNotificationValue
{
  private float strength;
  
  public ThunderStrengthValue(float strength)
  {
    if (strength > 1.0F) {
      strength = 1.0F;
    }
    if (strength < 0.0F) {
      strength = 0.0F;
    }
    this.strength = strength;
  }
  
  public float getStrength()
  {
    return this.strength;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    ThunderStrengthValue that = (ThunderStrengthValue)o;
    if (Float.compare(that.strength, this.strength) != 0) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return this.strength != 0.0F ? Float.floatToIntBits(this.strength) : 0;
  }
}
