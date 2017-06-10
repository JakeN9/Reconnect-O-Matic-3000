package org.spacehq.opennbt.conversion.builtin.custom;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.custom.StringArrayTag;

public class StringArrayTagConverter
  implements TagConverter<StringArrayTag, String[]>
{
  public String[] convert(StringArrayTag tag)
  {
    return tag.getValue();
  }
  
  public StringArrayTag convert(String name, String[] value)
  {
    return new StringArrayTag(name, value);
  }
}
