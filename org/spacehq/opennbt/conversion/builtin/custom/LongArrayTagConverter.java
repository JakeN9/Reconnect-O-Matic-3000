package org.spacehq.opennbt.conversion.builtin.custom;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.custom.LongArrayTag;

public class LongArrayTagConverter
  implements TagConverter<LongArrayTag, long[]>
{
  public long[] convert(LongArrayTag tag)
  {
    return tag.getValue();
  }
  
  public LongArrayTag convert(String name, long[] value)
  {
    return new LongArrayTag(name, value);
  }
}
