package io.netty.handler.codec.stomp;

import io.netty.handler.codec.DefaultTextHeaders;
import io.netty.handler.codec.TextHeaders;

public class DefaultStompHeaders
  extends DefaultTextHeaders
  implements StompHeaders
{
  public StompHeaders add(CharSequence name, CharSequence value)
  {
    super.add(name, value);
    return this;
  }
  
  public StompHeaders add(CharSequence name, Iterable<? extends CharSequence> values)
  {
    super.add(name, values);
    return this;
  }
  
  public StompHeaders add(CharSequence name, CharSequence... values)
  {
    super.add(name, values);
    return this;
  }
  
  public StompHeaders addObject(CharSequence name, Object value)
  {
    super.addObject(name, value);
    return this;
  }
  
  public StompHeaders addObject(CharSequence name, Iterable<?> values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public StompHeaders addObject(CharSequence name, Object... values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public StompHeaders addBoolean(CharSequence name, boolean value)
  {
    super.addBoolean(name, value);
    return this;
  }
  
  public StompHeaders addChar(CharSequence name, char value)
  {
    super.addChar(name, value);
    return this;
  }
  
  public StompHeaders addByte(CharSequence name, byte value)
  {
    super.addByte(name, value);
    return this;
  }
  
  public StompHeaders addShort(CharSequence name, short value)
  {
    super.addShort(name, value);
    return this;
  }
  
  public StompHeaders addInt(CharSequence name, int value)
  {
    super.addInt(name, value);
    return this;
  }
  
  public StompHeaders addLong(CharSequence name, long value)
  {
    super.addLong(name, value);
    return this;
  }
  
  public StompHeaders addFloat(CharSequence name, float value)
  {
    super.addFloat(name, value);
    return this;
  }
  
  public StompHeaders addDouble(CharSequence name, double value)
  {
    super.addDouble(name, value);
    return this;
  }
  
  public StompHeaders addTimeMillis(CharSequence name, long value)
  {
    super.addTimeMillis(name, value);
    return this;
  }
  
  public StompHeaders add(TextHeaders headers)
  {
    super.add(headers);
    return this;
  }
  
  public StompHeaders set(CharSequence name, CharSequence value)
  {
    super.set(name, value);
    return this;
  }
  
  public StompHeaders set(CharSequence name, Iterable<? extends CharSequence> values)
  {
    super.set(name, values);
    return this;
  }
  
  public StompHeaders set(CharSequence name, CharSequence... values)
  {
    super.set(name, values);
    return this;
  }
  
  public StompHeaders setObject(CharSequence name, Object value)
  {
    super.setObject(name, value);
    return this;
  }
  
  public StompHeaders setObject(CharSequence name, Iterable<?> values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public StompHeaders setObject(CharSequence name, Object... values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public StompHeaders setBoolean(CharSequence name, boolean value)
  {
    super.setBoolean(name, value);
    return this;
  }
  
  public StompHeaders setChar(CharSequence name, char value)
  {
    super.setChar(name, value);
    return this;
  }
  
  public StompHeaders setByte(CharSequence name, byte value)
  {
    super.setByte(name, value);
    return this;
  }
  
  public StompHeaders setShort(CharSequence name, short value)
  {
    super.setShort(name, value);
    return this;
  }
  
  public StompHeaders setInt(CharSequence name, int value)
  {
    super.setInt(name, value);
    return this;
  }
  
  public StompHeaders setLong(CharSequence name, long value)
  {
    super.setLong(name, value);
    return this;
  }
  
  public StompHeaders setFloat(CharSequence name, float value)
  {
    super.setFloat(name, value);
    return this;
  }
  
  public StompHeaders setDouble(CharSequence name, double value)
  {
    super.setDouble(name, value);
    return this;
  }
  
  public StompHeaders setTimeMillis(CharSequence name, long value)
  {
    super.setTimeMillis(name, value);
    return this;
  }
  
  public StompHeaders set(TextHeaders headers)
  {
    super.set(headers);
    return this;
  }
  
  public StompHeaders setAll(TextHeaders headers)
  {
    super.setAll(headers);
    return this;
  }
  
  public StompHeaders clear()
  {
    super.clear();
    return this;
  }
}
