package io.netty.handler.codec.http;

import io.netty.handler.codec.EmptyTextHeaders;
import io.netty.handler.codec.TextHeaders;

public class EmptyHttpHeaders
  extends EmptyTextHeaders
  implements HttpHeaders
{
  public static final EmptyHttpHeaders INSTANCE = new EmptyHttpHeaders();
  
  public HttpHeaders add(CharSequence name, CharSequence value)
  {
    super.add(name, value);
    return this;
  }
  
  public HttpHeaders add(CharSequence name, Iterable<? extends CharSequence> values)
  {
    super.add(name, values);
    return this;
  }
  
  public HttpHeaders add(CharSequence name, CharSequence... values)
  {
    super.add(name, values);
    return this;
  }
  
  public HttpHeaders addObject(CharSequence name, Object value)
  {
    super.addObject(name, value);
    return this;
  }
  
  public HttpHeaders addObject(CharSequence name, Iterable<?> values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public HttpHeaders addObject(CharSequence name, Object... values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public HttpHeaders addBoolean(CharSequence name, boolean value)
  {
    super.addBoolean(name, value);
    return this;
  }
  
  public HttpHeaders addChar(CharSequence name, char value)
  {
    super.addChar(name, value);
    return this;
  }
  
  public HttpHeaders addByte(CharSequence name, byte value)
  {
    super.addByte(name, value);
    return this;
  }
  
  public HttpHeaders addShort(CharSequence name, short value)
  {
    super.addShort(name, value);
    return this;
  }
  
  public HttpHeaders addInt(CharSequence name, int value)
  {
    super.addInt(name, value);
    return this;
  }
  
  public HttpHeaders addLong(CharSequence name, long value)
  {
    super.addLong(name, value);
    return this;
  }
  
  public HttpHeaders addFloat(CharSequence name, float value)
  {
    super.addFloat(name, value);
    return this;
  }
  
  public HttpHeaders addDouble(CharSequence name, double value)
  {
    super.addDouble(name, value);
    return this;
  }
  
  public HttpHeaders addTimeMillis(CharSequence name, long value)
  {
    super.addTimeMillis(name, value);
    return this;
  }
  
  public HttpHeaders add(TextHeaders headers)
  {
    super.add(headers);
    return this;
  }
  
  public HttpHeaders set(CharSequence name, CharSequence value)
  {
    super.set(name, value);
    return this;
  }
  
  public HttpHeaders set(CharSequence name, Iterable<? extends CharSequence> values)
  {
    super.set(name, values);
    return this;
  }
  
  public HttpHeaders set(CharSequence name, CharSequence... values)
  {
    super.set(name, values);
    return this;
  }
  
  public HttpHeaders setObject(CharSequence name, Object value)
  {
    super.setObject(name, value);
    return this;
  }
  
  public HttpHeaders setObject(CharSequence name, Iterable<?> values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public HttpHeaders setObject(CharSequence name, Object... values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public HttpHeaders setBoolean(CharSequence name, boolean value)
  {
    super.setBoolean(name, value);
    return this;
  }
  
  public HttpHeaders setChar(CharSequence name, char value)
  {
    super.setChar(name, value);
    return this;
  }
  
  public HttpHeaders setByte(CharSequence name, byte value)
  {
    super.setByte(name, value);
    return this;
  }
  
  public HttpHeaders setShort(CharSequence name, short value)
  {
    super.setShort(name, value);
    return this;
  }
  
  public HttpHeaders setInt(CharSequence name, int value)
  {
    super.setInt(name, value);
    return this;
  }
  
  public HttpHeaders setLong(CharSequence name, long value)
  {
    super.setLong(name, value);
    return this;
  }
  
  public HttpHeaders setFloat(CharSequence name, float value)
  {
    super.setFloat(name, value);
    return this;
  }
  
  public HttpHeaders setDouble(CharSequence name, double value)
  {
    super.setDouble(name, value);
    return this;
  }
  
  public HttpHeaders setTimeMillis(CharSequence name, long value)
  {
    super.setTimeMillis(name, value);
    return this;
  }
  
  public HttpHeaders set(TextHeaders headers)
  {
    super.set(headers);
    return this;
  }
  
  public HttpHeaders setAll(TextHeaders headers)
  {
    super.setAll(headers);
    return this;
  }
  
  public HttpHeaders clear()
  {
    super.clear();
    return this;
  }
}
