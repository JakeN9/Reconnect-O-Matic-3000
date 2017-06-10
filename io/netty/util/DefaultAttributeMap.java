package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class DefaultAttributeMap
  implements AttributeMap
{
  private static final AtomicReferenceFieldUpdater<DefaultAttributeMap, AtomicReferenceArray> updater;
  private static final int BUCKET_SIZE = 4;
  private static final int MASK = 3;
  private volatile AtomicReferenceArray<DefaultAttribute<?>> attributes;
  
  static
  {
    AtomicReferenceFieldUpdater<DefaultAttributeMap, AtomicReferenceArray> referenceFieldUpdater = PlatformDependent.newAtomicReferenceFieldUpdater(DefaultAttributeMap.class, "attributes");
    if (referenceFieldUpdater == null) {
      referenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(DefaultAttributeMap.class, AtomicReferenceArray.class, "attributes");
    }
    updater = referenceFieldUpdater;
  }
  
  public <T> Attribute<T> attr(AttributeKey<T> key)
  {
    if (key == null) {
      throw new NullPointerException("key");
    }
    AtomicReferenceArray<DefaultAttribute<?>> attributes = this.attributes;
    if (attributes == null)
    {
      attributes = new AtomicReferenceArray(4);
      if (!updater.compareAndSet(this, null, attributes)) {
        attributes = this.attributes;
      }
    }
    int i = index(key);
    DefaultAttribute<?> head = (DefaultAttribute)attributes.get(i);
    if (head == null)
    {
      head = new DefaultAttribute(key);
      if (attributes.compareAndSet(i, null, head)) {
        return head;
      }
      head = (DefaultAttribute)attributes.get(i);
    }
    synchronized (head)
    {
      DefaultAttribute<?> curr = head;
      if ((!curr.removed) && (curr.key == key)) {
        return curr;
      }
      DefaultAttribute<?> next = curr.next;
      if (next == null)
      {
        DefaultAttribute<T> attr = new DefaultAttribute(head, key);
        curr.next = attr;
        attr.prev = curr;
        return attr;
      }
      curr = next;
    }
  }
  
  public <T> boolean hasAttr(AttributeKey<T> key)
  {
    if (key == null) {
      throw new NullPointerException("key");
    }
    AtomicReferenceArray<DefaultAttribute<?>> attributes = this.attributes;
    if (attributes == null) {
      return false;
    }
    int i = index(key);
    DefaultAttribute<?> head = (DefaultAttribute)attributes.get(i);
    if (head == null) {
      return false;
    }
    if ((head.key == key) && (!head.removed)) {
      return true;
    }
    synchronized (head)
    {
      DefaultAttribute<?> curr = head.next;
      while (curr != null)
      {
        if ((!curr.removed) && (curr.key == key)) {
          return true;
        }
        curr = curr.next;
      }
      return false;
    }
  }
  
  private static int index(AttributeKey<?> key)
  {
    return key.id() & 0x3;
  }
  
  private static final class DefaultAttribute<T>
    extends AtomicReference<T>
    implements Attribute<T>
  {
    private static final long serialVersionUID = -2661411462200283011L;
    private final DefaultAttribute<?> head;
    private final AttributeKey<T> key;
    private DefaultAttribute<?> prev;
    private DefaultAttribute<?> next;
    private volatile boolean removed;
    
    DefaultAttribute(DefaultAttribute<?> head, AttributeKey<T> key)
    {
      this.head = head;
      this.key = key;
    }
    
    DefaultAttribute(AttributeKey<T> key)
    {
      this.head = this;
      this.key = key;
    }
    
    public AttributeKey<T> key()
    {
      return this.key;
    }
    
    public T setIfAbsent(T value)
    {
      while (!compareAndSet(null, value))
      {
        T old = get();
        if (old != null) {
          return old;
        }
      }
      return null;
    }
    
    public T getAndRemove()
    {
      this.removed = true;
      T oldValue = getAndSet(null);
      remove0();
      return oldValue;
    }
    
    public void remove()
    {
      this.removed = true;
      set(null);
      remove0();
    }
    
    private void remove0()
    {
      synchronized (this.head)
      {
        if (this.prev != null)
        {
          this.prev.next = this.next;
          if (this.next != null) {
            this.next.prev = this.prev;
          }
        }
      }
    }
  }
}
