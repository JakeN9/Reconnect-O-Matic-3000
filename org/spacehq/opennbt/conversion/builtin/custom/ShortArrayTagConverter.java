package org.spacehq.opennbt.conversion.builtin.custom;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.custom.ShortArrayTag;

public class ShortArrayTagConverter
  implements TagConverter<ShortArrayTag, short[]>
{
  public short[] convert(ShortArrayTag tag)
  {
    return tag.getValue();
  }
  
  public ShortArrayTag convert(String name, short[] value)
  {
    return new ShortArrayTag(name, value);
  }
}
