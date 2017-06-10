package io.netty.util.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

final class MpscLinkedQueue<E>
  extends MpscLinkedQueueTailRef<E>
  implements Queue<E>
{
  private static final long serialVersionUID = -1878402552271506449L;
  long p00;
  long p01;
  long p02;
  long p03;
  long p04;
  long p05;
  long p06;
  long p07;
  long p30;
  long p31;
  long p32;
  long p33;
  long p34;
  long p35;
  long p36;
  long p37;
  
  MpscLinkedQueue()
  {
    MpscLinkedQueueNode<E> tombstone = new DefaultNode(null);
    setHeadRef(tombstone);
    setTailRef(tombstone);
  }
  
  private MpscLinkedQueueNode<E> peekNode()
  {
    MpscLinkedQueueNode<E> head = headRef();
    MpscLinkedQueueNode<E> next = head.next();
    if ((next == null) && (head != tailRef())) {
      do
      {
        next = head.next();
      } while (next == null);
    }
    return next;
  }
  
  public boolean offer(E value)
  {
    if (value == null) {
      throw new NullPointerException("value");
    }
    MpscLinkedQueueNode<E> newTail;
    if ((value instanceof MpscLinkedQueueNode))
    {
      MpscLinkedQueueNode<E> newTail = (MpscLinkedQueueNode)value;
      newTail.setNext(null);
    }
    else
    {
      newTail = new DefaultNode(value);
    }
    MpscLinkedQueueNode<E> oldTail = getAndSetTailRef(newTail);
    oldTail.setNext(newTail);
    return true;
  }
  
  public E poll()
  {
    MpscLinkedQueueNode<E> next = peekNode();
    if (next == null) {
      return null;
    }
    MpscLinkedQueueNode<E> oldHead = headRef();
    
    lazySetHeadRef(next);
    
    oldHead.unlink();
    
    return (E)next.clearMaybe();
  }
  
  public E peek()
  {
    MpscLinkedQueueNode<E> next = peekNode();
    if (next == null) {
      return null;
    }
    return (E)next.value();
  }
  
  public int size()
  {
    int count = 0;
    MpscLinkedQueueNode<E> n = peekNode();
    while (n != null)
    {
      count++;
      n = n.next();
    }
    return count;
  }
  
  public boolean isEmpty()
  {
    return peekNode() == null;
  }
  
  public boolean contains(Object o)
  {
    MpscLinkedQueueNode<E> n = peekNode();
    while (n != null)
    {
      if (n.value() == o) {
        return true;
      }
      n = n.next();
    }
    return false;
  }
  
  public Iterator<E> iterator()
  {
    new Iterator()
    {
      private MpscLinkedQueueNode<E> node = MpscLinkedQueue.this.peekNode();
      
      public boolean hasNext()
      {
        return this.node != null;
      }
      
      public E next()
      {
        MpscLinkedQueueNode<E> node = this.node;
        if (node == null) {
          throw new NoSuchElementException();
        }
        E value = node.value();
        this.node = node.next();
        return value;
      }
      
      public void remove()
      {
        throw new UnsupportedOperationException();
      }
    };
  }
  
  public boolean add(E e)
  {
    if (offer(e)) {
      return true;
    }
    throw new IllegalStateException("queue full");
  }
  
  public E remove()
  {
    E e = poll();
    if (e != null) {
      return e;
    }
    throw new NoSuchElementException();
  }
  
  public E element()
  {
    E e = peek();
    if (e != null) {
      return e;
    }
    throw new NoSuchElementException();
  }
  
  public Object[] toArray()
  {
    Object[] array = new Object[size()];
    Iterator<E> it = iterator();
    for (int i = 0; i < array.length; i++) {
      if (it.hasNext()) {
        array[i] = it.next();
      } else {
        return Arrays.copyOf(array, i);
      }
    }
    return array;
  }
  
  public <T> T[] toArray(T[] a)
  {
    int size = size();
    T[] array;
    T[] array;
    if (a.length >= size) {
      array = a;
    } else {
      array = (Object[])Array.newInstance(a.getClass().getComponentType(), size);
    }
    Iterator<E> it = iterator();
    for (int i = 0; i < array.length; i++) {
      if (it.hasNext())
      {
        array[i] = it.next();
      }
      else
      {
        if (a == array)
        {
          array[i] = null;
          return array;
        }
        if (a.length < i) {
          return Arrays.copyOf(array, i);
        }
        System.arraycopy(array, 0, a, 0, i);
        if (a.length > i) {
          a[i] = null;
        }
        return a;
      }
    }
    return array;
  }
  
  public boolean remove(Object o)
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean containsAll(Collection<?> c)
  {
    for (Object e : c) {
      if (!contains(e)) {
        return false;
      }
    }
    return true;
  }
  
  public boolean addAll(Collection<? extends E> c)
  {
    if (c == null) {
      throw new NullPointerException("c");
    }
    if (c == this) {
      throw new IllegalArgumentException("c == this");
    }
    boolean modified = false;
    for (E e : c)
    {
      add(e);
      modified = true;
    }
    return modified;
  }
  
  public boolean removeAll(Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }
  
  public boolean retainAll(Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }
  
  public void clear()
  {
    while (poll() != null) {}
  }
  
  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.defaultWriteObject();
    for (E e : this) {
      out.writeObject(e);
    }
    out.writeObject(null);
  }
  
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException
  {
    in.defaultReadObject();
    
    MpscLinkedQueueNode<E> tombstone = new DefaultNode(null);
    setHeadRef(tombstone);
    setTailRef(tombstone);
    for (;;)
    {
      E e = in.readObject();
      if (e == null) {
        break;
      }
      add(e);
    }
  }
  
  private static final class DefaultNode<T>
    extends MpscLinkedQueueNode<T>
  {
    private T value;
    
    DefaultNode(T value)
    {
      this.value = value;
    }
    
    public T value()
    {
      return (T)this.value;
    }
    
    protected T clearMaybe()
    {
      T value = this.value;
      this.value = null;
      return value;
    }
  }
}
