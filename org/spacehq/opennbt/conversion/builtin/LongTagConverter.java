package org.spacehq.opennbt.conversion.builtin;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.LongTag;

public class LongTagConverter
  implements TagConverter<LongTag, Long>
{
  public Long convert(LongTag tag)
  {
    return tag.getValue();
  }
  
  public LongTag convert(String name, Long value)
  {
    return new LongTag(name, value.longValue());
  }
}
