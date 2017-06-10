package org.spacehq.opennbt.conversion.builtin.custom;

import java.io.Serializable;
import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.custom.SerializableTag;

public class SerializableTagConverter
  implements TagConverter<SerializableTag, Serializable>
{
  public Serializable convert(SerializableTag tag)
  {
    return tag.getValue();
  }
  
  public SerializableTag convert(String name, Serializable value)
  {
    return new SerializableTag(name, value);
  }
}
