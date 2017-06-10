package org.spacehq.opennbt.conversion.builtin.custom;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.custom.FloatArrayTag;

public class FloatArrayTagConverter
  implements TagConverter<FloatArrayTag, float[]>
{
  public float[] convert(FloatArrayTag tag)
  {
    return tag.getValue();
  }
  
  public FloatArrayTag convert(String name, float[] value)
  {
    return new FloatArrayTag(name, value);
  }
}
