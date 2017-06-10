package io.netty.util;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Recycler<T>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(Recycler.class);
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger(Integer.MIN_VALUE);
  private static final int OWN_THREAD_ID = ID_GENERATOR.getAndIncrement();
  private static final int DEFAULT_MAX_CAPACITY;
  
  static
  {
    int maxCapacity = SystemPropertyUtil.getInt("io.netty.recycler.maxCapacity", 0);
    if (maxCapacity <= 0) {
      maxCapacity = 262144;
    }
    DEFAULT_MAX_CAPACITY = maxCapacity;
    if (logger.isDebugEnabled()) {
      logger.debug("-Dio.netty.recycler.maxCapacity: {}", Integer.valueOf(DEFAULT_MAX_CAPACITY));
    }
  }
  
  private static final int INITIAL_CAPACITY = Math.min(DEFAULT_MAX_CAPACITY, 256);
  private final int maxCapacity;
  private final FastThreadLocal<Stack<T>> threadLocal = new FastThreadLocal()
  {
    protected Recycler.Stack<T> initialValue()
    {
      return new Recycler.Stack(Recycler.this, Thread.currentThread(), Recycler.this.maxCapacity);
    }
  };
  
  protected Recycler()
  {
    this(DEFAULT_MAX_CAPACITY);
  }
  
  protected Recycler(int maxCapacity)
  {
    this.maxCapacity = Math.max(0, maxCapacity);
  }
  
  public final T get()
  {
    Stack<T> stack = (Stack)this.threadLocal.get();
    DefaultHandle<T> handle = stack.pop();
    if (handle == null)
    {
      handle = stack.newHandle();
      handle.value = newObject(handle);
    }
    return (T)handle.value;
  }
  
  public final boolean recycle(T o, Handle<T> handle)
  {
    DefaultHandle<T> h = (DefaultHandle)handle;
    if (h.stack.parent != this) {
      return false;
    }
    h.recycle(o);
    return true;
  }
  
  final int threadLocalCapacity()
  {
    return ((Stack)this.threadLocal.get()).elements.length;
  }
  
  final int threadLocalSize()
  {
    return ((Stack)this.threadLocal.get()).size;
  }
  
  protected abstract T newObject(Handle<T> paramHandle);
  
  public static abstract interface Handle<T>
  {
    public abstract void recycle(T paramT);
  }
  
  static final class DefaultHandle<T>
    implements Recycler.Handle<T>
  {
    private int lastRecycledId;
    private int recycleId;
    private Recycler.Stack<?> stack;
    private Object value;
    
    DefaultHandle(Recycler.Stack<?> stack)
    {
      this.stack = stack;
    }
    
    public void recycle(Object object)
    {
      if (object != this.value) {
        throw new IllegalArgumentException("object does not belong to handle");
      }
      Thread thread = Thread.currentThread();
      if (thread == this.stack.thread)
      {
        this.stack.push(this);
        return;
      }
      Map<Recycler.Stack<?>, Recycler.WeakOrderQueue> delayedRecycled = (Map)Recycler.DELAYED_RECYCLED.get();
      Recycler.WeakOrderQueue queue = (Recycler.WeakOrderQueue)delayedRecycled.get(this.stack);
      if (queue == null) {
        delayedRecycled.put(this.stack, queue = new Recycler.WeakOrderQueue(this.stack, thread));
      }
      queue.add(this);
    }
  }
  
  private static final FastThreadLocal<Map<Stack<?>, WeakOrderQueue>> DELAYED_RECYCLED = new FastThreadLocal()
  {
    protected Map<Recycler.Stack<?>, Recycler.WeakOrderQueue> initialValue()
    {
      return new WeakHashMap();
    }
  };
  
  private static final class WeakOrderQueue
  {
    private static final int LINK_CAPACITY = 16;
    private Link head;
    private Link tail;
    private WeakOrderQueue next;
    private final WeakReference<Thread> owner;
    
    private static final class Link
      extends AtomicInteger
    {
      private final Recycler.DefaultHandle<?>[] elements = new Recycler.DefaultHandle[16];
      private int readIndex;
      private Link next;
    }
    
    private final int id = Recycler.ID_GENERATOR.getAndIncrement();
    
    WeakOrderQueue(Recycler.Stack<?> stack, Thread thread)
    {
      this.head = (this.tail = new Link(null));
      this.owner = new WeakReference(thread);
      synchronized (stack)
      {
        this.next = stack.head;
        stack.head = this;
      }
    }
    
    void add(Recycler.DefaultHandle<?> handle)
    {
      Recycler.DefaultHandle.access$902(handle, this.id);
      
      Link tail = this.tail;
      int writeIndex;
      if ((writeIndex = tail.get()) == 16)
      {
        this.tail = (tail = tail.next = new Link(null));
        writeIndex = tail.get();
      }
      tail.elements[writeIndex] = handle;
      Recycler.DefaultHandle.access$202(handle, null);
      
      tail.lazySet(writeIndex + 1);
    }
    
    boolean hasFinalData()
    {
      return this.tail.readIndex != this.tail.get();
    }
    
    boolean transfer(Recycler.Stack<?> dst)
    {
      Link head = this.head;
      if (head == null) {
        return false;
      }
      if (head.readIndex == 16)
      {
        if (head.next == null) {
          return false;
        }
        this.head = (head = head.next);
      }
      int srcStart = head.readIndex;
      int srcEnd = head.get();
      int srcSize = srcEnd - srcStart;
      if (srcSize == 0) {
        return false;
      }
      int dstSize = dst.size;
      int expectedCapacity = dstSize + srcSize;
      if (expectedCapacity > dst.elements.length)
      {
        int actualCapacity = dst.increaseCapacity(expectedCapacity);
        srcEnd = Math.min(srcStart + actualCapacity - dstSize, srcEnd);
      }
      if (srcStart != srcEnd)
      {
        Recycler.DefaultHandle[] srcElems = head.elements;
        Recycler.DefaultHandle[] dstElems = dst.elements;
        int newDstSize = dstSize;
        for (int i = srcStart; i < srcEnd; i++)
        {
          Recycler.DefaultHandle element = srcElems[i];
          if (Recycler.DefaultHandle.access$1300(element) == 0) {
            Recycler.DefaultHandle.access$1302(element, Recycler.DefaultHandle.access$900(element));
          } else if (Recycler.DefaultHandle.access$1300(element) != Recycler.DefaultHandle.access$900(element)) {
            throw new IllegalStateException("recycled already");
          }
          Recycler.DefaultHandle.access$202(element, dst);
          dstElems[(newDstSize++)] = element;
          srcElems[i] = null;
        }
        dst.size = newDstSize;
        if ((srcEnd == 16) && (head.next != null)) {
          this.head = head.next;
        }
        head.readIndex = srcEnd;
        return true;
      }
      return false;
    }
  }
  
  static final class Stack<T>
  {
    final Recycler<T> parent;
    final Thread thread;
    private Recycler.DefaultHandle<?>[] elements;
    private final int maxCapacity;
    private int size;
    private volatile Recycler.WeakOrderQueue head;
    private Recycler.WeakOrderQueue cursor;
    private Recycler.WeakOrderQueue prev;
    
    Stack(Recycler<T> parent, Thread thread, int maxCapacity)
    {
      this.parent = parent;
      this.thread = thread;
      this.maxCapacity = maxCapacity;
      this.elements = new Recycler.DefaultHandle[Math.min(Recycler.INITIAL_CAPACITY, maxCapacity)];
    }
    
    int increaseCapacity(int expectedCapacity)
    {
      int newCapacity = this.elements.length;
      int maxCapacity = this.maxCapacity;
      do
      {
        newCapacity <<= 1;
      } while ((newCapacity < expectedCapacity) && (newCapacity < maxCapacity));
      newCapacity = Math.min(newCapacity, maxCapacity);
      if (newCapacity != this.elements.length) {
        this.elements = ((Recycler.DefaultHandle[])Arrays.copyOf(this.elements, newCapacity));
      }
      return newCapacity;
    }
    
    Recycler.DefaultHandle<T> pop()
    {
      int size = this.size;
      if (size == 0)
      {
        if (!scavenge()) {
          return null;
        }
        size = this.size;
      }
      size--;
      Recycler.DefaultHandle ret = this.elements[size];
      if (Recycler.DefaultHandle.access$900(ret) != Recycler.DefaultHandle.access$1300(ret)) {
        throw new IllegalStateException("recycled multiple times");
      }
      Recycler.DefaultHandle.access$1302(ret, 0);
      Recycler.DefaultHandle.access$902(ret, 0);
      this.size = size;
      return ret;
    }
    
    boolean scavenge()
    {
      if (scavengeSome()) {
        return true;
      }
      this.prev = null;
      this.cursor = this.head;
      return false;
    }
    
    boolean scavengeSome()
    {
      Recycler.WeakOrderQueue cursor = this.cursor;
      if (cursor == null)
      {
        cursor = this.head;
        if (cursor == null) {
          return false;
        }
      }
      boolean success = false;
      Recycler.WeakOrderQueue prev = this.prev;
      do
      {
        if (cursor.transfer(this))
        {
          success = true;
          break;
        }
        Recycler.WeakOrderQueue next = Recycler.WeakOrderQueue.access$1500(cursor);
        if (Recycler.WeakOrderQueue.access$1600(cursor).get() == null)
        {
          if (cursor.hasFinalData()) {
            while (cursor.transfer(this)) {
              success = true;
            }
          }
          if (prev != null) {
            Recycler.WeakOrderQueue.access$1502(prev, next);
          }
        }
        else
        {
          prev = cursor;
        }
        cursor = next;
      } while ((cursor != null) && (!success));
      this.prev = prev;
      this.cursor = cursor;
      return success;
    }
    
    void push(Recycler.DefaultHandle<?> item)
    {
      if ((Recycler.DefaultHandle.access$1300(item) | Recycler.DefaultHandle.access$900(item)) != 0) {
        throw new IllegalStateException("recycled already");
      }
      Recycler.DefaultHandle.access$1302(item, Recycler.DefaultHandle.access$902(item, Recycler.OWN_THREAD_ID));
      
      int size = this.size;
      if (size >= this.maxCapacity) {
        return;
      }
      if (size == this.elements.length) {
        this.elements = ((Recycler.DefaultHandle[])Arrays.copyOf(this.elements, Math.min(size << 1, this.maxCapacity)));
      }
      this.elements[size] = item;
      this.size = (size + 1);
    }
    
    Recycler.DefaultHandle<T> newHandle()
    {
      return new Recycler.DefaultHandle(this);
    }
  }
}
