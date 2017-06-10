package io.netty.util.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import sun.misc.Unsafe;

final class UnsafeAtomicReferenceFieldUpdater<U, M>
  extends AtomicReferenceFieldUpdater<U, M>
{
  private final long offset;
  private final Unsafe unsafe;
  
  UnsafeAtomicReferenceFieldUpdater(Unsafe unsafe, Class<U> tClass, String fieldName)
    throws NoSuchFieldException
  {
    Field field = tClass.getDeclaredField(fieldName);
    if (!Modifier.isVolatile(field.getModifiers())) {
      throw new IllegalArgumentException("Must be volatile");
    }
    this.unsafe = unsafe;
    this.offset = unsafe.objectFieldOffset(field);
  }
  
  public boolean compareAndSet(U obj, M expect, M update)
  {
    return this.unsafe.compareAndSwapObject(obj, this.offset, expect, update);
  }
  
  public boolean weakCompareAndSet(U obj, M expect, M update)
  {
    return this.unsafe.compareAndSwapObject(obj, this.offset, expect, update);
  }
  
  public void set(U obj, M newValue)
  {
    this.unsafe.putObjectVolatile(obj, this.offset, newValue);
  }
  
  public void lazySet(U obj, M newValue)
  {
    this.unsafe.putOrderedObject(obj, this.offset, newValue);
  }
  
  public M get(U obj)
  {
    return (M)this.unsafe.getObjectVolatile(obj, this.offset);
  }
}
