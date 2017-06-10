package org.spacehq.opennbt.conversion.builtin;

import java.util.HashMap;
import java.util.Map;
import org.spacehq.opennbt.conversion.ConverterRegistry;
import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.CompoundTag;
import org.spacehq.opennbt.tag.builtin.Tag;

public class CompoundTagConverter
  implements TagConverter<CompoundTag, Map>
{
  public Map convert(CompoundTag tag)
  {
    Map<String, Object> ret = new HashMap();
    Map<String, Tag> tags = tag.getValue();
    for (String name : tags.keySet())
    {
      Tag t = (Tag)tags.get(name);
      ret.put(t.getName(), ConverterRegistry.convertToValue(t));
    }
    return ret;
  }
  
  public CompoundTag convert(String name, Map value)
  {
    Map<String, Tag> tags = new HashMap();
    for (Object na : value.keySet())
    {
      String n = (String)na;
      tags.put(n, ConverterRegistry.convertToTag(n, value.get(n)));
    }
    return new CompoundTag(name, tags);
  }
}
