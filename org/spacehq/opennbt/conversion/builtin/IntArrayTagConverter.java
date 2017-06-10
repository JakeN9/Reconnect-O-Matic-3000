package org.spacehq.opennbt.conversion.builtin;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.IntArrayTag;

public class IntArrayTagConverter
  implements TagConverter<IntArrayTag, int[]>
{
  public int[] convert(IntArrayTag tag)
  {
    return tag.getValue();
  }
  
  public IntArrayTag convert(String name, int[] value)
  {
    return new IntArrayTag(name, value);
  }
}
