package org.spacehq.opennbt.conversion.builtin;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.FloatTag;

public class FloatTagConverter
  implements TagConverter<FloatTag, Float>
{
  public Float convert(FloatTag tag)
  {
    return tag.getValue();
  }
  
  public FloatTag convert(String name, Float value)
  {
    return new FloatTag(name, value.floatValue());
  }
}
