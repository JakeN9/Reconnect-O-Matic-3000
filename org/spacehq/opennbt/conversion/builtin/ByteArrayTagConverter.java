package org.spacehq.opennbt.conversion.builtin;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.ByteArrayTag;

public class ByteArrayTagConverter
  implements TagConverter<ByteArrayTag, byte[]>
{
  public byte[] convert(ByteArrayTag tag)
  {
    return tag.getValue();
  }
  
  public ByteArrayTag convert(String name, byte[] value)
  {
    return new ByteArrayTag(name, value);
  }
}
