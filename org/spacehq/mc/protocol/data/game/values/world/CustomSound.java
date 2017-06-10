package org.spacehq.mc.protocol.data.game.values.world;

public class CustomSound
  implements Sound
{
  private String name;
  
  public CustomSound(String name)
  {
    this.name = name;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    CustomSound that = (CustomSound)o;
    if (!this.name.equals(that.name)) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return this.name.hashCode();
  }
}
