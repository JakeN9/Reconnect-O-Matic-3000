package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.EmptyArrays;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class AsciiString
  implements CharSequence, Comparable<CharSequence>
{
  public static final AsciiString EMPTY_STRING = new AsciiString("");
  public static final Comparator<AsciiString> CASE_INSENSITIVE_ORDER = new Comparator()
  {
    public int compare(AsciiString o1, AsciiString o2)
    {
      return AsciiString.CHARSEQUENCE_CASE_INSENSITIVE_ORDER.compare(o1, o2);
    }
  };
  public static final Comparator<AsciiString> CASE_SENSITIVE_ORDER = new Comparator()
  {
    public int compare(AsciiString o1, AsciiString o2)
    {
      return AsciiString.CHARSEQUENCE_CASE_SENSITIVE_ORDER.compare(o1, o2);
    }
  };
  public static final Comparator<CharSequence> CHARSEQUENCE_CASE_INSENSITIVE_ORDER = new Comparator()
  {
    public int compare(CharSequence o1, CharSequence o2)
    {
      if (o1 == o2) {
        return 0;
      }
      AsciiString a1 = (o1 instanceof AsciiString) ? (AsciiString)o1 : null;
      AsciiString a2 = (o2 instanceof AsciiString) ? (AsciiString)o2 : null;
      
      int length1 = o1.length();
      int length2 = o2.length();
      int minLength = Math.min(length1, length2);
      if ((a1 != null) && (a2 != null))
      {
        byte[] thisValue = a1.value;
        byte[] thatValue = a2.value;
        for (int i = 0; i < minLength; i++)
        {
          byte v1 = thisValue[i];
          byte v2 = thatValue[i];
          if (v1 != v2)
          {
            int c1 = AsciiString.toLowerCase(v1) & 0xFF;
            int c2 = AsciiString.toLowerCase(v2) & 0xFF;
            int result = c1 - c2;
            if (result != 0) {
              return result;
            }
          }
        }
      }
      else if (a1 != null)
      {
        byte[] thisValue = a1.value;
        for (int i = 0; i < minLength; i++)
        {
          int c1 = AsciiString.toLowerCase(thisValue[i]) & 0xFF;
          int c2 = AsciiString.toLowerCase(o2.charAt(i));
          int result = c1 - c2;
          if (result != 0) {
            return result;
          }
        }
      }
      else if (a2 != null)
      {
        byte[] thatValue = a2.value;
        for (int i = 0; i < minLength; i++)
        {
          int c1 = AsciiString.toLowerCase(o1.charAt(i));
          int c2 = AsciiString.toLowerCase(thatValue[i]) & 0xFF;
          int result = c1 - c2;
          if (result != 0) {
            return result;
          }
        }
      }
      else
      {
        for (int i = 0; i < minLength; i++)
        {
          int c1 = AsciiString.toLowerCase(o1.charAt(i));
          int c2 = AsciiString.toLowerCase(o2.charAt(i));
          int result = c1 - c2;
          if (result != 0) {
            return result;
          }
        }
      }
      return length1 - length2;
    }
  };
  public static final Comparator<CharSequence> CHARSEQUENCE_CASE_SENSITIVE_ORDER = new Comparator()
  {
    public int compare(CharSequence o1, CharSequence o2)
    {
      if (o1 == o2) {
        return 0;
      }
      AsciiString a1 = (o1 instanceof AsciiString) ? (AsciiString)o1 : null;
      AsciiString a2 = (o2 instanceof AsciiString) ? (AsciiString)o2 : null;
      
      int length1 = o1.length();
      int length2 = o2.length();
      int minLength = Math.min(length1, length2);
      if ((a1 != null) && (a2 != null))
      {
        byte[] thisValue = a1.value;
        byte[] thatValue = a2.value;
        for (int i = 0; i < minLength; i++)
        {
          byte v1 = thisValue[i];
          byte v2 = thatValue[i];
          int result = v1 - v2;
          if (result != 0) {
            return result;
          }
        }
      }
      else if (a1 != null)
      {
        byte[] thisValue = a1.value;
        for (int i = 0; i < minLength; i++)
        {
          int c1 = thisValue[i];
          int c2 = o2.charAt(i);
          int result = c1 - c2;
          if (result != 0) {
            return result;
          }
        }
      }
      else if (a2 != null)
      {
        byte[] thatValue = a2.value;
        for (int i = 0; i < minLength; i++)
        {
          int c1 = o1.charAt(i);
          int c2 = thatValue[i];
          int result = c1 - c2;
          if (result != 0) {
            return result;
          }
        }
      }
      else
      {
        for (int i = 0; i < minLength; i++)
        {
          int c1 = o1.charAt(i);
          int c2 = o2.charAt(i);
          int result = c1 - c2;
          if (result != 0) {
            return result;
          }
        }
      }
      return length1 - length2;
    }
  };
  private final byte[] value;
  private String string;
  private int hash;
  
  public static int caseInsensitiveHashCode(CharSequence value)
  {
    if ((value instanceof AsciiString)) {
      return value.hashCode();
    }
    int hash = 0;
    int end = value.length();
    for (int i = 0; i < end; i++) {
      hash = hash * 31 ^ value.charAt(i) & 0x1F;
    }
    return hash;
  }
  
  public static boolean equalsIgnoreCase(CharSequence a, CharSequence b)
  {
    if (a == b) {
      return true;
    }
    if ((a instanceof AsciiString))
    {
      AsciiString aa = (AsciiString)a;
      return aa.equalsIgnoreCase(b);
    }
    if ((b instanceof AsciiString))
    {
      AsciiString ab = (AsciiString)b;
      return ab.equalsIgnoreCase(a);
    }
    if ((a == null) || (b == null)) {
      return false;
    }
    return a.toString().equalsIgnoreCase(b.toString());
  }
  
  public static boolean equals(CharSequence a, CharSequence b)
  {
    if (a == b) {
      return true;
    }
    if ((a instanceof AsciiString))
    {
      AsciiString aa = (AsciiString)a;
      return aa.equals(b);
    }
    if ((b instanceof AsciiString))
    {
      AsciiString ab = (AsciiString)b;
      return ab.equals(a);
    }
    if ((a == null) || (b == null)) {
      return false;
    }
    return a.equals(b);
  }
  
  public static byte[] getBytes(CharSequence v, Charset charset)
  {
    if ((v instanceof AsciiString)) {
      return ((AsciiString)v).array();
    }
    if ((v instanceof String)) {
      return ((String)v).getBytes(charset);
    }
    if (v != null)
    {
      ByteBuf buf = Unpooled.copiedBuffer(v, charset);
      try
      {
        if (buf.hasArray()) {
          return buf.array();
        }
        byte[] result = new byte[buf.readableBytes()];
        buf.readBytes(result);
        return result;
      }
      finally
      {
        buf.release();
      }
    }
    return null;
  }
  
  public static AsciiString of(CharSequence string)
  {
    return (string instanceof AsciiString) ? (AsciiString)string : new AsciiString(string);
  }
  
  public AsciiString(byte[] value)
  {
    this(value, true);
  }
  
  public AsciiString(byte[] value, boolean copy)
  {
    checkNull(value);
    if (copy) {
      this.value = ((byte[])value.clone());
    } else {
      this.value = value;
    }
  }
  
  public AsciiString(byte[] value, int start, int length)
  {
    this(value, start, length, true);
  }
  
  public AsciiString(byte[] value, int start, int length, boolean copy)
  {
    checkNull(value);
    if ((start < 0) || (start > value.length - length)) {
      throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= start + length(" + length + ") <= " + "value.length(" + value.length + ')');
    }
    if ((copy) || (start != 0) || (length != value.length)) {
      this.value = Arrays.copyOfRange(value, start, start + length);
    } else {
      this.value = value;
    }
  }
  
  public AsciiString(char[] value)
  {
    this((char[])checkNull(value), 0, value.length);
  }
  
  public AsciiString(char[] value, int start, int length)
  {
    checkNull(value);
    if ((start < 0) || (start > value.length - length)) {
      throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= start + length(" + length + ") <= " + "value.length(" + value.length + ')');
    }
    this.value = new byte[length];
    int i = 0;
    for (int j = start; i < length; j++)
    {
      this.value[i] = c2b(value[j]);i++;
    }
  }
  
  public AsciiString(CharSequence value)
  {
    this((CharSequence)checkNull(value), 0, value.length());
  }
  
  public AsciiString(CharSequence value, int start, int length)
  {
    if (value == null) {
      throw new NullPointerException("value");
    }
    if ((start < 0) || (length < 0) || (length > value.length() - start)) {
      throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= start + length(" + length + ") <= " + "value.length(" + value.length() + ')');
    }
    this.value = new byte[length];
    for (int i = 0; i < length; i++) {
      this.value[i] = c2b(value.charAt(start + i));
    }
  }
  
  public AsciiString(ByteBuffer value)
  {
    this((ByteBuffer)checkNull(value), value.position(), value.remaining());
  }
  
  public AsciiString(ByteBuffer value, int start, int length)
  {
    if (value == null) {
      throw new NullPointerException("value");
    }
    if ((start < 0) || (length > value.capacity() - start)) {
      throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= start + length(" + length + ") <= " + "value.capacity(" + value.capacity() + ')');
    }
    if (value.hasArray())
    {
      int baseOffset = value.arrayOffset() + start;
      this.value = Arrays.copyOfRange(value.array(), baseOffset, baseOffset + length);
    }
    else
    {
      this.value = new byte[length];
      int oldPos = value.position();
      value.get(this.value, 0, this.value.length);
      value.position(oldPos);
    }
  }
  
  private static <T> T checkNull(T value)
  {
    if (value == null) {
      throw new NullPointerException("value");
    }
    return value;
  }
  
  public int length()
  {
    return this.value.length;
  }
  
  public char charAt(int index)
  {
    return (char)(byteAt(index) & 0xFF);
  }
  
  public byte byteAt(int index)
  {
    return this.value[index];
  }
  
  public byte[] array()
  {
    return this.value;
  }
  
  public int arrayOffset()
  {
    return 0;
  }
  
  private static byte c2b(char c)
  {
    if (c > 'ÿ') {
      return 63;
    }
    return (byte)c;
  }
  
  private static byte toLowerCase(byte b)
  {
    if ((65 <= b) && (b <= 90)) {
      return (byte)(b + 32);
    }
    return b;
  }
  
  private static char toLowerCase(char c)
  {
    if (('A' <= c) && (c <= 'Z')) {
      return (char)(c + ' ');
    }
    return c;
  }
  
  private static byte toUpperCase(byte b)
  {
    if ((97 <= b) && (b <= 122)) {
      return (byte)(b - 32);
    }
    return b;
  }
  
  public AsciiString subSequence(int start)
  {
    return subSequence(start, length());
  }
  
  public AsciiString subSequence(int start, int end)
  {
    if ((start < 0) || (start > end) || (end > length())) {
      throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= end (" + end + ") <= length(" + length() + ')');
    }
    byte[] value = this.value;
    if ((start == 0) && (end == value.length)) {
      return this;
    }
    if (end == start) {
      return EMPTY_STRING;
    }
    return new AsciiString(value, start, end - start, false);
  }
  
  public int hashCode()
  {
    int hash = this.hash;
    byte[] value = this.value;
    if ((hash != 0) || (value.length == 0)) {
      return hash;
    }
    for (int i = 0; i < value.length; i++) {
      hash = hash * 31 ^ value[i] & 0x1F;
    }
    return this.hash = hash;
  }
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof AsciiString)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    AsciiString that = (AsciiString)obj;
    int thisHash = hashCode();
    int thatHash = that.hashCode();
    if ((thisHash != thatHash) || (length() != that.length())) {
      return false;
    }
    byte[] thisValue = this.value;
    byte[] thatValue = that.value;
    int end = thisValue.length;
    int i = 0;
    for (int j = 0; i < end; j++)
    {
      if (thisValue[i] != thatValue[j]) {
        return false;
      }
      i++;
    }
    return true;
  }
  
  public String toString()
  {
    String string = this.string;
    if (string != null) {
      return string;
    }
    byte[] value = this.value;
    return this.string = new String(value, 0, 0, value.length);
  }
  
  public String toString(int start, int end)
  {
    byte[] value = this.value;
    if ((start == 0) && (end == value.length)) {
      return toString();
    }
    int length = end - start;
    if (length == 0) {
      return "";
    }
    return new String(value, 0, start, length);
  }
  
  public int compareTo(CharSequence string)
  {
    if (this == string) {
      return 0;
    }
    int length1 = length();
    int length2 = string.length();
    int minLength = Math.min(length1, length2);
    byte[] value = this.value;
    int i = 0;
    for (int j = 0; j < minLength; j++)
    {
      int result = (value[i] & 0xFF) - string.charAt(j);
      if (result != 0) {
        return result;
      }
      i++;
    }
    return length1 - length2;
  }
  
  public int compareToIgnoreCase(CharSequence string)
  {
    return CHARSEQUENCE_CASE_INSENSITIVE_ORDER.compare(this, string);
  }
  
  public AsciiString concat(CharSequence string)
  {
    int thisLen = length();
    int thatLen = string.length();
    if (thatLen == 0) {
      return this;
    }
    if ((string instanceof AsciiString))
    {
      AsciiString that = (AsciiString)string;
      if (isEmpty()) {
        return that;
      }
      byte[] newValue = Arrays.copyOf(this.value, thisLen + thatLen);
      System.arraycopy(that.value, 0, newValue, thisLen, thatLen);
      
      return new AsciiString(newValue, false);
    }
    if (isEmpty()) {
      return new AsciiString(string);
    }
    int newLen = thisLen + thatLen;
    byte[] newValue = Arrays.copyOf(this.value, newLen);
    int i = thisLen;
    for (int j = 0; i < newLen; j++)
    {
      newValue[i] = c2b(string.charAt(j));i++;
    }
    return new AsciiString(newValue, false);
  }
  
  public boolean endsWith(CharSequence suffix)
  {
    int suffixLen = suffix.length();
    return regionMatches(length() - suffixLen, suffix, 0, suffixLen);
  }
  
  public boolean equalsIgnoreCase(CharSequence string)
  {
    if (string == this) {
      return true;
    }
    if (string == null) {
      return false;
    }
    byte[] value = this.value;
    int thisLen = value.length;
    int thatLen = string.length();
    if (thisLen != thatLen) {
      return false;
    }
    for (int i = 0; i < thisLen; i++)
    {
      char c1 = (char)(value[i] & 0xFF);
      char c2 = string.charAt(i);
      if ((c1 != c2) && (toLowerCase(c1) != toLowerCase(c2))) {
        return false;
      }
    }
    return true;
  }
  
  public byte[] toByteArray()
  {
    return toByteArray(0, length());
  }
  
  public byte[] toByteArray(int start, int end)
  {
    return Arrays.copyOfRange(this.value, start, end);
  }
  
  public char[] toCharArray()
  {
    return toCharArray(0, length());
  }
  
  public char[] toCharArray(int start, int end)
  {
    int length = end - start;
    if (length == 0) {
      return EmptyArrays.EMPTY_CHARS;
    }
    byte[] value = this.value;
    char[] buffer = new char[length];
    int i = 0;
    for (int j = start; i < length; j++)
    {
      buffer[i] = ((char)(value[j] & 0xFF));i++;
    }
    return buffer;
  }
  
  public void copy(int srcIdx, ByteBuf dst, int dstIdx, int length)
  {
    if (dst == null) {
      throw new NullPointerException("dst");
    }
    byte[] value = this.value;
    int thisLen = value.length;
    if ((srcIdx < 0) || (length > thisLen - srcIdx)) {
      throw new IndexOutOfBoundsException("expected: 0 <= srcIdx(" + srcIdx + ") <= srcIdx + length(" + length + ") <= srcLen(" + thisLen + ')');
    }
    dst.setBytes(dstIdx, value, srcIdx, length);
  }
  
  public void copy(int srcIdx, ByteBuf dst, int length)
  {
    if (dst == null) {
      throw new NullPointerException("dst");
    }
    byte[] value = this.value;
    int thisLen = value.length;
    if ((srcIdx < 0) || (length > thisLen - srcIdx)) {
      throw new IndexOutOfBoundsException("expected: 0 <= srcIdx(" + srcIdx + ") <= srcIdx + length(" + length + ") <= srcLen(" + thisLen + ')');
    }
    dst.writeBytes(value, srcIdx, length);
  }
  
  public void copy(int srcIdx, byte[] dst, int dstIdx, int length)
  {
    if (dst == null) {
      throw new NullPointerException("dst");
    }
    byte[] value = this.value;
    int thisLen = value.length;
    if ((srcIdx < 0) || (length > thisLen - srcIdx)) {
      throw new IndexOutOfBoundsException("expected: 0 <= srcIdx(" + srcIdx + ") <= srcIdx + length(" + length + ") <= srcLen(" + thisLen + ')');
    }
    System.arraycopy(value, srcIdx, dst, dstIdx, length);
  }
  
  public void copy(int srcIdx, char[] dst, int dstIdx, int length)
  {
    if (dst == null) {
      throw new NullPointerException("dst");
    }
    byte[] value = this.value;
    int thisLen = value.length;
    if ((srcIdx < 0) || (length > thisLen - srcIdx)) {
      throw new IndexOutOfBoundsException("expected: 0 <= srcIdx(" + srcIdx + ") <= srcIdx + length(" + length + ") <= srcLen(" + thisLen + ')');
    }
    int dstEnd = dstIdx + length;
    int i = srcIdx;
    for (int j = dstIdx; j < dstEnd; j++)
    {
      dst[j] = ((char)(value[i] & 0xFF));i++;
    }
  }
  
  public int indexOf(int c)
  {
    return indexOf(c, 0);
  }
  
  public int indexOf(int c, int start)
  {
    byte[] value = this.value;
    int length = value.length;
    if (start < length)
    {
      if (start < 0) {
        start = 0;
      }
      for (int i = start; i < length; i++) {
        if ((value[i] & 0xFF) == c) {
          return i;
        }
      }
    }
    return -1;
  }
  
  public int indexOf(CharSequence string)
  {
    return indexOf(string, 0);
  }
  
  public int indexOf(CharSequence subString, int start)
  {
    if (start < 0) {
      start = 0;
    }
    byte[] value = this.value;
    int thisLen = value.length;
    
    int subCount = subString.length();
    if (subCount <= 0) {
      return start < thisLen ? start : thisLen;
    }
    if (subCount > thisLen - start) {
      return -1;
    }
    char firstChar = subString.charAt(0);
    for (;;)
    {
      int i = indexOf(firstChar, start);
      if ((i == -1) || (subCount + i > thisLen)) {
        return -1;
      }
      int o1 = i;int o2 = 0;
      do
      {
        o2++;
      } while ((o2 < subCount) && ((value[(++o1)] & 0xFF) == subString.charAt(o2)));
      if (o2 == subCount) {
        return i;
      }
      start = i + 1;
    }
  }
  
  public int lastIndexOf(int c)
  {
    return lastIndexOf(c, length() - 1);
  }
  
  public int lastIndexOf(int c, int start)
  {
    if (start >= 0)
    {
      byte[] value = this.value;
      int length = value.length;
      if (start >= length) {
        start = length - 1;
      }
      for (int i = start; i >= 0; i--) {
        if ((value[i] & 0xFF) == c) {
          return i;
        }
      }
    }
    return -1;
  }
  
  public int lastIndexOf(CharSequence string)
  {
    return lastIndexOf(string, length());
  }
  
  public int lastIndexOf(CharSequence subString, int start)
  {
    byte[] value = this.value;
    int thisLen = value.length;
    int subCount = subString.length();
    if ((subCount > thisLen) || (start < 0)) {
      return -1;
    }
    if (subCount <= 0) {
      return start < thisLen ? start : thisLen;
    }
    start = Math.min(start, thisLen - subCount);
    
    char firstChar = subString.charAt(0);
    for (;;)
    {
      int i = lastIndexOf(firstChar, start);
      if (i == -1) {
        return -1;
      }
      int o1 = i;int o2 = 0;
      do
      {
        o2++;
      } while ((o2 < subCount) && ((value[(++o1)] & 0xFF) == subString.charAt(o2)));
      if (o2 == subCount) {
        return i;
      }
      start = i - 1;
    }
  }
  
  public boolean isEmpty()
  {
    return this.value.length == 0;
  }
  
  public boolean regionMatches(int thisStart, CharSequence string, int start, int length)
  {
    if (string == null) {
      throw new NullPointerException("string");
    }
    if ((start < 0) || (string.length() - start < length)) {
      return false;
    }
    byte[] value = this.value;
    int thisLen = value.length;
    if ((thisStart < 0) || (thisLen - thisStart < length)) {
      return false;
    }
    if (length <= 0) {
      return true;
    }
    int thisEnd = thisStart + length;
    int i = thisStart;
    for (int j = start; i < thisEnd; j++)
    {
      if ((value[i] & 0xFF) != string.charAt(j)) {
        return false;
      }
      i++;
    }
    return true;
  }
  
  public boolean regionMatches(boolean ignoreCase, int thisStart, CharSequence string, int start, int length)
  {
    if (!ignoreCase) {
      return regionMatches(thisStart, string, start, length);
    }
    if (string == null) {
      throw new NullPointerException("string");
    }
    byte[] value = this.value;
    int thisLen = value.length;
    if ((thisStart < 0) || (length > thisLen - thisStart)) {
      return false;
    }
    if ((start < 0) || (length > string.length() - start)) {
      return false;
    }
    int thisEnd = thisStart + length;
    while (thisStart < thisEnd)
    {
      char c1 = (char)(value[(thisStart++)] & 0xFF);
      char c2 = string.charAt(start++);
      if ((c1 != c2) && (toLowerCase(c1) != toLowerCase(c2))) {
        return false;
      }
    }
    return true;
  }
  
  public AsciiString replace(char oldChar, char newChar)
  {
    int index = indexOf(oldChar, 0);
    if (index == -1) {
      return this;
    }
    byte[] value = this.value;
    int count = value.length;
    byte[] buffer = new byte[count];
    int i = 0;
    for (int j = 0; i < value.length; j++)
    {
      byte b = value[i];
      if ((char)(b & 0xFF) == oldChar) {
        b = (byte)newChar;
      }
      buffer[j] = b;i++;
    }
    return new AsciiString(buffer, false);
  }
  
  public boolean startsWith(CharSequence prefix)
  {
    return startsWith(prefix, 0);
  }
  
  public boolean startsWith(CharSequence prefix, int start)
  {
    return regionMatches(start, prefix, 0, prefix.length());
  }
  
  public AsciiString toLowerCase()
  {
    boolean lowercased = true;
    byte[] value = this.value;
    for (int i = 0; i < value.length; i++)
    {
      byte b = value[i];
      if ((b >= 65) && (b <= 90))
      {
        lowercased = false;
        break;
      }
    }
    if (lowercased) {
      return this;
    }
    int length = value.length;
    byte[] newValue = new byte[length];
    i = 0;
    for (int j = 0; i < length; j++)
    {
      newValue[i] = toLowerCase(value[j]);i++;
    }
    return new AsciiString(newValue, false);
  }
  
  public AsciiString toUpperCase()
  {
    byte[] value = this.value;
    boolean uppercased = true;
    for (int i = 0; i < value.length; i++)
    {
      byte b = value[i];
      if ((b >= 97) && (b <= 122))
      {
        uppercased = false;
        break;
      }
    }
    if (uppercased) {
      return this;
    }
    int length = value.length;
    byte[] newValue = new byte[length];
    i = 0;
    for (int j = 0; i < length; j++)
    {
      newValue[i] = toUpperCase(value[j]);i++;
    }
    return new AsciiString(newValue, false);
  }
  
  public AsciiString trim()
  {
    byte[] value = this.value;
    int start = 0;int last = value.length;
    int end = last;
    while ((start <= end) && (value[start] <= 32)) {
      start++;
    }
    while ((end >= start) && (value[end] <= 32)) {
      end--;
    }
    if ((start == 0) && (end == last)) {
      return this;
    }
    return new AsciiString(value, start, end - start + 1, false);
  }
  
  public boolean contentEquals(CharSequence cs)
  {
    if (cs == null) {
      throw new NullPointerException();
    }
    int length1 = length();
    int length2 = cs.length();
    if (length1 != length2) {
      return false;
    }
    if ((length1 == 0) && (length2 == 0)) {
      return true;
    }
    return regionMatches(0, cs, 0, length2);
  }
  
  public boolean matches(String expr)
  {
    return Pattern.matches(expr, this);
  }
  
  public AsciiString[] split(String expr, int max)
  {
    return toAsciiStringArray(Pattern.compile(expr).split(this, max));
  }
  
  private static AsciiString[] toAsciiStringArray(String[] jdkResult)
  {
    AsciiString[] res = new AsciiString[jdkResult.length];
    for (int i = 0; i < jdkResult.length; i++) {
      res[i] = new AsciiString(jdkResult[i]);
    }
    return res;
  }
  
  public AsciiString[] split(char delim)
  {
    List<AsciiString> res = new ArrayList();
    
    int start = 0;
    byte[] value = this.value;
    int length = value.length;
    for (int i = start; i < length; i++) {
      if (charAt(i) == delim)
      {
        if (start == i) {
          res.add(EMPTY_STRING);
        } else {
          res.add(new AsciiString(value, start, i - start, false));
        }
        start = i + 1;
      }
    }
    if (start == 0) {
      res.add(this);
    } else if (start != length) {
      res.add(new AsciiString(value, start, length - start, false));
    } else {
      for (int i = res.size() - 1; i >= 0; i--)
      {
        if (!((AsciiString)res.get(i)).isEmpty()) {
          break;
        }
        res.remove(i);
      }
    }
    return (AsciiString[])res.toArray(new AsciiString[res.size()]);
  }
  
  public boolean contains(CharSequence cs)
  {
    if (cs == null) {
      throw new NullPointerException();
    }
    return indexOf(cs) >= 0;
  }
  
  public int parseInt()
  {
    return parseInt(0, length(), 10);
  }
  
  public int parseInt(int radix)
  {
    return parseInt(0, length(), radix);
  }
  
  public int parseInt(int start, int end)
  {
    return parseInt(start, end, 10);
  }
  
  public int parseInt(int start, int end, int radix)
  {
    if ((radix < 2) || (radix > 36)) {
      throw new NumberFormatException();
    }
    if (start == end) {
      throw new NumberFormatException();
    }
    int i = start;
    boolean negative = charAt(i) == '-';
    if (negative)
    {
      i++;
      if (i == end) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
    }
    return parseInt(i, end, radix, negative);
  }
  
  private int parseInt(int start, int end, int radix, boolean negative)
  {
    byte[] value = this.value;
    int max = Integer.MIN_VALUE / radix;
    int result = 0;
    int offset = start;
    while (offset < end)
    {
      int digit = Character.digit((char)(value[(offset++)] & 0xFF), radix);
      if (digit == -1) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
      if (max > result) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
      int next = result * radix - digit;
      if (next > result) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
      result = next;
    }
    if (!negative)
    {
      result = -result;
      if (result < 0) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
    }
    return result;
  }
  
  public long parseLong()
  {
    return parseLong(0, length(), 10);
  }
  
  public long parseLong(int radix)
  {
    return parseLong(0, length(), radix);
  }
  
  public long parseLong(int start, int end)
  {
    return parseLong(start, end, 10);
  }
  
  public long parseLong(int start, int end, int radix)
  {
    if ((radix < 2) || (radix > 36)) {
      throw new NumberFormatException();
    }
    if (start == end) {
      throw new NumberFormatException();
    }
    int i = start;
    boolean negative = charAt(i) == '-';
    if (negative)
    {
      i++;
      if (i == end) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
    }
    return parseLong(i, end, radix, negative);
  }
  
  private long parseLong(int start, int end, int radix, boolean negative)
  {
    byte[] value = this.value;
    long max = Long.MIN_VALUE / radix;
    long result = 0L;
    int offset = start;
    while (offset < end)
    {
      int digit = Character.digit((char)(value[(offset++)] & 0xFF), radix);
      if (digit == -1) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
      if (max > result) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
      long next = result * radix - digit;
      if (next > result) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
      result = next;
    }
    if (!negative)
    {
      result = -result;
      if (result < 0L) {
        throw new NumberFormatException(subSequence(start, end).toString());
      }
    }
    return result;
  }
  
  public short parseShort()
  {
    return parseShort(0, length(), 10);
  }
  
  public short parseShort(int radix)
  {
    return parseShort(0, length(), radix);
  }
  
  public short parseShort(int start, int end)
  {
    return parseShort(start, end, 10);
  }
  
  public short parseShort(int start, int end, int radix)
  {
    int intValue = parseInt(start, end, radix);
    short result = (short)intValue;
    if (result != intValue) {
      throw new NumberFormatException(subSequence(start, end).toString());
    }
    return result;
  }
  
  public float parseFloat()
  {
    return parseFloat(0, length());
  }
  
  public float parseFloat(int start, int end)
  {
    return Float.parseFloat(toString(start, end));
  }
  
  public double parseDouble()
  {
    return parseDouble(0, length());
  }
  
  public double parseDouble(int start, int end)
  {
    return Double.parseDouble(toString(start, end));
  }
}
