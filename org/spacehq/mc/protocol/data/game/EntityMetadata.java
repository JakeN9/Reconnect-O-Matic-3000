package org.spacehq.mc.protocol.data.game;

import org.spacehq.mc.protocol.data.game.values.entity.MetadataType;

public class EntityMetadata
{
  private int id;
  private MetadataType type;
  private Object value;
  
  public EntityMetadata(int id, MetadataType type, Object value)
  {
    this.id = id;
    this.type = type;
    this.value = value;
  }
  
  public int getId()
  {
    return this.id;
  }
  
  public MetadataType getType()
  {
    return this.type;
  }
  
  public Object getValue()
  {
    return this.value;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    EntityMetadata metadata = (EntityMetadata)o;
    if (this.id != metadata.id) {
      return false;
    }
    if (this.type != metadata.type) {
      return false;
    }
    if (!this.value.equals(metadata.value)) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = this.id;
    result = 31 * result + this.type.hashCode();
    result = 31 * result + this.value.hashCode();
    return result;
  }
}
