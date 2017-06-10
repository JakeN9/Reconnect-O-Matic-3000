package io.netty.handler.codec;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class EmptyConvertibleHeaders<UnconvertedType, ConvertedType>
  extends EmptyHeaders<UnconvertedType>
  implements ConvertibleHeaders<UnconvertedType, ConvertedType>
{
  public ConvertedType getAndConvert(UnconvertedType name)
  {
    return null;
  }
  
  public ConvertedType getAndConvert(UnconvertedType name, ConvertedType defaultValue)
  {
    return defaultValue;
  }
  
  public ConvertedType getAndRemoveAndConvert(UnconvertedType name)
  {
    return null;
  }
  
  public ConvertedType getAndRemoveAndConvert(UnconvertedType name, ConvertedType defaultValue)
  {
    return defaultValue;
  }
  
  public List<ConvertedType> getAllAndConvert(UnconvertedType name)
  {
    return Collections.emptyList();
  }
  
  public List<ConvertedType> getAllAndRemoveAndConvert(UnconvertedType name)
  {
    return Collections.emptyList();
  }
  
  public List<Map.Entry<ConvertedType, ConvertedType>> entriesConverted()
  {
    return Collections.emptyList();
  }
  
  public Iterator<Map.Entry<ConvertedType, ConvertedType>> iteratorConverted()
  {
    return entriesConverted().iterator();
  }
  
  public Set<ConvertedType> namesAndConvert(Comparator<ConvertedType> comparator)
  {
    return Collections.emptySet();
  }
}
