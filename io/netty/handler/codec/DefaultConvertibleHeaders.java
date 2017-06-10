package io.netty.handler.codec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class DefaultConvertibleHeaders<UnconvertedType, ConvertedType>
  extends DefaultHeaders<UnconvertedType>
  implements ConvertibleHeaders<UnconvertedType, ConvertedType>
{
  private final ConvertibleHeaders.TypeConverter<UnconvertedType, ConvertedType> typeConverter;
  
  public DefaultConvertibleHeaders(Comparator<? super UnconvertedType> keyComparator, Comparator<? super UnconvertedType> valueComparator, DefaultHeaders.HashCodeGenerator<UnconvertedType> hashCodeGenerator, Headers.ValueConverter<UnconvertedType> valueConverter, ConvertibleHeaders.TypeConverter<UnconvertedType, ConvertedType> typeConverter)
  {
    super(keyComparator, valueComparator, hashCodeGenerator, valueConverter);
    this.typeConverter = typeConverter;
  }
  
  public DefaultConvertibleHeaders(Comparator<? super UnconvertedType> keyComparator, Comparator<? super UnconvertedType> valueComparator, DefaultHeaders.HashCodeGenerator<UnconvertedType> hashCodeGenerator, Headers.ValueConverter<UnconvertedType> valueConverter, ConvertibleHeaders.TypeConverter<UnconvertedType, ConvertedType> typeConverter, DefaultHeaders.NameConverter<UnconvertedType> nameConverter)
  {
    super(keyComparator, valueComparator, hashCodeGenerator, valueConverter, nameConverter);
    this.typeConverter = typeConverter;
  }
  
  public ConvertedType getAndConvert(UnconvertedType name)
  {
    return (ConvertedType)getAndConvert(name, null);
  }
  
  public ConvertedType getAndConvert(UnconvertedType name, ConvertedType defaultValue)
  {
    UnconvertedType v = get(name);
    if (v == null) {
      return defaultValue;
    }
    return (ConvertedType)this.typeConverter.toConvertedType(v);
  }
  
  public ConvertedType getAndRemoveAndConvert(UnconvertedType name)
  {
    return (ConvertedType)getAndRemoveAndConvert(name, null);
  }
  
  public ConvertedType getAndRemoveAndConvert(UnconvertedType name, ConvertedType defaultValue)
  {
    UnconvertedType v = getAndRemove(name);
    if (v == null) {
      return defaultValue;
    }
    return (ConvertedType)this.typeConverter.toConvertedType(v);
  }
  
  public List<ConvertedType> getAllAndConvert(UnconvertedType name)
  {
    List<UnconvertedType> all = getAll(name);
    List<ConvertedType> allConverted = new ArrayList(all.size());
    for (int i = 0; i < all.size(); i++) {
      allConverted.add(this.typeConverter.toConvertedType(all.get(i)));
    }
    return allConverted;
  }
  
  public List<ConvertedType> getAllAndRemoveAndConvert(UnconvertedType name)
  {
    List<UnconvertedType> all = getAllAndRemove(name);
    List<ConvertedType> allConverted = new ArrayList(all.size());
    for (int i = 0; i < all.size(); i++) {
      allConverted.add(this.typeConverter.toConvertedType(all.get(i)));
    }
    return allConverted;
  }
  
  public List<Map.Entry<ConvertedType, ConvertedType>> entriesConverted()
  {
    List<Map.Entry<UnconvertedType, UnconvertedType>> entries = entries();
    List<Map.Entry<ConvertedType, ConvertedType>> entriesConverted = new ArrayList(entries.size());
    for (int i = 0; i < entries.size(); i++) {
      entriesConverted.add(new ConvertedEntry((Map.Entry)entries.get(i)));
    }
    return entriesConverted;
  }
  
  public Iterator<Map.Entry<ConvertedType, ConvertedType>> iteratorConverted()
  {
    return new ConvertedIterator(null);
  }
  
  public Set<ConvertedType> namesAndConvert(Comparator<ConvertedType> comparator)
  {
    Set<UnconvertedType> names = names();
    Set<ConvertedType> namesConverted = new TreeSet(comparator);
    for (UnconvertedType unconverted : names) {
      namesConverted.add(this.typeConverter.toConvertedType(unconverted));
    }
    return namesConverted;
  }
  
  private final class ConvertedIterator
    implements Iterator<Map.Entry<ConvertedType, ConvertedType>>
  {
    private final Iterator<Map.Entry<UnconvertedType, UnconvertedType>> iter = DefaultConvertibleHeaders.this.iterator();
    
    private ConvertedIterator() {}
    
    public boolean hasNext()
    {
      return this.iter.hasNext();
    }
    
    public Map.Entry<ConvertedType, ConvertedType> next()
    {
      Map.Entry<UnconvertedType, UnconvertedType> next = (Map.Entry)this.iter.next();
      
      return new DefaultConvertibleHeaders.ConvertedEntry(DefaultConvertibleHeaders.this, next);
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  private final class ConvertedEntry
    implements Map.Entry<ConvertedType, ConvertedType>
  {
    private final Map.Entry<UnconvertedType, UnconvertedType> entry;
    private ConvertedType name;
    private ConvertedType value;
    
    ConvertedEntry()
    {
      this.entry = entry;
    }
    
    public ConvertedType getKey()
    {
      if (this.name == null) {
        this.name = DefaultConvertibleHeaders.this.typeConverter.toConvertedType(this.entry.getKey());
      }
      return (ConvertedType)this.name;
    }
    
    public ConvertedType getValue()
    {
      if (this.value == null) {
        this.value = DefaultConvertibleHeaders.this.typeConverter.toConvertedType(this.entry.getValue());
      }
      return (ConvertedType)this.value;
    }
    
    public ConvertedType setValue(ConvertedType value)
    {
      ConvertedType old = getValue();
      this.entry.setValue(DefaultConvertibleHeaders.this.typeConverter.toUnconvertedType(value));
      return old;
    }
    
    public String toString()
    {
      return this.entry.toString();
    }
  }
}
