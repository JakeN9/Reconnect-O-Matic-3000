package io.netty.handler.codec;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class EmptyHeaders<T>
  implements Headers<T>
{
  public T get(T name)
  {
    return null;
  }
  
  public T get(T name, T defaultValue)
  {
    return null;
  }
  
  public T getAndRemove(T name)
  {
    return null;
  }
  
  public T getAndRemove(T name, T defaultValue)
  {
    return null;
  }
  
  public List<T> getAll(T name)
  {
    return Collections.emptyList();
  }
  
  public List<T> getAllAndRemove(T name)
  {
    return Collections.emptyList();
  }
  
  public Boolean getBoolean(T name)
  {
    return null;
  }
  
  public boolean getBoolean(T name, boolean defaultValue)
  {
    return defaultValue;
  }
  
  public Byte getByte(T name)
  {
    return null;
  }
  
  public byte getByte(T name, byte defaultValue)
  {
    return defaultValue;
  }
  
  public Character getChar(T name)
  {
    return null;
  }
  
  public char getChar(T name, char defaultValue)
  {
    return defaultValue;
  }
  
  public Short getShort(T name)
  {
    return null;
  }
  
  public short getInt(T name, short defaultValue)
  {
    return defaultValue;
  }
  
  public Integer getInt(T name)
  {
    return null;
  }
  
  public int getInt(T name, int defaultValue)
  {
    return defaultValue;
  }
  
  public Long getLong(T name)
  {
    return null;
  }
  
  public long getLong(T name, long defaultValue)
  {
    return defaultValue;
  }
  
  public Float getFloat(T name)
  {
    return null;
  }
  
  public float getFloat(T name, float defaultValue)
  {
    return defaultValue;
  }
  
  public Double getDouble(T name)
  {
    return null;
  }
  
  public double getDouble(T name, double defaultValue)
  {
    return defaultValue;
  }
  
  public Long getTimeMillis(T name)
  {
    return null;
  }
  
  public long getTimeMillis(T name, long defaultValue)
  {
    return defaultValue;
  }
  
  public Boolean getBooleanAndRemove(T name)
  {
    return null;
  }
  
  public boolean getBooleanAndRemove(T name, boolean defaultValue)
  {
    return defaultValue;
  }
  
  public Byte getByteAndRemove(T name)
  {
    return null;
  }
  
  public byte getByteAndRemove(T name, byte defaultValue)
  {
    return defaultValue;
  }
  
  public Character getCharAndRemove(T name)
  {
    return null;
  }
  
  public char getCharAndRemove(T name, char defaultValue)
  {
    return defaultValue;
  }
  
  public Short getShortAndRemove(T name)
  {
    return null;
  }
  
  public short getShortAndRemove(T name, short defaultValue)
  {
    return defaultValue;
  }
  
  public Integer getIntAndRemove(T name)
  {
    return null;
  }
  
  public int getIntAndRemove(T name, int defaultValue)
  {
    return defaultValue;
  }
  
  public Long getLongAndRemove(T name)
  {
    return null;
  }
  
  public long getLongAndRemove(T name, long defaultValue)
  {
    return defaultValue;
  }
  
  public Float getFloatAndRemove(T name)
  {
    return null;
  }
  
  public float getFloatAndRemove(T name, float defaultValue)
  {
    return defaultValue;
  }
  
  public Double getDoubleAndRemove(T name)
  {
    return null;
  }
  
  public double getDoubleAndRemove(T name, double defaultValue)
  {
    return defaultValue;
  }
  
  public Long getTimeMillisAndRemove(T name)
  {
    return null;
  }
  
  public long getTimeMillisAndRemove(T name, long defaultValue)
  {
    return defaultValue;
  }
  
  public List<Map.Entry<T, T>> entries()
  {
    return Collections.emptyList();
  }
  
  public boolean contains(T name)
  {
    return false;
  }
  
  public boolean contains(T name, T value)
  {
    return false;
  }
  
  public boolean containsObject(T name, Object value)
  {
    return false;
  }
  
  public boolean containsBoolean(T name, boolean value)
  {
    return false;
  }
  
  public boolean containsByte(T name, byte value)
  {
    return false;
  }
  
  public boolean containsChar(T name, char value)
  {
    return false;
  }
  
  public boolean containsShort(T name, short value)
  {
    return false;
  }
  
  public boolean containsInt(T name, int value)
  {
    return false;
  }
  
  public boolean containsLong(T name, long value)
  {
    return false;
  }
  
  public boolean containsFloat(T name, float value)
  {
    return false;
  }
  
  public boolean containsDouble(T name, double value)
  {
    return false;
  }
  
  public boolean containsTimeMillis(T name, long value)
  {
    return false;
  }
  
  public boolean contains(T name, T value, Comparator<? super T> comparator)
  {
    return false;
  }
  
  public boolean contains(T name, T value, Comparator<? super T> keyComparator, Comparator<? super T> valueComparator)
  {
    return false;
  }
  
  public boolean containsObject(T name, Object value, Comparator<? super T> comparator)
  {
    return false;
  }
  
  public boolean containsObject(T name, Object value, Comparator<? super T> keyComparator, Comparator<? super T> valueComparator)
  {
    return false;
  }
  
  public int size()
  {
    return 0;
  }
  
  public boolean isEmpty()
  {
    return true;
  }
  
  public Set<T> names()
  {
    return Collections.emptySet();
  }
  
  public List<T> namesList()
  {
    return Collections.emptyList();
  }
  
  public Headers<T> add(T name, T value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> add(T name, Iterable<? extends T> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> add(T name, T... values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addObject(T name, Object value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addObject(T name, Iterable<?> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addObject(T name, Object... values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addBoolean(T name, boolean value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addByte(T name, byte value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addChar(T name, char value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addShort(T name, short value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addInt(T name, int value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addLong(T name, long value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addFloat(T name, float value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addDouble(T name, double value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> addTimeMillis(T name, long value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> add(Headers<T> headers)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> set(T name, T value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> set(T name, Iterable<? extends T> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> set(T name, T... values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setObject(T name, Object value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setObject(T name, Iterable<?> values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setObject(T name, Object... values)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setBoolean(T name, boolean value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setByte(T name, byte value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setChar(T name, char value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setShort(T name, short value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setInt(T name, int value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setLong(T name, long value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setFloat(T name, float value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setDouble(T name, double value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setTimeMillis(T name, long value)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> set(Headers<T> headers)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public Headers<T> setAll(Headers<T> headers)
  {
    throw new UnsupportedOperationException("read only");
  }
  
  public boolean remove(T name)
  {
    return false;
  }
  
  public Headers<T> clear()
  {
    return this;
  }
  
  public Iterator<Map.Entry<T, T>> iterator()
  {
    return entries().iterator();
  }
  
  public Map.Entry<T, T> forEachEntry(Headers.EntryVisitor<T> visitor)
    throws Exception
  {
    return null;
  }
  
  public T forEachName(Headers.NameVisitor<T> visitor)
    throws Exception
  {
    return null;
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof Headers)) {
      return false;
    }
    Headers<?> rhs = (Headers)o;
    return (isEmpty()) && (rhs.isEmpty());
  }
  
  public int hashCode()
  {
    return 1;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + '[' + ']';
  }
}
