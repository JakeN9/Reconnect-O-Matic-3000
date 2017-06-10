package io.netty.util.collection;

import io.netty.util.internal.EmptyArrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class PrimitiveCollections
{
  private static final IntObjectMap<Object> EMPTY_INT_OBJECT_MAP = new EmptyIntObjectMap(null);
  
  public static <V> IntObjectMap<V> emptyIntObjectMap()
  {
    return EMPTY_INT_OBJECT_MAP;
  }
  
  public static <V> IntObjectMap<V> unmodifiableIntObjectMap(IntObjectMap<V> map)
  {
    return new UnmodifiableIntObjectMap(map);
  }
  
  private static final class EmptyIntObjectMap
    implements IntObjectMap<Object>
  {
    public Object get(int key)
    {
      return null;
    }
    
    public Object put(int key, Object value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public void putAll(IntObjectMap<Object> sourceMap)
    {
      throw new UnsupportedOperationException("putAll");
    }
    
    public Object remove(int key)
    {
      throw new UnsupportedOperationException("remove");
    }
    
    public int size()
    {
      return 0;
    }
    
    public boolean isEmpty()
    {
      return true;
    }
    
    public void clear() {}
    
    public boolean containsKey(int key)
    {
      return false;
    }
    
    public boolean containsValue(Object value)
    {
      return false;
    }
    
    public Iterable<IntObjectMap.Entry<Object>> entries()
    {
      return Collections.emptySet();
    }
    
    public int[] keys()
    {
      return EmptyArrays.EMPTY_INTS;
    }
    
    public Object[] values(Class<Object> clazz)
    {
      return EmptyArrays.EMPTY_OBJECTS;
    }
    
    public Collection<Object> values()
    {
      return Collections.emptyList();
    }
  }
  
  private static final class UnmodifiableIntObjectMap<V>
    implements IntObjectMap<V>, Iterable<IntObjectMap.Entry<V>>
  {
    final IntObjectMap<V> map;
    
    UnmodifiableIntObjectMap(IntObjectMap<V> map)
    {
      this.map = map;
    }
    
    public V get(int key)
    {
      return (V)this.map.get(key);
    }
    
    public V put(int key, V value)
    {
      throw new UnsupportedOperationException("put");
    }
    
    public void putAll(IntObjectMap<V> sourceMap)
    {
      throw new UnsupportedOperationException("putAll");
    }
    
    public V remove(int key)
    {
      throw new UnsupportedOperationException("remove");
    }
    
    public int size()
    {
      return this.map.size();
    }
    
    public boolean isEmpty()
    {
      return this.map.isEmpty();
    }
    
    public void clear()
    {
      throw new UnsupportedOperationException("clear");
    }
    
    public boolean containsKey(int key)
    {
      return this.map.containsKey(key);
    }
    
    public boolean containsValue(V value)
    {
      return this.map.containsValue(value);
    }
    
    public Iterable<IntObjectMap.Entry<V>> entries()
    {
      return this;
    }
    
    public Iterator<IntObjectMap.Entry<V>> iterator()
    {
      return new IteratorImpl(this.map.entries().iterator());
    }
    
    public int[] keys()
    {
      return this.map.keys();
    }
    
    public V[] values(Class<V> clazz)
    {
      return this.map.values(clazz);
    }
    
    public Collection<V> values()
    {
      return this.map.values();
    }
    
    private class IteratorImpl
      implements Iterator<IntObjectMap.Entry<V>>
    {
      final Iterator<IntObjectMap.Entry<V>> iter;
      
      IteratorImpl()
      {
        this.iter = iter;
      }
      
      public boolean hasNext()
      {
        return this.iter.hasNext();
      }
      
      public IntObjectMap.Entry<V> next()
      {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return new PrimitiveCollections.UnmodifiableIntObjectMap.EntryImpl(PrimitiveCollections.UnmodifiableIntObjectMap.this, (IntObjectMap.Entry)this.iter.next());
      }
      
      public void remove()
      {
        throw new UnsupportedOperationException("remove");
      }
    }
    
    private class EntryImpl
      implements IntObjectMap.Entry<V>
    {
      final IntObjectMap.Entry<V> entry;
      
      EntryImpl()
      {
        this.entry = entry;
      }
      
      public int key()
      {
        return this.entry.key();
      }
      
      public V value()
      {
        return (V)this.entry.value();
      }
      
      public void setValue(V value)
      {
        throw new UnsupportedOperationException("setValue");
      }
    }
  }
}
