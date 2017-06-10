package io.netty.handler.codec.http;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.DefaultHeaders.NameConverter;
import io.netty.handler.codec.DefaultTextHeaders;
import io.netty.handler.codec.DefaultTextHeaders.DefaultTextValueTypeConverter;
import io.netty.handler.codec.TextHeaders;
import java.util.Calendar;
import java.util.Date;

public class DefaultHttpHeaders
  extends DefaultTextHeaders
  implements HttpHeaders
{
  private static final int HIGHEST_INVALID_NAME_CHAR_MASK = -64;
  private static final int HIGHEST_INVALID_VALUE_CHAR_MASK = -16;
  private static final byte[] LOOKUP_TABLE = new byte[64];
  
  static
  {
    LOOKUP_TABLE[9] = -1;
    LOOKUP_TABLE[10] = -1;
    LOOKUP_TABLE[11] = -1;
    LOOKUP_TABLE[12] = -1;
    LOOKUP_TABLE[32] = -1;
    LOOKUP_TABLE[44] = -1;
    LOOKUP_TABLE[58] = -1;
    LOOKUP_TABLE[59] = -1;
    LOOKUP_TABLE[61] = -1;
  }
  
  private static final class HttpHeadersValidationConverter
    extends DefaultTextHeaders.DefaultTextValueTypeConverter
  {
    private final boolean validate;
    
    HttpHeadersValidationConverter(boolean validate)
    {
      this.validate = validate;
    }
    
    public CharSequence convertObject(Object value)
    {
      if (value == null) {
        throw new NullPointerException("value");
      }
      CharSequence seq;
      CharSequence seq;
      if ((value instanceof CharSequence))
      {
        seq = (CharSequence)value;
      }
      else
      {
        CharSequence seq;
        if ((value instanceof Number))
        {
          seq = value.toString();
        }
        else
        {
          CharSequence seq;
          if ((value instanceof Date))
          {
            seq = HttpHeaderDateFormat.get().format((Date)value);
          }
          else
          {
            CharSequence seq;
            if ((value instanceof Calendar)) {
              seq = HttpHeaderDateFormat.get().format(((Calendar)value).getTime());
            } else {
              seq = value.toString();
            }
          }
        }
      }
      if (this.validate) {
        if ((value instanceof AsciiString)) {
          validateValue((AsciiString)seq);
        } else {
          validateValue(seq);
        }
      }
      return seq;
    }
    
    private static void validateValue(AsciiString seq)
    {
      int state = 0;
      
      int start = seq.arrayOffset();
      int end = start + seq.length();
      byte[] array = seq.array();
      for (int index = start; index < end; index++) {
        state = validateValueChar(seq, state, (char)(array[index] & 0xFF));
      }
      if (state != 0) {
        throw new IllegalArgumentException("a header value must not end with '\\r' or '\\n':" + seq);
      }
    }
    
    private static void validateValue(CharSequence seq)
    {
      int state = 0;
      for (int index = 0; index < seq.length(); index++) {
        state = validateValueChar(seq, state, seq.charAt(index));
      }
      if (state != 0) {
        throw new IllegalArgumentException("a header value must not end with '\\r' or '\\n':" + seq);
      }
    }
    
    private static int validateValueChar(CharSequence seq, int state, char character)
    {
      if ((character & 0xFFFFFFF0) == 0) {
        switch (character)
        {
        case '\000': 
          throw new IllegalArgumentException("a header value contains a prohibited character '\000': " + seq);
        case '\013': 
          throw new IllegalArgumentException("a header value contains a prohibited character '\\v': " + seq);
        case '\f': 
          throw new IllegalArgumentException("a header value contains a prohibited character '\\f': " + seq);
        }
      }
      switch (state)
      {
      case 0: 
        switch (character)
        {
        case '\r': 
          state = 1;
          break;
        case '\n': 
          state = 2;
        }
        break;
      case 1: 
        switch (character)
        {
        case '\n': 
          state = 2;
          break;
        default: 
          throw new IllegalArgumentException("only '\\n' is allowed after '\\r': " + seq);
        }
        break;
      case 2: 
        switch (character)
        {
        case '\t': 
        case ' ': 
          state = 0;
          break;
        default: 
          throw new IllegalArgumentException("only ' ' and '\\t' are allowed after '\\n': " + seq);
        }
        break;
      }
      return state;
    }
  }
  
  static class HttpHeadersNameConverter
    implements DefaultHeaders.NameConverter<CharSequence>
  {
    protected final boolean validate;
    
    HttpHeadersNameConverter(boolean validate)
    {
      this.validate = validate;
    }
    
    public CharSequence convertName(CharSequence name)
    {
      if (this.validate) {
        if ((name instanceof AsciiString)) {
          validateName((AsciiString)name);
        } else {
          validateName(name);
        }
      }
      return name;
    }
    
    private static void validateName(AsciiString name)
    {
      int start = name.arrayOffset();
      int end = start + name.length();
      byte[] array = name.array();
      for (int index = start; index < end; index++)
      {
        byte b = array[index];
        if (b < 0) {
          throw new IllegalArgumentException("a header name cannot contain non-ASCII characters: " + name);
        }
        validateNameChar(name, b);
      }
    }
    
    private static void validateName(CharSequence name)
    {
      for (int index = 0; index < name.length(); index++)
      {
        char character = name.charAt(index);
        if (character > '') {
          throw new IllegalArgumentException("a header name cannot contain non-ASCII characters: " + name);
        }
        validateNameChar(name, character);
      }
    }
    
    private static void validateNameChar(CharSequence name, int character)
    {
      if (((character & 0xFFFFFFC0) == 0) && (DefaultHttpHeaders.LOOKUP_TABLE[character] != 0)) {
        throw new IllegalArgumentException("a header name cannot contain the following prohibited characters: =,;: \\t\\r\\n\\v\\f: " + name);
      }
    }
  }
  
  private static final HttpHeadersValidationConverter VALIDATE_OBJECT_CONVERTER = new HttpHeadersValidationConverter(true);
  private static final HttpHeadersValidationConverter NO_VALIDATE_OBJECT_CONVERTER = new HttpHeadersValidationConverter(false);
  private static final HttpHeadersNameConverter VALIDATE_NAME_CONVERTER = new HttpHeadersNameConverter(true);
  private static final HttpHeadersNameConverter NO_VALIDATE_NAME_CONVERTER = new HttpHeadersNameConverter(false);
  
  public DefaultHttpHeaders()
  {
    this(true);
  }
  
  public DefaultHttpHeaders(boolean validate)
  {
    this(true, validate ? VALIDATE_NAME_CONVERTER : NO_VALIDATE_NAME_CONVERTER, false);
  }
  
  protected DefaultHttpHeaders(boolean validate, boolean singleHeaderFields)
  {
    this(true, validate ? VALIDATE_NAME_CONVERTER : NO_VALIDATE_NAME_CONVERTER, singleHeaderFields);
  }
  
  protected DefaultHttpHeaders(boolean validate, DefaultHeaders.NameConverter<CharSequence> nameConverter, boolean singleHeaderFields)
  {
    super(true, validate ? VALIDATE_OBJECT_CONVERTER : NO_VALIDATE_OBJECT_CONVERTER, nameConverter, singleHeaderFields);
  }
  
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
