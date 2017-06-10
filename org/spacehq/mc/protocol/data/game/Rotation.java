package org.spacehq.mc.protocol.data.game;

public class Rotation
{
  private float pitch;
  private float yaw;
  private float roll;
  
  public Rotation()
  {
    this(0.0F, 0.0F, 0.0F);
  }
  
  public Rotation(float pitch, float yaw, float roll)
  {
    this.pitch = pitch;
    this.yaw = yaw;
    this.roll = roll;
  }
  
  public float getPitch()
  {
    return this.pitch;
  }
  
  public float getYaw()
  {
    return this.yaw;
  }
  
  public float getRoll()
  {
    return this.roll;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    Rotation rotation = (Rotation)o;
    if (Float.compare(rotation.pitch, this.pitch) != 0) {
      return false;
    }
    if (Float.compare(rotation.roll, this.roll) != 0) {
      return false;
    }
    if (Float.compare(rotation.yaw, this.yaw) != 0) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = this.pitch != 0.0F ? Float.floatToIntBits(this.pitch) : 0;
    result = 31 * result + (this.yaw != 0.0F ? Float.floatToIntBits(this.yaw) : 0);
    result = 31 * result + (this.roll != 0.0F ? Float.floatToIntBits(this.roll) : 0);
    return result;
  }
}
