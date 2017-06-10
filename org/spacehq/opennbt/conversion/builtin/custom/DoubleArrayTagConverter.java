package org.spacehq.opennbt.conversion.builtin.custom;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.custom.DoubleArrayTag;

public class DoubleArrayTagConverter
  implements TagConverter<DoubleArrayTag, double[]>
{
  public double[] convert(DoubleArrayTag tag)
  {
    return tag.getValue();
  }
  
  public DoubleArrayTag convert(String name, double[] value)
  {
    return new DoubleArrayTag(name, value);
  }
}
