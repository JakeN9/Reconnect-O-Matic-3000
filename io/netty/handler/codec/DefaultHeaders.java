package io.netty.handler.codec;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

public class DefaultHeaders<T>
  implements Headers<T>
{
  private static final int HASH_CODE_PRIME = 31;
  private static final int DEFAULT_BUCKET_SIZE = 17;
  private static final int DEFAULT_MAP_SIZE = 4;
  
  public static abstract interface HashCodeGenerator<T>
  {
    public abstract int generateHashCode(T paramT);
  }
  
  public static abstract interface NameConverter<T>
  {
    public abstract T convertName(T paramT);
  }
  
  public static final class IdentityNameConverter<T>
    implements DefaultHeaders.NameConverter<T>
  {
    public T convertName(T name)
    {
      return name;
    }
  }
  
  private static final NameConverter<Object> DEFAULT_NAME_CONVERTER = new IdentityNameConverter();
  private final IntObjectMap<DefaultHeaders<T>.HeaderEntry> entries;
  private final IntObjectMap<DefaultHeaders<T>.HeaderEntry> tailEntries;
  private final DefaultHeaders<T>.HeaderEntry head;
  private final Comparator<? super T> keyComparator;
  private final Comparator<? super T> valueComparator;
  private final HashCodeGenerator<T> hashCodeGenerator;
  private final Headers.ValueConverter<T> valueConverter;
  private final NameConverter<T> nameConverter;
  private final int bucketSize;
  int size;
  
  public DefaultHeaders(Comparator<? super T> keyComparator, Comparator<? super T> valueComparator, HashCodeGenerator<T> hashCodeGenerator, Headers.ValueConverter<T> typeConverter)
  {
    this(keyComparator, valueComparator, hashCodeGenerator, typeConverter, DEFAULT_NAME_CONVERTER);
  }
  
  public DefaultHeaders(Comparator<? super T> keyComparator, Comparator<? super T> valueComparator, HashCodeGenerator<T> hashCodeGenerator, Headers.ValueConverter<T> typeConverter, NameConverter<T> nameConverter)
  {
    this(keyComparator, valueComparator, hashCodeGenerator, typeConverter, nameConverter, 17, 4);
  }
  
  public DefaultHeaders(Comparator<? super T> keyComparator, Comparator<? super T> valueComparator, HashCodeGenerator<T> hashCodeGenerator, Headers.ValueConverter<T> valueConverter, NameConverter<T> nameConverter, int bucketSize, int initialMapSize)
  {
    if (keyComparator == null) {
      throw new NullPointerException("keyComparator");
    }
    if (valueComparator == null) {
      throw new NullPointerException("valueComparator");
    }
    if (hashCodeGenerator == null) {
      throw new NullPointerException("hashCodeGenerator");
    }
    if (valueConverter == null) {
      throw new NullPointerException("valueConverter");
    }
    if (nameConverter == null) {
      throw new NullPointerException("nameConverter");
    }
    if (bucketSize < 1) {
      throw new IllegalArgumentException("bucketSize must be a positive integer");
    }
    this.head = new HeaderEntry();
    this.head.before = (this.head.after = this.head);
    this.keyComparator = keyComparator;
    this.valueComparator = valueComparator;
    this.hashCodeGenerator = hashCodeGenerator;
    this.valueConverter = valueConverter;
    this.nameConverter = nameConverter;
    this.bucketSize = bucketSize;
    this.entries = new IntObjectHashMap(initialMapSize);
    this.tailEntries = new IntObjectHashMap(initialMapSize);
  }
  
  public T get(T name)
  {
    ObjectUtil.checkNotNull(name, "name");
    
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    DefaultHeaders<T>.HeaderEntry e = (HeaderEntry)this.entries.get(i);
    while (e != null)
    {
      if ((e.hash == h) && (this.keyComparator.compare(e.name, name) == 0)) {
        return (T)e.value;
      }
      e = e.next;
    }
    return null;
  }
  
  public T get(T name, T defaultValue)
  {
    T value = get(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
  
  public T getAndRemove(T name)
  {
    ObjectUtil.checkNotNull(name, "name");
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    DefaultHeaders<T>.HeaderEntry e = (HeaderEntry)this.entries.get(i);
    if (e == null) {
      return null;
    }
    T value = null;
    while ((e.hash == h) && (this.keyComparator.compare(e.name, name) == 0))
    {
      if (value == null) {
        value = e.value;
      }
      e.remove();
      DefaultHeaders<T>.HeaderEntry next = e.next;
      if (next != null)
      {
        this.entries.put(i, next);
        e = next;
      }
      else
      {
        this.entries.remove(i);
        this.tailEntries.remove(i);
        return value;
      }
    }
    for (;;)
    {
      DefaultHeaders<T>.HeaderEntry next = e.next;
      if (next == null) {
        break;
      }
      if ((next.hash == h) && (this.keyComparator.compare(e.name, name) == 0))
      {
        if (value == null) {
          value = next.value;
        }
        e.next = next.next;
        if (e.next == null) {
          this.tailEntries.put(i, e);
        }
        next.remove();
      }
      else
      {
        e = next;
      }
    }
    return value;
  }
  
  public T getAndRemove(T name, T defaultValue)
  {
    T value = getAndRemove(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
  
  public List<T> getAll(T name)
  {
    ObjectUtil.checkNotNull(name, "name");
    List<T> values = new ArrayList(4);
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    DefaultHeaders<T>.HeaderEntry e = (HeaderEntry)this.entries.get(i);
    while (e != null)
    {
      if ((e.hash == h) && (this.keyComparator.compare(e.name, name) == 0)) {
        values.add(e.value);
      }
      e = e.next;
    }
    return values;
  }
  
  public List<T> getAllAndRemove(T name)
  {
    ObjectUtil.checkNotNull(name, "name");
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    DefaultHeaders<T>.HeaderEntry e = (HeaderEntry)this.entries.get(i);
    if (e == null) {
      return null;
    }
    List<T> values = new ArrayList(4);
    while ((e.hash == h) && (this.keyComparator.compare(e.name, name) == 0))
    {
      values.add(e.value);
      e.remove();
      DefaultHeaders<T>.HeaderEntry next = e.next;
      if (next != null)
      {
        this.entries.put(i, next);
        e = next;
      }
      else
      {
        this.entries.remove(i);
        this.tailEntries.remove(i);
        return values;
      }
    }
    for (;;)
    {
      DefaultHeaders<T>.HeaderEntry next = e.next;
      if (next == null) {
        break;
      }
      if ((next.hash == h) && (this.keyComparator.compare(next.name, name) == 0))
      {
        values.add(next.value);
        e.next = next.next;
        if (e.next == null) {
          this.tailEntries.put(i, e);
        }
        next.remove();
      }
      else
      {
        e = next;
      }
    }
    return values;
  }
  
  public List<Map.Entry<T, T>> entries()
  {
    int size = size();
    List<Map.Entry<T, T>> localEntries = new ArrayList(size);
    
    DefaultHeaders<T>.HeaderEntry e = this.head.after;
    while (e != this.head)
    {
      localEntries.add(e);
      e = e.after;
    }
    assert (size == localEntries.size());
    return localEntries;
  }
  
  public boolean contains(T name)
  {
    return get(name) != null;
  }
  
  public boolean contains(T name, T value)
  {
    return contains(name, value, this.keyComparator, this.valueComparator);
  }
  
  public boolean containsObject(T name, Object value)
  {
    return contains(name, this.valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")));
  }
  
  public boolean containsBoolean(T name, boolean value)
  {
    return contains(name, this.valueConverter.convertBoolean(((Boolean)ObjectUtil.checkNotNull(Boolean.valueOf(value), "value")).booleanValue()));
  }
  
  public boolean containsByte(T name, byte value)
  {
    return contains(name, this.valueConverter.convertByte(((Byte)ObjectUtil.checkNotNull(Byte.valueOf(value), "value")).byteValue()));
  }
  
  public boolean containsChar(T name, char value)
  {
    return contains(name, this.valueConverter.convertChar(((Character)ObjectUtil.checkNotNull(Character.valueOf(value), "value")).charValue()));
  }
  
  public boolean containsShort(T name, short value)
  {
    return contains(name, this.valueConverter.convertShort(((Short)ObjectUtil.checkNotNull(Short.valueOf(value), "value")).shortValue()));
  }
  
  public boolean containsInt(T name, int value)
  {
    return contains(name, this.valueConverter.convertInt(((Integer)ObjectUtil.checkNotNull(Integer.valueOf(value), "value")).intValue()));
  }
  
  public boolean containsLong(T name, long value)
  {
    return contains(name, this.valueConverter.convertLong(((Long)ObjectUtil.checkNotNull(Long.valueOf(value), "value")).longValue()));
  }
  
  public boolean containsFloat(T name, float value)
  {
    return contains(name, this.valueConverter.convertFloat(((Float)ObjectUtil.checkNotNull(Float.valueOf(value), "value")).floatValue()));
  }
  
  public boolean containsDouble(T name, double value)
  {
    return contains(name, this.valueConverter.convertDouble(((Double)ObjectUtil.checkNotNull(Double.valueOf(value), "value")).doubleValue()));
  }
  
  public boolean containsTimeMillis(T name, long value)
  {
    return contains(name, this.valueConverter.convertTimeMillis(((Long)ObjectUtil.checkNotNull(Long.valueOf(value), "value")).longValue()));
  }
  
  public boolean contains(T name, T value, Comparator<? super T> comparator)
  {
    return contains(name, value, comparator, comparator);
  }
  
  public boolean contains(T name, T value, Comparator<? super T> keyComparator, Comparator<? super T> valueComparator)
  {
    ObjectUtil.checkNotNull(name, "name");
    ObjectUtil.checkNotNull(value, "value");
    ObjectUtil.checkNotNull(keyComparator, "keyComparator");
    ObjectUtil.checkNotNull(valueComparator, "valueComparator");
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    DefaultHeaders<T>.HeaderEntry e = (HeaderEntry)this.entries.get(i);
    while (e != null)
    {
      if ((e.hash == h) && (keyComparator.compare(e.name, name) == 0) && (valueComparator.compare(e.value, value) == 0)) {
        return true;
      }
      e = e.next;
    }
    return false;
  }
  
  public boolean containsObject(T name, Object value, Comparator<? super T> comparator)
  {
    return containsObject(name, value, comparator, comparator);
  }
  
  public boolean containsObject(T name, Object value, Comparator<? super T> keyComparator, Comparator<? super T> valueComparator)
  {
    return contains(name, this.valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")), keyComparator, valueComparator);
  }
  
  public int size()
  {
    return this.size;
  }
  
  public boolean isEmpty()
  {
    return this.head == this.head.after;
  }
  
  public Set<T> names()
  {
    Set<T> names = new TreeSet(this.keyComparator);
    
    DefaultHeaders<T>.HeaderEntry e = this.head.after;
    while (e != this.head)
    {
      names.add(e.name);
      e = e.after;
    }
    return names;
  }
  
  public List<T> namesList()
  {
    List<T> names = new ArrayList(size());
    
    DefaultHeaders<T>.HeaderEntry e = this.head.after;
    while (e != this.head)
    {
      names.add(e.name);
      e = e.after;
    }
    return names;
  }
  
  public Headers<T> add(T name, T value)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(value, "value");
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    add0(h, i, name, value);
    return this;
  }
  
  public Headers<T> add(T name, Iterable<? extends T> values)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    for (T v : values)
    {
      if (v == null) {
        break;
      }
      add0(h, i, name, v);
    }
    return this;
  }
  
  public Headers<T> add(T name, T... values)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    for (T v : values)
    {
      if (v == null) {
        break;
      }
      add0(h, i, name, v);
    }
    return this;
  }
  
  public Headers<T> addObject(T name, Object value)
  {
    return add(name, this.valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")));
  }
  
  public Headers<T> addObject(T name, Iterable<?> values)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    for (Object o : values)
    {
      if (o == null) {
        break;
      }
      T converted = this.valueConverter.convertObject(o);
      ObjectUtil.checkNotNull(converted, "converted");
      add0(h, i, name, converted);
    }
    return this;
  }
  
  public Headers<T> addObject(T name, Object... values)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    for (Object o : values)
    {
      if (o == null) {
        break;
      }
      T converted = this.valueConverter.convertObject(o);
      ObjectUtil.checkNotNull(converted, "converted");
      add0(h, i, name, converted);
    }
    return this;
  }
  
  public Headers<T> addInt(T name, int value)
  {
    return add(name, this.valueConverter.convertInt(value));
  }
  
  public Headers<T> addLong(T name, long value)
  {
    return add(name, this.valueConverter.convertLong(value));
  }
  
  public Headers<T> addDouble(T name, double value)
  {
    return add(name, this.valueConverter.convertDouble(value));
  }
  
  public Headers<T> addTimeMillis(T name, long value)
  {
    return add(name, this.valueConverter.convertTimeMillis(value));
  }
  
  public Headers<T> addChar(T name, char value)
  {
    return add(name, this.valueConverter.convertChar(value));
  }
  
  public Headers<T> addBoolean(T name, boolean value)
  {
    return add(name, this.valueConverter.convertBoolean(value));
  }
  
  public Headers<T> addFloat(T name, float value)
  {
    return add(name, this.valueConverter.convertFloat(value));
  }
  
  public Headers<T> addByte(T name, byte value)
  {
    return add(name, this.valueConverter.convertByte(value));
  }
  
  public Headers<T> addShort(T name, short value)
  {
    return add(name, this.valueConverter.convertShort(value));
  }
  
  public Headers<T> add(Headers<T> headers)
  {
    ObjectUtil.checkNotNull(headers, "headers");
    
    add0(headers);
    return this;
  }
  
  public Headers<T> set(T name, T value)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(value, "value");
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    remove0(h, i, name);
    add0(h, i, name, value);
    return this;
  }
  
  public Headers<T> set(T name, Iterable<? extends T> values)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    remove0(h, i, name);
    for (T v : values)
    {
      if (v == null) {
        break;
      }
      add0(h, i, name, v);
    }
    return this;
  }
  
  public Headers<T> set(T name, T... values)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    remove0(h, i, name);
    for (T v : values)
    {
      if (v == null) {
        break;
      }
      add0(h, i, name, v);
    }
    return this;
  }
  
  public Headers<T> setObject(T name, Object value)
  {
    return set(name, this.valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")));
  }
  
  public Headers<T> setObject(T name, Iterable<?> values)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    remove0(h, i, name);
    for (Object o : values)
    {
      if (o == null) {
        break;
      }
      T converted = this.valueConverter.convertObject(o);
      ObjectUtil.checkNotNull(converted, "converted");
      add0(h, i, name, converted);
    }
    return this;
  }
  
  public Headers<T> setObject(T name, Object... values)
  {
    name = convertName(name);
    ObjectUtil.checkNotNull(values, "values");
    
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    remove0(h, i, name);
    for (Object o : values)
    {
      if (o == null) {
        break;
      }
      T converted = this.valueConverter.convertObject(o);
      ObjectUtil.checkNotNull(converted, "converted");
      add0(h, i, name, converted);
    }
    return this;
  }
  
  public Headers<T> setInt(T name, int value)
  {
    return set(name, this.valueConverter.convertInt(value));
  }
  
  public Headers<T> setLong(T name, long value)
  {
    return set(name, this.valueConverter.convertLong(value));
  }
  
  public Headers<T> setDouble(T name, double value)
  {
    return set(name, this.valueConverter.convertDouble(value));
  }
  
  public Headers<T> setTimeMillis(T name, long value)
  {
    return set(name, this.valueConverter.convertTimeMillis(value));
  }
  
  public Headers<T> setFloat(T name, float value)
  {
    return set(name, this.valueConverter.convertFloat(value));
  }
  
  public Headers<T> setChar(T name, char value)
  {
    return set(name, this.valueConverter.convertChar(value));
  }
  
  public Headers<T> setBoolean(T name, boolean value)
  {
    return set(name, this.valueConverter.convertBoolean(value));
  }
  
  public Headers<T> setByte(T name, byte value)
  {
    return set(name, this.valueConverter.convertByte(value));
  }
  
  public Headers<T> setShort(T name, short value)
  {
    return set(name, this.valueConverter.convertShort(value));
  }
  
  public Headers<T> set(Headers<T> headers)
  {
    ObjectUtil.checkNotNull(headers, "headers");
    
    clear();
    add0(headers);
    return this;
  }
  
  public Headers<T> setAll(Headers<T> headers)
  {
    ObjectUtil.checkNotNull(headers, "headers");
    if ((headers instanceof DefaultHeaders))
    {
      DefaultHeaders<T> m = (DefaultHeaders)headers;
      DefaultHeaders<T>.HeaderEntry e = m.head.after;
      while (e != m.head)
      {
        set(e.name, e.value);
        e = e.after;
      }
    }
    else
    {
      try
      {
        headers.forEachEntry(setAllVisitor());
      }
      catch (Exception ex)
      {
        PlatformDependent.throwException(ex);
      }
    }
    return this;
  }
  
  public boolean remove(T name)
  {
    ObjectUtil.checkNotNull(name, "name");
    int h = this.hashCodeGenerator.generateHashCode(name);
    int i = index(h);
    return remove0(h, i, name);
  }
  
  public Headers<T> clear()
  {
    this.entries.clear();
    this.tailEntries.clear();
    this.head.before = (this.head.after = this.head);
    this.size = 0;
    return this;
  }
  
  public Iterator<Map.Entry<T, T>> iterator()
  {
    return new KeyValueHeaderIterator();
  }
  
  public Map.Entry<T, T> forEachEntry(Headers.EntryVisitor<T> visitor)
    throws Exception
  {
    DefaultHeaders<T>.HeaderEntry e = this.head.after;
    while (e != this.head)
    {
      if (!visitor.visit(e)) {
        return e;
      }
      e = e.after;
    }
    return null;
  }
  
  public T forEachName(Headers.NameVisitor<T> visitor)
    throws Exception
  {
    DefaultHeaders<T>.HeaderEntry e = this.head.after;
    while (e != this.head)
    {
      if (!visitor.visit(e.name)) {
        return (T)e.name;
      }
      e = e.after;
    }
    return null;
  }
  
  public Boolean getBoolean(T name)
  {
    T v = get(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Boolean.valueOf(this.valueConverter.convertToBoolean(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public boolean getBoolean(T name, boolean defaultValue)
  {
    Boolean v = getBoolean(name);
    return v == null ? defaultValue : v.booleanValue();
  }
  
  public Byte getByte(T name)
  {
    T v = get(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Byte.valueOf(this.valueConverter.convertToByte(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public byte getByte(T name, byte defaultValue)
  {
    Byte v = getByte(name);
    return v == null ? defaultValue : v.byteValue();
  }
  
  public Character getChar(T name)
  {
    T v = get(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Character.valueOf(this.valueConverter.convertToChar(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public char getChar(T name, char defaultValue)
  {
    Character v = getChar(name);
    return v == null ? defaultValue : v.charValue();
  }
  
  public Short getShort(T name)
  {
    T v = get(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Short.valueOf(this.valueConverter.convertToShort(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public short getInt(T name, short defaultValue)
  {
    Short v = getShort(name);
    return v == null ? defaultValue : v.shortValue();
  }
  
  public Integer getInt(T name)
  {
    T v = get(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Integer.valueOf(this.valueConverter.convertToInt(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public int getInt(T name, int defaultValue)
  {
    Integer v = getInt(name);
    return v == null ? defaultValue : v.intValue();
  }
  
  public Long getLong(T name)
  {
    T v = get(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Long.valueOf(this.valueConverter.convertToLong(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public long getLong(T name, long defaultValue)
  {
    Long v = getLong(name);
    return v == null ? defaultValue : v.longValue();
  }
  
  public Float getFloat(T name)
  {
    T v = get(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Float.valueOf(this.valueConverter.convertToFloat(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public float getFloat(T name, float defaultValue)
  {
    Float v = getFloat(name);
    return v == null ? defaultValue : v.floatValue();
  }
  
  public Double getDouble(T name)
  {
    T v = get(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Double.valueOf(this.valueConverter.convertToDouble(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public double getDouble(T name, double defaultValue)
  {
    Double v = getDouble(name);
    return v == null ? defaultValue : v.doubleValue();
  }
  
  public Long getTimeMillis(T name)
  {
    T v = get(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Long.valueOf(this.valueConverter.convertToTimeMillis(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public long getTimeMillis(T name, long defaultValue)
  {
    Long v = getTimeMillis(name);
    return v == null ? defaultValue : v.longValue();
  }
  
  public Boolean getBooleanAndRemove(T name)
  {
    T v = getAndRemove(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Boolean.valueOf(this.valueConverter.convertToBoolean(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public boolean getBooleanAndRemove(T name, boolean defaultValue)
  {
    Boolean v = getBooleanAndRemove(name);
    return v == null ? defaultValue : v.booleanValue();
  }
  
  public Byte getByteAndRemove(T name)
  {
    T v = getAndRemove(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Byte.valueOf(this.valueConverter.convertToByte(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public byte getByteAndRemove(T name, byte defaultValue)
  {
    Byte v = getByteAndRemove(name);
    return v == null ? defaultValue : v.byteValue();
  }
  
  public Character getCharAndRemove(T name)
  {
    T v = getAndRemove(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Character.valueOf(this.valueConverter.convertToChar(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public char getCharAndRemove(T name, char defaultValue)
  {
    Character v = getCharAndRemove(name);
    return v == null ? defaultValue : v.charValue();
  }
  
  public Short getShortAndRemove(T name)
  {
    T v = getAndRemove(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Short.valueOf(this.valueConverter.convertToShort(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public short getShortAndRemove(T name, short defaultValue)
  {
    Short v = getShortAndRemove(name);
    return v == null ? defaultValue : v.shortValue();
  }
  
  public Integer getIntAndRemove(T name)
  {
    T v = getAndRemove(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Integer.valueOf(this.valueConverter.convertToInt(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public int getIntAndRemove(T name, int defaultValue)
  {
    Integer v = getIntAndRemove(name);
    return v == null ? defaultValue : v.intValue();
  }
  
  public Long getLongAndRemove(T name)
  {
    T v = getAndRemove(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Long.valueOf(this.valueConverter.convertToLong(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public long getLongAndRemove(T name, long defaultValue)
  {
    Long v = getLongAndRemove(name);
    return v == null ? defaultValue : v.longValue();
  }
  
  public Float getFloatAndRemove(T name)
  {
    T v = getAndRemove(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Float.valueOf(this.valueConverter.convertToFloat(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public float getFloatAndRemove(T name, float defaultValue)
  {
    Float v = getFloatAndRemove(name);
    return v == null ? defaultValue : v.floatValue();
  }
  
  public Double getDoubleAndRemove(T name)
  {
    T v = getAndRemove(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Double.valueOf(this.valueConverter.convertToDouble(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public double getDoubleAndRemove(T name, double defaultValue)
  {
    Double v = getDoubleAndRemove(name);
    return v == null ? defaultValue : v.doubleValue();
  }
  
  public Long getTimeMillisAndRemove(T name)
  {
    T v = getAndRemove(name);
    if (v == null) {
      return null;
    }
    try
    {
      return Long.valueOf(this.valueConverter.convertToTimeMillis(v));
    }
    catch (Throwable ignored) {}
    return null;
  }
  
  public long getTimeMillisAndRemove(T name, long defaultValue)
  {
    Long v = getTimeMillisAndRemove(name);
    return v == null ? defaultValue : v.longValue();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DefaultHeaders)) {
      return false;
    }
    DefaultHeaders<T> h2 = (DefaultHeaders)o;
    
    List<T> namesList = namesList();
    List<T> otherNamesList = h2.namesList();
    if (!equals(namesList, otherNamesList, this.keyComparator)) {
      return false;
    }
    Set<T> names = new TreeSet(this.keyComparator);
    names.addAll(namesList);
    for (T name : names) {
      if (!equals(getAll(name), h2.getAll(name), this.valueComparator)) {
        return false;
      }
    }
    return true;
  }
  
  private static <T> boolean equals(List<T> lhs, List<T> rhs, Comparator<? super T> comparator)
  {
    int lhsSize = lhs.size();
    if (lhsSize != rhs.size()) {
      return false;
    }
    Collections.sort(lhs, comparator);
    Collections.sort(rhs, comparator);
    for (int i = 0; i < lhsSize; i++) {
      if (comparator.compare(lhs.get(i), rhs.get(i)) != 0) {
        return false;
      }
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = 1;
    for (T name : names())
    {
      result = 31 * result + name.hashCode();
      List<T> values = getAll(name);
      Collections.sort(values, this.valueComparator);
      for (int i = 0; i < values.size(); i++) {
        result = 31 * result + this.hashCodeGenerator.generateHashCode(values.get(i));
      }
    }
    return result;
  }
  
  public String toString()
  {
    StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append('[');
    for (T name : names())
    {
      List<T> values = getAll(name);
      Collections.sort(values, this.valueComparator);
      for (int i = 0; i < values.size(); i++) {
        builder.append(name).append(": ").append(values.get(i)).append(", ");
      }
    }
    if (builder.length() > 2) {
      builder.setLength(builder.length() - 2);
    }
    return ']';
  }
  
  protected Headers.ValueConverter<T> valueConverter()
  {
    return this.valueConverter;
  }
  
  private T convertName(T name)
  {
    return (T)this.nameConverter.convertName(ObjectUtil.checkNotNull(name, "name"));
  }
  
  private int index(int hash)
  {
    return Math.abs(hash % this.bucketSize);
  }
  
  private void add0(Headers<T> headers)
  {
    if (headers.isEmpty()) {
      return;
    }
    if ((headers instanceof DefaultHeaders))
    {
      DefaultHeaders<T> m = (DefaultHeaders)headers;
      DefaultHeaders<T>.HeaderEntry e = m.head.after;
      while (e != m.head)
      {
        add(e.name, e.value);
        e = e.after;
      }
    }
    else
    {
      try
      {
        headers.forEachEntry(addAllVisitor());
      }
      catch (Exception ex)
      {
        PlatformDependent.throwException(ex);
      }
    }
  }
  
  private void add0(int h, int i, T name, T value)
  {
    DefaultHeaders<T>.HeaderEntry newEntry = new HeaderEntry(h, name, value);
    DefaultHeaders<T>.HeaderEntry oldTail = (HeaderEntry)this.tailEntries.get(i);
    if (oldTail == null) {
      this.entries.put(i, newEntry);
    } else {
      oldTail.next = newEntry;
    }
    this.tailEntries.put(i, newEntry);
    
    newEntry.addBefore(this.head);
  }
  
  private boolean remove0(int h, int i, T name)
  {
    DefaultHeaders<T>.HeaderEntry e = (HeaderEntry)this.entries.get(i);
    if (e == null) {
      return false;
    }
    boolean removed = false;
    while ((e.hash == h) && (this.keyComparator.compare(e.name, name) == 0))
    {
      e.remove();
      DefaultHeaders<T>.HeaderEntry next = e.next;
      if (next != null)
      {
        this.entries.put(i, next);
        e = next;
      }
      else
      {
        this.entries.remove(i);
        this.tailEntries.remove(i);
        return true;
      }
      removed = true;
    }
    for (;;)
    {
      DefaultHeaders<T>.HeaderEntry next = e.next;
      if (next == null) {
        break;
      }
      if ((next.hash == h) && (this.keyComparator.compare(next.name, name) == 0))
      {
        e.next = next.next;
        if (e.next == null) {
          this.tailEntries.put(i, e);
        }
        next.remove();
        removed = true;
      }
      else
      {
        e = next;
      }
    }
    return removed;
  }
  
  private Headers.EntryVisitor<T> setAllVisitor()
  {
    new Headers.EntryVisitor()
    {
      public boolean visit(Map.Entry<T, T> entry)
      {
        DefaultHeaders.this.set(entry.getKey(), entry.getValue());
        return true;
      }
    };
  }
  
  private Headers.EntryVisitor<T> addAllVisitor()
  {
    new Headers.EntryVisitor()
    {
      public boolean visit(Map.Entry<T, T> entry)
      {
        DefaultHeaders.this.add(entry.getKey(), entry.getValue());
        return true;
      }
    };
  }
  
  private final class HeaderEntry
    implements Map.Entry<T, T>
  {
    final int hash;
    final T name;
    T value;
    DefaultHeaders<T>.HeaderEntry next;
    DefaultHeaders<T>.HeaderEntry before;
    DefaultHeaders<T>.HeaderEntry after;
    
    HeaderEntry(T hash, T name)
    {
      this.hash = hash;
      this.name = name;
      this.value = value;
    }
    
    HeaderEntry()
    {
      this.hash = -1;
      this.name = null;
      this.value = null;
    }
    
    void remove()
    {
      this.before.after = this.after;
      this.after.before = this.before;
      DefaultHeaders.this.size -= 1;
    }
    
    void addBefore(DefaultHeaders<T>.HeaderEntry e)
    {
      this.after = e;
      this.before = e.before;
      this.before.after = this;
      this.after.before = this;
      DefaultHeaders.this.size += 1;
    }
    
    public T getKey()
    {
      return (T)this.name;
    }
    
    public T getValue()
    {
      return (T)this.value;
    }
    
    public T setValue(T value)
    {
      ObjectUtil.checkNotNull(value, "value");
      T oldValue = this.value;
      this.value = value;
      return oldValue;
    }
    
    public String toString()
    {
      return this.name + '=' + this.value;
    }
  }
  
  protected final class KeyValueHeaderIterator
    implements Iterator<Map.Entry<T, T>>
  {
    private DefaultHeaders<T>.HeaderEntry current = DefaultHeaders.this.head;
    
    protected KeyValueHeaderIterator() {}
    
    public boolean hasNext()
    {
      return this.current.after != DefaultHeaders.this.head;
    }
    
    public Map.Entry<T, T> next()
    {
      this.current = this.current.after;
      if (this.current == DefaultHeaders.this.head) {
        throw new NoSuchElementException();
      }
      return this.current;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  static final class HeaderDateFormat
  {
    private static final ParsePosition parsePos = new ParsePosition(0);
    private static final FastThreadLocal<HeaderDateFormat> dateFormatThreadLocal = new FastThreadLocal()
    {
      protected DefaultHeaders.HeaderDateFormat initialValue()
      {
        return new DefaultHeaders.HeaderDateFormat(null);
      }
    };
    
    static HeaderDateFormat get()
    {
      return (HeaderDateFormat)dateFormatThreadLocal.get();
    }
    
    private final DateFormat dateFormat1 = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
    private final DateFormat dateFormat2 = new SimpleDateFormat("E, dd-MMM-yy HH:mm:ss z", Locale.ENGLISH);
    private final DateFormat dateFormat3 = new SimpleDateFormat("E MMM d HH:mm:ss yyyy", Locale.ENGLISH);
    
    private HeaderDateFormat()
    {
      TimeZone tz = TimeZone.getTimeZone("GMT");
      this.dateFormat1.setTimeZone(tz);
      this.dateFormat2.setTimeZone(tz);
      this.dateFormat3.setTimeZone(tz);
    }
    
    long parse(String text)
      throws ParseException
    {
      Date date = this.dateFormat1.parse(text, parsePos);
      if (date == null) {
        date = this.dateFormat2.parse(text, parsePos);
      }
      if (date == null) {
        date = this.dateFormat3.parse(text, parsePos);
      }
      if (date == null) {
        throw new ParseException(text, 0);
      }
      return date.getTime();
    }
    
    long parse(String text, long defaultValue)
    {
      Date date = this.dateFormat1.parse(text, parsePos);
      if (date == null) {
        date = this.dateFormat2.parse(text, parsePos);
      }
      if (date == null) {
        date = this.dateFormat3.parse(text, parsePos);
      }
      if (date == null) {
        return defaultValue;
      }
      return date.getTime();
    }
  }
}
