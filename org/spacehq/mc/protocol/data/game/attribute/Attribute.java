package org.spacehq.mc.protocol.data.game.attribute;

import java.util.ArrayList;
import java.util.List;
import org.spacehq.mc.protocol.data.game.values.entity.AttributeType;

public class Attribute
{
  private AttributeType type;
  private double value;
  private List<AttributeModifier> modifiers;
  
  public Attribute(AttributeType type)
  {
    this(type, type.getDefault());
  }
  
  public Attribute(AttributeType type, double value)
  {
    this(type, value, new ArrayList());
  }
  
  public Attribute(AttributeType type, double value, List<AttributeModifier> modifiers)
  {
    this.type = type;
    this.value = value;
    this.modifiers = modifiers;
  }
  
  public AttributeType getType()
  {
    return this.type;
  }
  
  public double getValue()
  {
    return this.value;
  }
  
  public List<AttributeModifier> getModifiers()
  {
    return new ArrayList(this.modifiers);
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    Attribute attribute = (Attribute)o;
    if (Double.compare(attribute.value, this.value) != 0) {
      return false;
    }
    if (!this.modifiers.equals(attribute.modifiers)) {
      return false;
    }
    if (this.type != attribute.type) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = this.type.hashCode();
    long temp = Double.doubleToLongBits(this.value);
    result = 31 * result + (int)(temp ^ temp >>> 32);
    result = 31 * result + this.modifiers.hashCode();
    return result;
  }
}
