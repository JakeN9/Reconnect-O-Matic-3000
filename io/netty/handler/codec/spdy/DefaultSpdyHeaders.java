package io.netty.handler.codec.spdy;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.DefaultHeaders.NameConverter;
import io.netty.handler.codec.DefaultTextHeaders;
import io.netty.handler.codec.DefaultTextHeaders.DefaultTextValueTypeConverter;
import io.netty.handler.codec.Headers.ValueConverter;
import io.netty.handler.codec.TextHeaders;
import java.util.Locale;

public class DefaultSpdyHeaders
  extends DefaultTextHeaders
  implements SpdyHeaders
{
  private static final Headers.ValueConverter<CharSequence> SPDY_VALUE_CONVERTER = new DefaultTextHeaders.DefaultTextValueTypeConverter()
  {
    public CharSequence convertObject(Object value)
    {
      CharSequence seq;
      CharSequence seq;
      if ((value instanceof CharSequence)) {
        seq = (CharSequence)value;
      } else {
        seq = value.toString();
      }
      SpdyCodecUtil.validateHeaderValue(seq);
      return seq;
    }
  };
  private static final DefaultHeaders.NameConverter<CharSequence> SPDY_NAME_CONVERTER = new DefaultHeaders.NameConverter()
  {
    public CharSequence convertName(CharSequence name)
    {
      if ((name instanceof AsciiString)) {
        name = ((AsciiString)name).toLowerCase();
      } else {
        name = name.toString().toLowerCase(Locale.US);
      }
      SpdyCodecUtil.validateHeaderName(name);
      return name;
    }
  };
  
  public DefaultSpdyHeaders()
  {
    super(true, SPDY_VALUE_CONVERTER, SPDY_NAME_CONVERTER);
  }
  
  public SpdyHeaders add(CharSequence name, CharSequence value)
  {
    super.add(name, value);
    return this;
  }
  
  public SpdyHeaders add(CharSequence name, Iterable<? extends CharSequence> values)
  {
    super.add(name, values);
    return this;
  }
  
  public SpdyHeaders add(CharSequence name, CharSequence... values)
  {
    super.add(name, values);
    return this;
  }
  
  public SpdyHeaders addObject(CharSequence name, Object value)
  {
    super.addObject(name, value);
    return this;
  }
  
  public SpdyHeaders addObject(CharSequence name, Iterable<?> values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public SpdyHeaders addObject(CharSequence name, Object... values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public SpdyHeaders addBoolean(CharSequence name, boolean value)
  {
    super.addBoolean(name, value);
    return this;
  }
  
  public SpdyHeaders addChar(CharSequence name, char value)
  {
    super.addChar(name, value);
    return this;
  }
  
  public SpdyHeaders addByte(CharSequence name, byte value)
  {
    super.addByte(name, value);
    return this;
  }
  
  public SpdyHeaders addShort(CharSequence name, short value)
  {
    super.addShort(name, value);
    return this;
  }
  
  public SpdyHeaders addInt(CharSequence name, int value)
  {
    super.addInt(name, value);
    return this;
  }
  
  public SpdyHeaders addLong(CharSequence name, long value)
  {
    super.addLong(name, value);
    return this;
  }
  
  public SpdyHeaders addFloat(CharSequence name, float value)
  {
    super.addFloat(name, value);
    return this;
  }
  
  public SpdyHeaders addDouble(CharSequence name, double value)
  {
    super.addDouble(name, value);
    return this;
  }
  
  public SpdyHeaders addTimeMillis(CharSequence name, long value)
  {
    super.addTimeMillis(name, value);
    return this;
  }
  
  public SpdyHeaders add(TextHeaders headers)
  {
    super.add(headers);
    return this;
  }
  
  public SpdyHeaders set(CharSequence name, CharSequence value)
  {
    super.set(name, value);
    return this;
  }
  
  public SpdyHeaders set(CharSequence name, Iterable<? extends CharSequence> values)
  {
    super.set(name, values);
    return this;
  }
  
  public SpdyHeaders set(CharSequence name, CharSequence... values)
  {
    super.set(name, values);
    return this;
  }
  
  public SpdyHeaders setObject(CharSequence name, Object value)
  {
    super.setObject(name, value);
    return this;
  }
  
  public SpdyHeaders setObject(CharSequence name, Iterable<?> values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public SpdyHeaders setObject(CharSequence name, Object... values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public SpdyHeaders setBoolean(CharSequence name, boolean value)
  {
    super.setBoolean(name, value);
    return this;
  }
  
  public SpdyHeaders setChar(CharSequence name, char value)
  {
    super.setChar(name, value);
    return this;
  }
  
  public SpdyHeaders setByte(CharSequence name, byte value)
  {
    super.setByte(name, value);
    return this;
  }
  
  public SpdyHeaders setShort(CharSequence name, short value)
  {
    super.setShort(name, value);
    return this;
  }
  
  public SpdyHeaders setInt(CharSequence name, int value)
  {
    super.setInt(name, value);
    return this;
  }
  
  public SpdyHeaders setLong(CharSequence name, long value)
  {
    super.setLong(name, value);
    return this;
  }
  
  public SpdyHeaders setFloat(CharSequence name, float value)
  {
    super.setFloat(name, value);
    return this;
  }
  
  public SpdyHeaders setDouble(CharSequence name, double value)
  {
    super.setDouble(name, value);
    return this;
  }
  
  public SpdyHeaders setTimeMillis(CharSequence name, long value)
  {
    super.setTimeMillis(name, value);
    return this;
  }
  
  public SpdyHeaders set(TextHeaders headers)
  {
    super.set(headers);
    return this;
  }
  
  public SpdyHeaders setAll(TextHeaders headers)
  {
    super.setAll(headers);
    return this;
  }
  
  public SpdyHeaders clear()
  {
    super.clear();
    return this;
  }
}
