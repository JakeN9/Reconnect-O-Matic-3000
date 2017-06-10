package org.spacehq.opennbt.conversion.builtin;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.DoubleTag;

public class DoubleTagConverter
  implements TagConverter<DoubleTag, Double>
{
  public Double convert(DoubleTag tag)
  {
    return tag.getValue();
  }
  
  public DoubleTag convert(String name, Double value)
  {
    return new DoubleTag(name, value.doubleValue());
  }
}
