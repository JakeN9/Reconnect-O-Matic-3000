package io.netty.util.concurrent;

import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.PlatformDependent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class FastThreadLocal<V>
{
  private static final int variablesToRemoveIndex = ;
  private final int index;
  
  public static void removeAll()
  {
    InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
    if (threadLocalMap == null) {
      return;
    }
    try
    {
      Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
      if ((v != null) && (v != InternalThreadLocalMap.UNSET))
      {
        Set<FastThreadLocal<?>> variablesToRemove = (Set)v;
        FastThreadLocal<?>[] variablesToRemoveArray = (FastThreadLocal[])variablesToRemove.toArray(new FastThreadLocal[variablesToRemove.size()]);
        for (FastThreadLocal<?> tlv : variablesToRemoveArray) {
          tlv.remove(threadLocalMap);
        }
      }
    }
    finally
    {
      InternalThreadLocalMap.remove();
    }
  }
  
  public static int size()
  {
    InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
    if (threadLocalMap == null) {
      return 0;
    }
    return threadLocalMap.size();
  }
  
  public static void destroy() {}
  
  private static void addToVariablesToRemove(InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable)
  {
    Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
    Set<FastThreadLocal<?>> variablesToRemove;
    if ((v == InternalThreadLocalMap.UNSET) || (v == null))
    {
      Set<FastThreadLocal<?>> variablesToRemove = Collections.newSetFromMap(new IdentityHashMap());
      threadLocalMap.setIndexedVariable(variablesToRemoveIndex, variablesToRemove);
    }
    else
    {
      variablesToRemove = (Set)v;
    }
    variablesToRemove.add(variable);
  }
  
  private static void removeFromVariablesToRemove(InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable)
  {
    Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
    if ((v == InternalThreadLocalMap.UNSET) || (v == null)) {
      return;
    }
    Set<FastThreadLocal<?>> variablesToRemove = (Set)v;
    variablesToRemove.remove(variable);
  }
  
  public FastThreadLocal()
  {
    this.index = InternalThreadLocalMap.nextVariableIndex();
  }
  
  public final V get()
  {
    return (V)get(InternalThreadLocalMap.get());
  }
  
  public final V get(InternalThreadLocalMap threadLocalMap)
  {
    Object v = threadLocalMap.indexedVariable(this.index);
    if (v != InternalThreadLocalMap.UNSET) {
      return (V)v;
    }
    return (V)initialize(threadLocalMap);
  }
  
  private V initialize(InternalThreadLocalMap threadLocalMap)
  {
    V v = null;
    try
    {
      v = initialValue();
    }
    catch (Exception e)
    {
      PlatformDependent.throwException(e);
    }
    threadLocalMap.setIndexedVariable(this.index, v);
    addToVariablesToRemove(threadLocalMap, this);
    return v;
  }
  
  public final void set(V value)
  {
    if (value != InternalThreadLocalMap.UNSET) {
      set(InternalThreadLocalMap.get(), value);
    } else {
      remove();
    }
  }
  
  public final void set(InternalThreadLocalMap threadLocalMap, V value)
  {
    if (value != InternalThreadLocalMap.UNSET)
    {
      if (threadLocalMap.setIndexedVariable(this.index, value)) {
        addToVariablesToRemove(threadLocalMap, this);
      }
    }
    else {
      remove(threadLocalMap);
    }
  }
  
  public final boolean isSet()
  {
    return isSet(InternalThreadLocalMap.getIfSet());
  }
  
  public final boolean isSet(InternalThreadLocalMap threadLocalMap)
  {
    return (threadLocalMap != null) && (threadLocalMap.isIndexedVariableSet(this.index));
  }
  
  public final void remove()
  {
    remove(InternalThreadLocalMap.getIfSet());
  }
  
  public final void remove(InternalThreadLocalMap threadLocalMap)
  {
    if (threadLocalMap == null) {
      return;
    }
    Object v = threadLocalMap.removeIndexedVariable(this.index);
    removeFromVariablesToRemove(threadLocalMap, this);
    if (v != InternalThreadLocalMap.UNSET) {
      try
      {
        onRemoval(v);
      }
      catch (Exception e)
      {
        PlatformDependent.throwException(e);
      }
    }
  }
  
  protected V initialValue()
    throws Exception
  {
    return null;
  }
  
  protected void onRemoval(V value)
    throws Exception
  {}
}
