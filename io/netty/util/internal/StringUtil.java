package io.netty.util.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

public final class StringUtil
{
  public static final String NEWLINE;
  public static final char DOUBLE_QUOTE = '"';
  public static final char COMMA = ',';
  public static final char LINE_FEED = '\n';
  public static final char CARRIAGE_RETURN = '\r';
  private static final String[] BYTE2HEX_PAD;
  private static final String[] BYTE2HEX_NOPAD;
  private static final String EMPTY_STRING = "";
  private static final int CSV_NUMBER_ESCAPE_CHARACTERS = 7;
  
  static
  {
    BYTE2HEX_PAD = new String['Ā'];
    BYTE2HEX_NOPAD = new String['Ā'];
    String newLine;
    try
    {
      newLine = new Formatter().format("%n", new Object[0]).toString();
    }
    catch (Exception e)
    {
      newLine = "\n";
    }
    NEWLINE = newLine;
    for (int i = 0; i < 10; i++)
    {
      StringBuilder buf = new StringBuilder(2);
      buf.append('0');
      buf.append(i);
      BYTE2HEX_PAD[i] = buf.toString();
      BYTE2HEX_NOPAD[i] = String.valueOf(i);
    }
    for (; i < 16; i++)
    {
      StringBuilder buf = new StringBuilder(2);
      char c = (char)(97 + i - 10);
      buf.append('0');
      buf.append(c);
      BYTE2HEX_PAD[i] = buf.toString();
      BYTE2HEX_NOPAD[i] = String.valueOf(c);
    }
    for (; i < BYTE2HEX_PAD.length; i++)
    {
      StringBuilder buf = new StringBuilder(2);
      buf.append(Integer.toHexString(i));
      String str = buf.toString();
      BYTE2HEX_PAD[i] = str;
      BYTE2HEX_NOPAD[i] = str;
    }
  }
  
  public static String[] split(String value, char delim)
  {
    int end = value.length();
    List<String> res = new ArrayList();
    
    int start = 0;
    for (int i = 0; i < end; i++) {
      if (value.charAt(i) == delim)
      {
        if (start == i) {
          res.add("");
        } else {
          res.add(value.substring(start, i));
        }
        start = i + 1;
      }
    }
    if (start == 0) {
      res.add(value);
    } else if (start != end) {
      res.add(value.substring(start, end));
    } else {
      for (int i = res.size() - 1; i >= 0; i--)
      {
        if (!((String)res.get(i)).isEmpty()) {
          break;
        }
        res.remove(i);
      }
    }
    return (String[])res.toArray(new String[res.size()]);
  }
  
  public static String[] split(String value, char delim, int maxParts)
  {
    int end = value.length();
    List<String> res = new ArrayList();
    
    int start = 0;
    int cpt = 1;
    for (int i = 0; (i < end) && (cpt < maxParts); i++) {
      if (value.charAt(i) == delim)
      {
        if (start == i) {
          res.add("");
        } else {
          res.add(value.substring(start, i));
        }
        start = i + 1;
        cpt++;
      }
    }
    if (start == 0) {
      res.add(value);
    } else if (start != end) {
      res.add(value.substring(start, end));
    } else {
      for (int i = res.size() - 1; i >= 0; i--)
      {
        if (!((String)res.get(i)).isEmpty()) {
          break;
        }
        res.remove(i);
      }
    }
    return (String[])res.toArray(new String[res.size()]);
  }
  
  public static String substringAfter(String value, char delim)
  {
    int pos = value.indexOf(delim);
    if (pos >= 0) {
      return value.substring(pos + 1);
    }
    return null;
  }
  
  public static String byteToHexStringPadded(int value)
  {
    return BYTE2HEX_PAD[(value & 0xFF)];
  }
  
  public static <T extends Appendable> T byteToHexStringPadded(T buf, int value)
  {
    try
    {
      buf.append(byteToHexStringPadded(value));
    }
    catch (IOException e)
    {
      PlatformDependent.throwException(e);
    }
    return buf;
  }
  
  public static String toHexStringPadded(byte[] src)
  {
    return toHexStringPadded(src, 0, src.length);
  }
  
  public static String toHexStringPadded(byte[] src, int offset, int length)
  {
    return ((StringBuilder)toHexStringPadded(new StringBuilder(length << 1), src, offset, length)).toString();
  }
  
  public static <T extends Appendable> T toHexStringPadded(T dst, byte[] src)
  {
    return toHexStringPadded(dst, src, 0, src.length);
  }
  
  public static <T extends Appendable> T toHexStringPadded(T dst, byte[] src, int offset, int length)
  {
    int end = offset + length;
    for (int i = offset; i < end; i++) {
      byteToHexStringPadded(dst, src[i]);
    }
    return dst;
  }
  
  public static String byteToHexString(int value)
  {
    return BYTE2HEX_NOPAD[(value & 0xFF)];
  }
  
  public static <T extends Appendable> T byteToHexString(T buf, int value)
  {
    try
    {
      buf.append(byteToHexString(value));
    }
    catch (IOException e)
    {
      PlatformDependent.throwException(e);
    }
    return buf;
  }
  
  public static String toHexString(byte[] src)
  {
    return toHexString(src, 0, src.length);
  }
  
  public static String toHexString(byte[] src, int offset, int length)
  {
    return ((StringBuilder)toHexString(new StringBuilder(length << 1), src, offset, length)).toString();
  }
  
  public static <T extends Appendable> T toHexString(T dst, byte[] src)
  {
    return toHexString(dst, src, 0, src.length);
  }
  
  public static <T extends Appendable> T toHexString(T dst, byte[] src, int offset, int length)
  {
    assert (length >= 0);
    if (length == 0) {
      return dst;
    }
    int end = offset + length;
    int endMinusOne = end - 1;
    for (int i = offset; i < endMinusOne; i++) {
      if (src[i] != 0) {
        break;
      }
    }
    byteToHexString(dst, src[(i++)]);
    int remaining = end - i;
    toHexStringPadded(dst, src, i, remaining);
    
    return dst;
  }
  
  public static String simpleClassName(Object o)
  {
    if (o == null) {
      return "null_object";
    }
    return simpleClassName(o.getClass());
  }
  
  public static String simpleClassName(Class<?> clazz)
  {
    if (clazz == null) {
      return "null_class";
    }
    Package pkg = clazz.getPackage();
    if (pkg != null) {
      return clazz.getName().substring(pkg.getName().length() + 1);
    }
    return clazz.getName();
  }
  
  public static CharSequence escapeCsv(CharSequence value)
  {
    int length = ((CharSequence)ObjectUtil.checkNotNull(value, "value")).length();
    if (length == 0) {
      return value;
    }
    int last = length - 1;
    boolean quoted = (isDoubleQuote(value.charAt(0))) && (isDoubleQuote(value.charAt(last))) && (length != 1);
    boolean foundSpecialCharacter = false;
    boolean escapedDoubleQuote = false;
    StringBuilder escaped = new StringBuilder(length + 7).append('"');
    for (int i = 0; i < length; i++)
    {
      char current = value.charAt(i);
      switch (current)
      {
      case '"': 
        if ((i == 0) || (i == last))
        {
          if (quoted) {
            continue;
          }
          escaped.append('"');
        }
        else
        {
          boolean isNextCharDoubleQuote = isDoubleQuote(value.charAt(i + 1));
          if ((isDoubleQuote(value.charAt(i - 1))) || ((isNextCharDoubleQuote) && ((!isNextCharDoubleQuote) || (i + 1 != last)))) {
            break label240;
          }
          escaped.append('"');
          escapedDoubleQuote = true;
        }
        break;
      case '\n': 
      case '\r': 
      case ',': 
        foundSpecialCharacter = true;
      }
      label240:
      escaped.append(current);
    }
    return (escapedDoubleQuote) || ((foundSpecialCharacter) && (!quoted)) ? escaped.append('"') : value;
  }
  
  private static boolean isDoubleQuote(char c)
  {
    return c == '"';
  }
}
