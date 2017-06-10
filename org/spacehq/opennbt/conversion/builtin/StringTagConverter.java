package org.spacehq.opennbt.conversion.builtin;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.StringTag;

public class StringTagConverter
  implements TagConverter<StringTag, String>
{
  public String convert(StringTag tag)
  {
    return tag.getValue();
  }
  
  public StringTag convert(String name, String value)
  {
    return new StringTag(name, value);
  }
}
