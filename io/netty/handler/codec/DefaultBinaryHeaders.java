package io.netty.handler.codec;

import io.netty.util.internal.PlatformDependent;
import java.text.ParseException;

public class DefaultBinaryHeaders
  extends DefaultHeaders<AsciiString>
  implements BinaryHeaders
{
  private static final DefaultHeaders.HashCodeGenerator<AsciiString> ASCII_HASH_CODE_GENERATOR = new DefaultHeaders.HashCodeGenerator()
  {
    public int generateHashCode(AsciiString name)
    {
      return AsciiString.caseInsensitiveHashCode(name);
    }
  };
  private static final Headers.ValueConverter<AsciiString> OBJECT_TO_ASCII = new Headers.ValueConverter()
  {
    public AsciiString convertObject(Object value)
    {
      if ((value instanceof AsciiString)) {
        return (AsciiString)value;
      }
      if ((value instanceof CharSequence)) {
        return new AsciiString((CharSequence)value);
      }
      return new AsciiString(value.toString());
    }
    
    public AsciiString convertInt(int value)
    {
      return new AsciiString(String.valueOf(value));
    }
    
    public AsciiString convertLong(long value)
    {
      return new AsciiString(String.valueOf(value));
    }
    
    public AsciiString convertDouble(double value)
    {
      return new AsciiString(String.valueOf(value));
    }
    
    public AsciiString convertChar(char value)
    {
      return new AsciiString(String.valueOf(value));
    }
    
    public AsciiString convertBoolean(boolean value)
    {
      return new AsciiString(String.valueOf(value));
    }
    
    public AsciiString convertFloat(float value)
    {
      return new AsciiString(String.valueOf(value));
    }
    
    public int convertToInt(AsciiString value)
    {
      return value.parseInt();
    }
    
    public long convertToLong(AsciiString value)
    {
      return value.parseLong();
    }
    
    public AsciiString convertTimeMillis(long value)
    {
      return new AsciiString(String.valueOf(value));
    }
    
    public long convertToTimeMillis(AsciiString value)
    {
      try
      {
        return DefaultHeaders.HeaderDateFormat.get().parse(value.toString());
      }
      catch (ParseException e)
      {
        PlatformDependent.throwException(e);
      }
      return 0L;
    }
    
    public double convertToDouble(AsciiString value)
    {
      return value.parseDouble();
    }
    
    public char convertToChar(AsciiString value)
    {
      return value.charAt(0);
    }
    
    public boolean convertToBoolean(AsciiString value)
    {
      return value.byteAt(0) != 0;
    }
    
    public float convertToFloat(AsciiString value)
    {
      return value.parseFloat();
    }
    
    public AsciiString convertShort(short value)
    {
      return new AsciiString(String.valueOf(value));
    }
    
    public short convertToShort(AsciiString value)
    {
      return value.parseShort();
    }
    
    public AsciiString convertByte(byte value)
    {
      return new AsciiString(String.valueOf(value));
    }
    
    public byte convertToByte(AsciiString value)
    {
      return value.byteAt(0);
    }
  };
  private static final DefaultHeaders.NameConverter<AsciiString> ASCII_TO_LOWER_CONVERTER = new DefaultHeaders.NameConverter()
  {
    public AsciiString convertName(AsciiString name)
    {
      return name.toLowerCase();
    }
  };
  private static final DefaultHeaders.NameConverter<AsciiString> ASCII_IDENTITY_CONVERTER = new DefaultHeaders.NameConverter()
  {
    public AsciiString convertName(AsciiString name)
    {
      return name;
    }
  };
  
  public DefaultBinaryHeaders()
  {
    this(false);
  }
  
  public DefaultBinaryHeaders(boolean forceKeyToLower)
  {
    super(AsciiString.CASE_INSENSITIVE_ORDER, AsciiString.CASE_INSENSITIVE_ORDER, ASCII_HASH_CODE_GENERATOR, OBJECT_TO_ASCII, forceKeyToLower ? ASCII_TO_LOWER_CONVERTER : ASCII_IDENTITY_CONVERTER);
  }
  
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
