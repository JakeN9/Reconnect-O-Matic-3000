package org.spacehq.opennbt.conversion.builtin;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.ShortTag;

public class ShortTagConverter
  implements TagConverter<ShortTag, Short>
{
  public Short convert(ShortTag tag)
  {
    return tag.getValue();
  }
  
  public ShortTag convert(String name, Short value)
  {
    return new ShortTag(name, value.shortValue());
  }
}
