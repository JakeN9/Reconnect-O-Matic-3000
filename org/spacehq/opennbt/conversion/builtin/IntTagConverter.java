package org.spacehq.opennbt.conversion.builtin;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.IntTag;

public class IntTagConverter
  implements TagConverter<IntTag, Integer>
{
  public Integer convert(IntTag tag)
  {
    return tag.getValue();
  }
  
  public IntTag convert(String name, Integer value)
  {
    return new IntTag(name, value.intValue());
  }
}
