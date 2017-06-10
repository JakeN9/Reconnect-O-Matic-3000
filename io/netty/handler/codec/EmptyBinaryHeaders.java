package io.netty.handler.codec;

public class EmptyBinaryHeaders
  extends EmptyHeaders<AsciiString>
  implements BinaryHeaders
{
  public BinaryHeaders add(AsciiString name, AsciiString value)
  {
    super.add(name, value);
    return this;
  }
  
  public BinaryHeaders add(AsciiString name, Iterable<? extends AsciiString> values)
  {
    super.add(name, values);
    return this;
  }
  
  public BinaryHeaders add(AsciiString name, AsciiString... values)
  {
    super.add(name, values);
    return this;
  }
  
  public BinaryHeaders addObject(AsciiString name, Object value)
  {
    super.addObject(name, value);
    return this;
  }
  
  public BinaryHeaders addObject(AsciiString name, Iterable<?> values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public BinaryHeaders addObject(AsciiString name, Object... values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public BinaryHeaders addBoolean(AsciiString name, boolean value)
  {
    super.addBoolean(name, value);
    return this;
  }
  
  public BinaryHeaders addChar(AsciiString name, char value)
  {
    super.addChar(name, value);
    return this;
  }
  
  public BinaryHeaders addByte(AsciiString name, byte value)
  {
    super.addByte(name, value);
    return this;
  }
  
  public BinaryHeaders addShort(AsciiString name, short value)
  {
    super.addShort(name, value);
    return this;
  }
  
  public BinaryHeaders addInt(AsciiString name, int value)
  {
    super.addInt(name, value);
    return this;
  }
  
  public BinaryHeaders addLong(AsciiString name, long value)
  {
    super.addLong(name, value);
    return this;
  }
  
  public BinaryHeaders addFloat(AsciiString name, float value)
  {
    super.addFloat(name, value);
    return this;
  }
  
  public BinaryHeaders addDouble(AsciiString name, double value)
  {
    super.addDouble(name, value);
    return this;
  }
  
  public BinaryHeaders addTimeMillis(AsciiString name, long value)
  {
    super.addTimeMillis(name, value);
    return this;
  }
  
  public BinaryHeaders add(BinaryHeaders headers)
  {
    super.add(headers);
    return this;
  }
  
  public BinaryHeaders set(AsciiString name, AsciiString value)
  {
    super.set(name, value);
    return this;
  }
  
  public BinaryHeaders set(AsciiString name, Iterable<? extends AsciiString> values)
  {
    super.set(name, values);
    return this;
  }
  
  public BinaryHeaders set(AsciiString name, AsciiString... values)
  {
    super.set(name, values);
    return this;
  }
  
  public BinaryHeaders setObject(AsciiString name, Object value)
  {
    super.setObject(name, value);
    return this;
  }
  
  public BinaryHeaders setObject(AsciiString name, Iterable<?> values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public BinaryHeaders setObject(AsciiString name, Object... values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public BinaryHeaders setBoolean(AsciiString name, boolean value)
  {
    super.setBoolean(name, value);
    return this;
  }
  
  public BinaryHeaders setChar(AsciiString name, char value)
  {
    super.setChar(name, value);
    return this;
  }
  
  public BinaryHeaders setByte(AsciiString name, byte value)
  {
    super.setByte(name, value);
    return this;
  }
  
  public BinaryHeaders setShort(AsciiString name, short value)
  {
    super.setShort(name, value);
    return this;
  }
  
  public BinaryHeaders setInt(AsciiString name, int value)
  {
    super.setInt(name, value);
    return this;
  }
  
  public BinaryHeaders setLong(AsciiString name, long value)
  {
    super.setLong(name, value);
    return this;
  }
  
  public BinaryHeaders setFloat(AsciiString name, float value)
  {
    super.setFloat(name, value);
    return this;
  }
  
  public BinaryHeaders setDouble(AsciiString name, double value)
  {
    super.setDouble(name, value);
    return this;
  }
  
  public BinaryHeaders setTimeMillis(AsciiString name, long value)
  {
    super.setTimeMillis(name, value);
    return this;
  }
  
  public BinaryHeaders set(BinaryHeaders headers)
  {
    super.set(headers);
    return this;
  }
  
  public BinaryHeaders setAll(BinaryHeaders headers)
  {
    super.setAll(headers);
    return this;
  }
  
  public BinaryHeaders clear()
  {
    super.clear();
    return this;
  }
}
