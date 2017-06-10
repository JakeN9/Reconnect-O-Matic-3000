package io.netty.handler.codec;

public class EmptyTextHeaders
  extends EmptyConvertibleHeaders<CharSequence, String>
  implements TextHeaders
{
  public boolean contains(CharSequence name, CharSequence value, boolean ignoreCase)
  {
    return false;
  }
  
  public boolean containsObject(CharSequence name, Object value, boolean ignoreCase)
  {
    return false;
  }
  
  public TextHeaders add(CharSequence name, CharSequence value)
  {
    super.add(name, value);
    return this;
  }
  
  public TextHeaders add(CharSequence name, Iterable<? extends CharSequence> values)
  {
    super.add(name, values);
    return this;
  }
  
  public TextHeaders add(CharSequence name, CharSequence... values)
  {
    super.add(name, values);
    return this;
  }
  
  public TextHeaders addObject(CharSequence name, Object value)
  {
    super.addObject(name, value);
    return this;
  }
  
  public TextHeaders addObject(CharSequence name, Iterable<?> values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public TextHeaders addObject(CharSequence name, Object... values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public TextHeaders addBoolean(CharSequence name, boolean value)
  {
    super.addBoolean(name, value);
    return this;
  }
  
  public TextHeaders addChar(CharSequence name, char value)
  {
    super.addChar(name, value);
    return this;
  }
  
  public TextHeaders addByte(CharSequence name, byte value)
  {
    super.addByte(name, value);
    return this;
  }
  
  public TextHeaders addShort(CharSequence name, short value)
  {
    super.addShort(name, value);
    return this;
  }
  
  public TextHeaders addInt(CharSequence name, int value)
  {
    super.addInt(name, value);
    return this;
  }
  
  public TextHeaders addLong(CharSequence name, long value)
  {
    super.addLong(name, value);
    return this;
  }
  
  public TextHeaders addFloat(CharSequence name, float value)
  {
    super.addFloat(name, value);
    return this;
  }
  
  public TextHeaders addDouble(CharSequence name, double value)
  {
    super.addDouble(name, value);
    return this;
  }
  
  public TextHeaders addTimeMillis(CharSequence name, long value)
  {
    super.addTimeMillis(name, value);
    return this;
  }
  
  public TextHeaders add(TextHeaders headers)
  {
    super.add(headers);
    return this;
  }
  
  public TextHeaders set(CharSequence name, CharSequence value)
  {
    super.set(name, value);
    return this;
  }
  
  public TextHeaders set(CharSequence name, Iterable<? extends CharSequence> values)
  {
    super.set(name, values);
    return this;
  }
  
  public TextHeaders set(CharSequence name, CharSequence... values)
  {
    super.set(name, values);
    return this;
  }
  
  public TextHeaders setObject(CharSequence name, Object value)
  {
    super.setObject(name, value);
    return this;
  }
  
  public TextHeaders setObject(CharSequence name, Iterable<?> values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public TextHeaders setObject(CharSequence name, Object... values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public TextHeaders setBoolean(CharSequence name, boolean value)
  {
    super.setBoolean(name, value);
    return this;
  }
  
  public TextHeaders setChar(CharSequence name, char value)
  {
    super.setChar(name, value);
    return this;
  }
  
  public TextHeaders setByte(CharSequence name, byte value)
  {
    super.setByte(name, value);
    return this;
  }
  
  public TextHeaders setShort(CharSequence name, short value)
  {
    super.setShort(name, value);
    return this;
  }
  
  public TextHeaders setInt(CharSequence name, int value)
  {
    super.setInt(name, value);
    return this;
  }
  
  public TextHeaders setLong(CharSequence name, long value)
  {
    super.setLong(name, value);
    return this;
  }
  
  public TextHeaders setFloat(CharSequence name, float value)
  {
    super.setFloat(name, value);
    return this;
  }
  
  public TextHeaders setDouble(CharSequence name, double value)
  {
    super.setDouble(name, value);
    return this;
  }
  
  public TextHeaders setTimeMillis(CharSequence name, long value)
  {
    super.setTimeMillis(name, value);
    return this;
  }
  
  public TextHeaders set(TextHeaders headers)
  {
    super.set(headers);
    return this;
  }
  
  public TextHeaders setAll(TextHeaders headers)
  {
    super.setAll(headers);
    return this;
  }
  
  public TextHeaders clear()
  {
    super.clear();
    return this;
  }
}
