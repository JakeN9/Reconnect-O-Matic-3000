package org.spacehq.opennbt.conversion.builtin;

import java.util.ArrayList;
import java.util.List;
import org.spacehq.opennbt.conversion.ConverterRegistry;
import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.ListTag;
import org.spacehq.opennbt.tag.builtin.Tag;

public class ListTagConverter
  implements TagConverter<ListTag, List>
{
  public List convert(ListTag tag)
  {
    List<Object> ret = new ArrayList();
    List<? extends Tag> tags = tag.getValue();
    for (Tag t : tags) {
      ret.add(ConverterRegistry.convertToValue(t));
    }
    return ret;
  }
  
  public ListTag convert(String name, List value)
  {
    if (value.isEmpty()) {
      throw new IllegalArgumentException("Cannot convert ListTag with size of 0.");
    }
    List<Tag> tags = new ArrayList();
    for (Object o : value) {
      tags.add(ConverterRegistry.convertToTag("", o));
    }
    return new ListTag(name, tags);
  }
}
