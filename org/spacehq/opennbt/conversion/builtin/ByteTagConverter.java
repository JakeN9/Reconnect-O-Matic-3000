package org.spacehq.opennbt.conversion.builtin;

import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.ByteTag;

public class ByteTagConverter
  implements TagConverter<ByteTag, Byte>
{
  public Byte convert(ByteTag tag)
  {
    return tag.getValue();
  }
  
  public ByteTag convert(String name, Byte value)
  {
    return new ByteTag(name, value.byteValue());
  }
}
