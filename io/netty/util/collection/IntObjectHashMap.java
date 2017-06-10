package io.netty.util.collection;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class IntObjectHashMap<V>
  implements IntObjectMap<V>, Iterable<IntObjectMap.Entry<V>>
{
  private static final int DEFAULT_CAPACITY = 11;
  private static final float DEFAULT_LOAD_FACTOR = 0.5F;
  private static final Object NULL_VALUE = new Object();
  private int maxSize;
  private final float loadFactor;
  private int[] keys;
  private V[] values;
  private Collection<V> valueCollection;
  private int size;
  
  public IntObjectHashMap()
  {
    this(11, 0.5F);
  }
  
  public IntObjectHashMap(int initialCapacity)
  {
    this(initialCapacity, 0.5F);
  }
  
  public IntObjectHashMap(int initialCapacity, float loadFactor)
  {
    if (initialCapacity < 1) {
      throw new IllegalArgumentException("initialCapacity must be >= 1");
    }
    if ((loadFactor <= 0.0F) || (loadFactor > 1.0F)) {
      throw new IllegalArgumentException("loadFactor must be > 0 and <= 1");
    }
    this.loadFactor = loadFactor;
    
    int capacity = adjustCapacity(initialCapacity);
    
    this.keys = new int[capacity];
    
    V[] temp = (Object[])new Object[capacity];
    this.values = temp;
    
    this.maxSize = calcMaxSize(capacity);
  }
  
  private static <T> T toExternal(T value)
  {
    return value == NULL_VALUE ? null : value;
  }
  
  private static <T> T toInternal(T value)
  {
    return (T)(value == null ? NULL_VALUE : value);
  }
  
  public V get(int key)
  {
    int index = indexOf(key);
    return index == -1 ? null : toExternal(this.values[index]);
  }
  
  public V put(int key, V value)
  {
    int startIndex = hashIndex(key);
    int index = startIndex;
    do
    {
      if (this.values[index] == null)
      {
        this.keys[index] = key;
        this.values[index] = toInternal(value);
        growSize();
        return null;
      }
      if (this.keys[index] == key)
      {
        V previousValue = this.values[index];
        this.values[index] = toInternal(value);
        return (V)toExternal(previousValue);
      }
    } while ((index = probeNext(index)) != startIndex);
    throw new IllegalStateException("Unable to insert");
  }
  
  private int probeNext(int index)
  {
    return index == this.values.length - 1 ? 0 : index + 1;
  }
  
  public void putAll(IntObjectMap<V> sourceMap)
  {
    if ((sourceMap instanceof IntObjectHashMap))
    {
      IntObjectHashMap<V> source = (IntObjectHashMap)sourceMap;
      for (int i = 0; i < source.values.length; i++)
      {
        V sourceValue = source.values[i];
        if (sourceValue != null) {
          put(source.keys[i], sourceValue);
        }
      }
      return;
    }
    for (IntObjectMap.Entry<V> entry : sourceMap.entries()) {
      put(entry.key(), entry.value());
    }
  }
  
  public V remove(int key)
  {
    int index = indexOf(key);
    if (index == -1) {
      return null;
    }
    V prev = this.values[index];
    removeAt(index);
    return (V)toExternal(prev);
  }
  
  public int size()
  {
    return this.size;
  }
  
  public boolean isEmpty()
  {
    return this.size == 0;
  }
  
  public void clear()
  {
    Arrays.fill(this.keys, 0);
    Arrays.fill(this.values, null);
    this.size = 0;
  }
  
  public boolean containsKey(int key)
  {
    return indexOf(key) >= 0;
  }
  
  public boolean containsValue(V value)
  {
    V v1 = toInternal(value);
    for (V v2 : this.values) {
      if ((v2 != null) && (v2.equals(v1))) {
        return true;
      }
    }
    return false;
  }
  
  public Iterable<IntObjectMap.Entry<V>> entries()
  {
    return this;
  }
  
  public Iterator<IntObjectMap.Entry<V>> iterator()
  {
    return new IteratorImpl(null);
  }
  
  public int[] keys()
  {
    int[] outKeys = new int[size()];
    int targetIx = 0;
    for (int i = 0; i < this.values.length; i++) {
      if (this.values[i] != null) {
        outKeys[(targetIx++)] = this.keys[i];
      }
    }
    return outKeys;
  }
  
  public V[] values(Class<V> clazz)
  {
    V[] outValues = (Object[])Array.newInstance(clazz, size());
    int targetIx = 0;
    for (V value : this.values) {
      if (value != null) {
        outValues[(targetIx++)] = value;
      }
    }
    return outValues;
  }
  
  public Collection<V> values()
  {
    Collection<V> valueCollection = this.valueCollection;
    if (valueCollection == null) {
      this.valueCollection = ( = new AbstractCollection()
      {
        public Iterator<V> iterator()
        {
          new Iterator()
          {
            final Iterator<IntObjectMap.Entry<V>> iter = IntObjectHashMap.this.iterator();
            
            public boolean hasNext()
            {
              return this.iter.hasNext();
            }
            
            public V next()
            {
              return (V)((IntObjectMap.Entry)this.iter.next()).value();
            }
            
            public void remove()
            {
              throw new UnsupportedOperationException();
            }
          };
        }
        
        public int size()
        {
          return IntObjectHashMap.this.size;
        }
      });
    }
    return valueCollection;
  }
  
  public int hashCode()
  {
    int hash = this.size;
    for (int key : this.keys) {
      hash ^= key;
    }
    return hash;
  }
  
  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof IntObjectMap)) {
      return false;
    }
    IntObjectMap other = (IntObjectMap)obj;
    if (this.size != other.size()) {
      return false;
    }
    for (int i = 0; i < this.values.length; i++)
    {
      V value = this.values[i];
      if (value != null)
      {
        int key = this.keys[i];
        Object otherValue = other.get(key);
        if (value == NULL_VALUE)
        {
          if (otherValue != null) {
            return false;
          }
        }
        else if (!value.equals(otherValue)) {
          return false;
        }
      }
    }
    return true;
  }
  
  private int indexOf(int key)
  {
    int startIndex = hashIndex(key);
    int index = startIndex;
    do
    {
      if (this.values[index] == null) {
        return -1;
      }
      if (key == this.keys[index]) {
        return index;
      }
    } while ((index = probeNext(index)) != startIndex);
    return -1;
  }
  
  private int hashIndex(int key)
  {
    return (key % this.keys.length + this.keys.length) % this.keys.length;
  }
  
  private void growSize()
  {
    this.size += 1;
    if (this.size > this.maxSize) {
      rehash(adjustCapacity((int)Math.min(this.keys.length * 2.0D, 2.147483639E9D)));
    } else if (this.size == this.keys.length) {
      rehash(this.keys.length);
    }
  }
  
  private static int adjustCapacity(int capacity)
  {
    return capacity | 0x1;
  }
  
  private void removeAt(int index)
  {
    this.size -= 1;
    
    this.keys[index] = 0;
    this.values[index] = null;
    
    int nextFree = index;
    for (int i = probeNext(index); this.values[i] != null; i = probeNext(i))
    {
      int bucket = hashIndex(this.keys[i]);
      if (((i < bucket) && ((bucket <= nextFree) || (nextFree <= i))) || ((bucket <= nextFree) && (nextFree <= i)))
      {
        this.keys[nextFree] = this.keys[i];
        this.values[nextFree] = this.values[i];
        
        this.keys[i] = 0;
        this.values[i] = null;
        nextFree = i;
      }
    }
  }
  
  private int calcMaxSize(int capacity)
  {
    int upperBound = capacity - 1;
    return Math.min(upperBound, (int)(capacity * this.loadFactor));
  }
  
  private void rehash(int newCapacity)
  {
    int[] oldKeys = this.keys;
    V[] oldVals = this.values;
    
    this.keys = new int[newCapacity];
    
    V[] temp = (Object[])new Object[newCapacity];
    this.values = temp;
    
    this.maxSize = calcMaxSize(newCapacity);
    for (int i = 0; i < oldVals.length; i++)
    {
      V oldVal = oldVals[i];
      if (oldVal != null)
      {
        int oldKey = oldKeys[i];
        int index = hashIndex(oldKey);
        for (;;)
        {
          if (this.values[index] == null)
          {
            this.keys[index] = oldKey;
            this.values[index] = toInternal(oldVal);
            break;
          }
          index = probeNext(index);
        }
      }
    }
  }
  
  private final class IteratorImpl
    implements Iterator<IntObjectMap.Entry<V>>, IntObjectMap.Entry<V>
  {
    private int prevIndex = -1;
    private int nextIndex = -1;
    private int entryIndex = -1;
    
    private IteratorImpl() {}
    
    private void scanNext()
    {
      while (++this.nextIndex != IntObjectHashMap.this.values.length) {
        if (IntObjectHashMap.this.values[this.nextIndex] != null) {
          break;
        }
      }
    }
    
    public boolean hasNext()
    {
      if (this.nextIndex == -1) {
        scanNext();
      }
      return this.nextIndex < IntObjectHashMap.this.keys.length;
    }
    
    public IntObjectMap.Entry<V> next()
    {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      this.prevIndex = this.nextIndex;
      scanNext();
      
      this.entryIndex = this.prevIndex;
      return this;
    }
    
    public void remove()
    {
      if (this.prevIndex < 0) {
        throw new IllegalStateException("next must be called before each remove.");
      }
      IntObjectHashMap.this.removeAt(this.prevIndex);
      this.prevIndex = -1;
    }
    
    public int key()
    {
      return IntObjectHashMap.this.keys[this.entryIndex];
    }
    
    public V value()
    {
      return (V)IntObjectHashMap.toExternal(IntObjectHashMap.this.values[this.entryIndex]);
    }
    
    public void setValue(V value)
    {
      IntObjectHashMap.this.values[this.entryIndex] = IntObjectHashMap.toInternal(value);
    }
  }
  
  public String toString()
  {
    if (this.size == 0) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder(4 * this.size);
    for (int i = 0; i < this.values.length; i++)
    {
      V value = this.values[i];
      if (value != null)
      {
        sb.append(sb.length() == 0 ? "{" : ", ");
        sb.append(keyToString(this.keys[i])).append('=').append(value == this ? "(this Map)" : value);
      }
    }
    return '}';
  }
  
  protected String keyToString(int key)
  {
    return Integer.toString(key);
  }
}
