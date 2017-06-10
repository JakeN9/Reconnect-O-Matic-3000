package org.spacehq.opennbt.conversion;

import org.spacehq.opennbt.tag.builtin.Tag;

public abstract interface TagConverter<T extends Tag, V>
{
  public abstract V convert(T paramT);
  
  public abstract T convert(String paramString, V paramV);
}
