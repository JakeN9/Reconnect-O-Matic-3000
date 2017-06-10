package org.spacehq.opennbt.conversion.builtin.custom;

import java.io.Serializable;
import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.custom.SerializableArrayTag;

public class SerializableArrayTagConverter
  implements TagConverter<SerializableArrayTag, Serializable[]>
{
  public Serializable[] convert(SerializableArrayTag tag)
  {
    return tag.getValue();
  }
  
  public SerializableArrayTag convert(String name, Serializable[] value)
  {
    return new SerializableArrayTag(name, value);
  }
}
