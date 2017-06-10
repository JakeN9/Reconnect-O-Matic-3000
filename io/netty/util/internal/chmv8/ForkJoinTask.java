package io.netty.util.internal.chmv8;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

public abstract class ForkJoinTask<V>
  implements Future<V>, Serializable
{
  volatile int status;
  static final int DONE_MASK = -268435456;
  static final int NORMAL = -268435456;
  static final int CANCELLED = -1073741824;
  static final int EXCEPTIONAL = Integer.MIN_VALUE;
  static final int SIGNAL = 65536;
  static final int SMASK = 65535;
  private static final ExceptionNode[] exceptionTable;
  
  private int setCompletion(int completion)
  {
    int s;
    do
    {
      if ((s = this.status) < 0) {
        return s;
      }
    } while (!U.compareAndSwapInt(this, STATUS, s, s | completion));
    if (s >>> 16 != 0) {
      synchronized (this)
      {
        notifyAll();
      }
    }
    return completion;
  }
  
  final int doExec()
  {
    int s;
    if ((s = this.status) >= 0)
    {
      boolean completed;
      try
      {
        completed = exec();
      }
      catch (Throwable rex)
      {
        return setExceptionalCompletion(rex);
      }
      if (completed) {
        s = setCompletion(-268435456);
      }
    }
    return s;
  }
  
  final boolean trySetSignal()
  {
    int s = this.status;
    return (s >= 0) && (U.compareAndSwapInt(this, STATUS, s, s | 0x10000));
  }
  
  private int externalAwaitDone()
  {
    ForkJoinPool cp = ForkJoinPool.common;
    int s;
    if ((s = this.status) >= 0)
    {
      if (cp != null) {
        if ((this instanceof CountedCompleter)) {
          s = cp.externalHelpComplete((CountedCompleter)this);
        } else if (cp.tryExternalUnpush(this)) {
          s = doExec();
        }
      }
      if ((s >= 0) && ((s = this.status) >= 0))
      {
        boolean interrupted = false;
        do
        {
          if (U.compareAndSwapInt(this, STATUS, s, s | 0x10000)) {
            synchronized (this)
            {
              if (this.status >= 0) {
                try
                {
                  wait();
                }
                catch (InterruptedException ie)
                {
                  interrupted = true;
                }
              } else {
                notifyAll();
              }
            }
          }
        } while ((s = this.status) >= 0);
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
    return s;
  }
  
  private int externalInterruptibleAwaitDone()
    throws InterruptedException
  {
    ForkJoinPool cp = ForkJoinPool.common;
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    int s;
    if (((s = this.status) >= 0) && (cp != null)) {
      if ((this instanceof CountedCompleter)) {
        cp.externalHelpComplete((CountedCompleter)this);
      } else if (cp.tryExternalUnpush(this)) {
        doExec();
      }
    }
    while ((s = this.status) >= 0) {
      if (U.compareAndSwapInt(this, STATUS, s, s | 0x10000)) {
        synchronized (this)
        {
          if (this.status >= 0) {
            wait();
          } else {
            notifyAll();
          }
        }
      }
    }
    return s;
  }
  
  private int doJoin()
  {
    int s;
    Thread t;
    ForkJoinWorkerThread wt;
    ForkJoinPool.WorkQueue w;
    return ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ? wt.pool.awaitJoin(w, this) : ((w = (wt = (ForkJoinWorkerThread)t).workQueue).tryUnpush(this)) && ((s = doExec()) < 0) ? s : (s = this.status) < 0 ? s : externalAwaitDone();
  }
  
  private int doInvoke()
  {
    int s;
    Thread t;
    ForkJoinWorkerThread wt;
    return ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ? (wt = (ForkJoinWorkerThread)t).pool.awaitJoin(wt.workQueue, this) : (s = doExec()) < 0 ? s : externalAwaitDone();
  }
  
  static final class ExceptionNode
    extends WeakReference<ForkJoinTask<?>>
  {
    final Throwable ex;
    ExceptionNode next;
    final long thrower;
    
    ExceptionNode(ForkJoinTask<?> task, Throwable ex, ExceptionNode next)
    {
      super(ForkJoinTask.exceptionTableRefQueue);
      this.ex = ex;
      this.next = next;
      this.thrower = Thread.currentThread().getId();
    }
  }
  
  final int recordExceptionalCompletion(Throwable ex)
  {
    int s;
    if ((s = this.status) >= 0)
    {
      int h = System.identityHashCode(this);
      ReentrantLock lock = exceptionTableLock;
      lock.lock();
      try
      {
        expungeStaleExceptions();
        ExceptionNode[] t = exceptionTable;
        int i = h & t.length - 1;
        for (ExceptionNode e = t[i];; e = e.next) {
          if (e == null) {
            t[i] = new ExceptionNode(this, ex, t[i]);
          } else {
            if (e.get() == this) {
              break;
            }
          }
        }
      }
      finally
      {
        lock.unlock();
      }
      s = setCompletion(Integer.MIN_VALUE);
    }
    return s;
  }
  
  private int setExceptionalCompletion(Throwable ex)
  {
    int s = recordExceptionalCompletion(ex);
    if ((s & 0xF0000000) == Integer.MIN_VALUE) {
      internalPropagateException(ex);
    }
    return s;
  }
  
  static final void cancelIgnoringExceptions(ForkJoinTask<?> t)
  {
    if ((t != null) && (t.status >= 0)) {
      try
      {
        t.cancel(false);
      }
      catch (Throwable ignore) {}
    }
  }
  
  private void clearExceptionalCompletion()
  {
    int h = System.identityHashCode(this);
    ReentrantLock lock = exceptionTableLock;
    lock.lock();
    try
    {
      ExceptionNode[] t = exceptionTable;
      int i = h & t.length - 1;
      ExceptionNode e = t[i];
      ExceptionNode pred = null;
      while (e != null)
      {
        ExceptionNode next = e.next;
        if (e.get() == this)
        {
          if (pred == null)
          {
            t[i] = next; break;
          }
          pred.next = next;
          break;
        }
        pred = e;
        e = next;
      }
      expungeStaleExceptions();
      this.status = 0;
    }
    finally
    {
      lock.unlock();
    }
  }
  
  private Throwable getThrowableException()
  {
    if ((this.status & 0xF0000000) != Integer.MIN_VALUE) {
      return null;
    }
    int h = System.identityHashCode(this);
    
    ReentrantLock lock = exceptionTableLock;
    lock.lock();
    ExceptionNode e;
    try
    {
      expungeStaleExceptions();
      ExceptionNode[] t = exceptionTable;
      e = t[(h & t.length - 1)];
      while ((e != null) && (e.get() != this)) {
        e = e.next;
      }
    }
    finally
    {
      lock.unlock();
    }
    Throwable ex;
    if ((e == null) || ((ex = e.ex) == null)) {
      return null;
    }
    Throwable ex;
    return ex;
  }
  
  private static void expungeStaleExceptions()
  {
    Object x;
    while ((x = exceptionTableRefQueue.poll()) != null) {
      if ((x instanceof ExceptionNode))
      {
        ForkJoinTask<?> key = (ForkJoinTask)((ExceptionNode)x).get();
        ExceptionNode[] t = exceptionTable;
        int i = System.identityHashCode(key) & t.length - 1;
        ExceptionNode e = t[i];
        ExceptionNode pred = null;
        while (e != null)
        {
          ExceptionNode next = e.next;
          if (e == x)
          {
            if (pred == null)
            {
              t[i] = next; break;
            }
            pred.next = next;
            break;
          }
          pred = e;
          e = next;
        }
      }
    }
  }
  
  static final void helpExpungeStaleExceptions()
  {
    ReentrantLock lock = exceptionTableLock;
    if (lock.tryLock()) {
      try
      {
        expungeStaleExceptions();
      }
      finally
      {
        lock.unlock();
      }
    }
  }
  
  static void rethrow(Throwable ex)
  {
    if (ex != null) {
      uncheckedThrow(ex);
    }
  }
  
  static <T extends Throwable> void uncheckedThrow(Throwable t)
    throws Throwable
  {
    throw t;
  }
  
  private void reportException(int s)
  {
    if (s == -1073741824) {
      throw new CancellationException();
    }
    if (s == Integer.MIN_VALUE) {
      rethrow(getThrowableException());
    }
  }
  
  public final ForkJoinTask<V> fork()
  {
    Thread t;
    if (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)) {
      ((ForkJoinWorkerThread)t).workQueue.push(this);
    } else {
      ForkJoinPool.common.externalPush(this);
    }
    return this;
  }
  
  public final V join()
  {
    int s;
    if ((s = doJoin() & 0xF0000000) != -268435456) {
      reportException(s);
    }
    return (V)getRawResult();
  }
  
  public final V invoke()
  {
    int s;
    if ((s = doInvoke() & 0xF0000000) != -268435456) {
      reportException(s);
    }
    return (V)getRawResult();
  }
  
  public static void invokeAll(ForkJoinTask<?> t1, ForkJoinTask<?> t2)
  {
    t2.fork();
    int s1;
    if ((s1 = t1.doInvoke() & 0xF0000000) != -268435456) {
      t1.reportException(s1);
    }
    int s2;
    if ((s2 = t2.doJoin() & 0xF0000000) != -268435456) {
      t2.reportException(s2);
    }
  }
  
  public static void invokeAll(ForkJoinTask<?>... tasks)
  {
    Throwable ex = null;
    int last = tasks.length - 1;
    for (int i = last; i >= 0; i--)
    {
      ForkJoinTask<?> t = tasks[i];
      if (t == null)
      {
        if (ex == null) {
          ex = new NullPointerException();
        }
      }
      else if (i != 0) {
        t.fork();
      } else if ((t.doInvoke() < -268435456) && (ex == null)) {
        ex = t.getException();
      }
    }
    for (int i = 1; i <= last; i++)
    {
      ForkJoinTask<?> t = tasks[i];
      if (t != null) {
        if (ex != null) {
          t.cancel(false);
        } else if (t.doJoin() < -268435456) {
          ex = t.getException();
        }
      }
    }
    if (ex != null) {
      rethrow(ex);
    }
  }
  
  public static <T extends ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> tasks)
  {
    if ((!(tasks instanceof RandomAccess)) || (!(tasks instanceof List)))
    {
      invokeAll((ForkJoinTask[])tasks.toArray(new ForkJoinTask[tasks.size()]));
      return tasks;
    }
    List<? extends ForkJoinTask<?>> ts = (List)tasks;
    
    Throwable ex = null;
    int last = ts.size() - 1;
    for (int i = last; i >= 0; i--)
    {
      ForkJoinTask<?> t = (ForkJoinTask)ts.get(i);
      if (t == null)
      {
        if (ex == null) {
          ex = new NullPointerException();
        }
      }
      else if (i != 0) {
        t.fork();
      } else if ((t.doInvoke() < -268435456) && (ex == null)) {
        ex = t.getException();
      }
    }
    for (int i = 1; i <= last; i++)
    {
      ForkJoinTask<?> t = (ForkJoinTask)ts.get(i);
      if (t != null) {
        if (ex != null) {
          t.cancel(false);
        } else if (t.doJoin() < -268435456) {
          ex = t.getException();
        }
      }
    }
    if (ex != null) {
      rethrow(ex);
    }
    return tasks;
  }
  
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    return (setCompletion(-1073741824) & 0xF0000000) == -1073741824;
  }
  
  public final boolean isDone()
  {
    return this.status < 0;
  }
  
  public final boolean isCancelled()
  {
    return (this.status & 0xF0000000) == -1073741824;
  }
  
  public final boolean isCompletedAbnormally()
  {
    return this.status < -268435456;
  }
  
  public final boolean isCompletedNormally()
  {
    return (this.status & 0xF0000000) == -268435456;
  }
  
  public final Throwable getException()
  {
    int s = this.status & 0xF0000000;
    return s == -1073741824 ? new CancellationException() : s >= -268435456 ? null : getThrowableException();
  }
  
  public void completeExceptionally(Throwable ex)
  {
    setExceptionalCompletion(((ex instanceof RuntimeException)) || ((ex instanceof Error)) ? ex : new RuntimeException(ex));
  }
  
  public void complete(V value)
  {
    try
    {
      setRawResult(value);
    }
    catch (Throwable rex)
    {
      setExceptionalCompletion(rex);
      return;
    }
    setCompletion(-268435456);
  }
  
  public final void quietlyComplete()
  {
    setCompletion(-268435456);
  }
  
  public final V get()
    throws InterruptedException, ExecutionException
  {
    int s = (Thread.currentThread() instanceof ForkJoinWorkerThread) ? doJoin() : externalInterruptibleAwaitDone();
    if ((s &= 0xF0000000) == -1073741824) {
      throw new CancellationException();
    }
    Throwable ex;
    if ((s == Integer.MIN_VALUE) && ((ex = getThrowableException()) != null)) {
      throw new ExecutionException(ex);
    }
    return (V)getRawResult();
  }
  
  public final V get(long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException
  {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    long ns = unit.toNanos(timeout);
    int s;
    if (((s = this.status) >= 0) && (ns > 0L))
    {
      long deadline = System.nanoTime() + ns;
      ForkJoinPool p = null;
      ForkJoinPool.WorkQueue w = null;
      Thread t = Thread.currentThread();
      if ((t instanceof ForkJoinWorkerThread))
      {
        ForkJoinWorkerThread wt = (ForkJoinWorkerThread)t;
        p = wt.pool;
        w = wt.workQueue;
        p.helpJoinOnce(w, this);
      }
      else
      {
        ForkJoinPool cp;
        if ((cp = ForkJoinPool.common) != null) {
          if ((this instanceof CountedCompleter)) {
            cp.externalHelpComplete((CountedCompleter)this);
          } else if (cp.tryExternalUnpush(this)) {
            doExec();
          }
        }
      }
      boolean canBlock = false;
      boolean interrupted = false;
      try
      {
        while ((s = this.status) >= 0) {
          if ((w != null) && (w.qlock < 0))
          {
            cancelIgnoringExceptions(this);
          }
          else if (!canBlock)
          {
            if ((p == null) || (p.tryCompensate(p.ctl))) {
              canBlock = true;
            }
          }
          else
          {
            long ms;
            if (((ms = TimeUnit.NANOSECONDS.toMillis(ns)) > 0L) && (U.compareAndSwapInt(this, STATUS, s, s | 0x10000))) {
              synchronized (this)
              {
                if (this.status >= 0) {
                  try
                  {
                    wait(ms);
                  }
                  catch (InterruptedException ie)
                  {
                    if (p == null) {
                      interrupted = true;
                    }
                  }
                } else {
                  notifyAll();
                }
              }
            }
            if (((s = this.status) >= 0) && (!interrupted)) {
              if ((ns = deadline - System.nanoTime()) <= 0L) {
                break;
              }
            }
          }
        }
      }
      finally
      {
        if ((p != null) && (canBlock)) {
          p.incrementActiveCount();
        }
      }
      if (interrupted) {
        throw new InterruptedException();
      }
    }
    if ((s &= 0xF0000000) != -268435456)
    {
      if (s == -1073741824) {
        throw new CancellationException();
      }
      if (s != Integer.MIN_VALUE) {
        throw new TimeoutException();
      }
      Throwable ex;
      if ((ex = getThrowableException()) != null) {
        throw new ExecutionException(ex);
      }
    }
    return (V)getRawResult();
  }
  
  public final void quietlyJoin()
  {
    doJoin();
  }
  
  public final void quietlyInvoke()
  {
    doInvoke();
  }
  
  public static void helpQuiesce()
  {
    Thread t;
    if (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread))
    {
      ForkJoinWorkerThread wt = (ForkJoinWorkerThread)t;
      wt.pool.helpQuiescePool(wt.workQueue);
    }
    else
    {
      ForkJoinPool.quiesceCommonPool();
    }
  }
  
  public void reinitialize()
  {
    if ((this.status & 0xF0000000) == Integer.MIN_VALUE) {
      clearExceptionalCompletion();
    } else {
      this.status = 0;
    }
  }
  
  public static ForkJoinPool getPool()
  {
    Thread t = Thread.currentThread();
    return (t instanceof ForkJoinWorkerThread) ? ((ForkJoinWorkerThread)t).pool : null;
  }
  
  public static boolean inForkJoinPool()
  {
    return Thread.currentThread() instanceof ForkJoinWorkerThread;
  }
  
  public boolean tryUnfork()
  {
    Thread t;
    return ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ? ((ForkJoinWorkerThread)t).workQueue.tryUnpush(this) : ForkJoinPool.common.tryExternalUnpush(this);
  }
  
  public static int getQueuedTaskCount()
  {
    Thread t;
    ForkJoinPool.WorkQueue q;
    ForkJoinPool.WorkQueue q;
    if (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)) {
      q = ((ForkJoinWorkerThread)t).workQueue;
    } else {
      q = ForkJoinPool.commonSubmitterQueue();
    }
    return q == null ? 0 : q.queueSize();
  }
  
  public static int getSurplusQueuedTaskCount()
  {
    return ForkJoinPool.getSurplusQueuedTaskCount();
  }
  
  protected static ForkJoinTask<?> peekNextLocalTask()
  {
    Thread t;
    ForkJoinPool.WorkQueue q;
    ForkJoinPool.WorkQueue q;
    if (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)) {
      q = ((ForkJoinWorkerThread)t).workQueue;
    } else {
      q = ForkJoinPool.commonSubmitterQueue();
    }
    return q == null ? null : q.peek();
  }
  
  protected static ForkJoinTask<?> pollNextLocalTask()
  {
    Thread t;
    return ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ? ((ForkJoinWorkerThread)t).workQueue.nextLocalTask() : null;
  }
  
  protected static ForkJoinTask<?> pollTask()
  {
    Thread t;
    ForkJoinWorkerThread wt;
    return ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ? (wt = (ForkJoinWorkerThread)t).pool.nextTaskFor(wt.workQueue) : null;
  }
  
  public final short getForkJoinTaskTag()
  {
    return (short)this.status;
  }
  
  public final short setForkJoinTaskTag(short tag)
  {
    int s;
    while (!U.compareAndSwapInt(this, STATUS, s = this.status, s & 0xFFFF0000 | tag & 0xFFFF)) {}
    return (short)s;
  }
  
  public final boolean compareAndSetForkJoinTaskTag(short e, short tag)
  {
    int s;
    do
    {
      if ((short)(s = this.status) != e) {
        return false;
      }
    } while (!U.compareAndSwapInt(this, STATUS, s, s & 0xFFFF0000 | tag & 0xFFFF));
    return true;
  }
  
  static final class AdaptedRunnable<T>
    extends ForkJoinTask<T>
    implements RunnableFuture<T>
  {
    final Runnable runnable;
    T result;
    private static final long serialVersionUID = 5232453952276885070L;
    
    AdaptedRunnable(Runnable runnable, T result)
    {
      if (runnable == null) {
        throw new NullPointerException();
      }
      this.runnable = runnable;
      this.result = result;
    }
    
    public final T getRawResult()
    {
      return (T)this.result;
    }
    
    public final void setRawResult(T v)
    {
      this.result = v;
    }
    
    public final boolean exec()
    {
      this.runnable.run();return true;
    }
    
    public final void run()
    {
      invoke();
    }
  }
  
  static final class AdaptedRunnableAction
    extends ForkJoinTask<Void>
    implements RunnableFuture<Void>
  {
    final Runnable runnable;
    private static final long serialVersionUID = 5232453952276885070L;
    
    AdaptedRunnableAction(Runnable runnable)
    {
      if (runnable == null) {
        throw new NullPointerException();
      }
      this.runnable = runnable;
    }
    
    public final Void getRawResult()
    {
      return null;
    }
    
    public final void setRawResult(Void v) {}
    
    public final boolean exec()
    {
      this.runnable.run();return true;
    }
    
    public final void run()
    {
      invoke();
    }
  }
  
  static final class RunnableExecuteAction
    extends ForkJoinTask<Void>
  {
    final Runnable runnable;
    private static final long serialVersionUID = 5232453952276885070L;
    
    RunnableExecuteAction(Runnable runnable)
    {
      if (runnable == null) {
        throw new NullPointerException();
      }
      this.runnable = runnable;
    }
    
    public final Void getRawResult()
    {
      return null;
    }
    
    public final void setRawResult(Void v) {}
    
    public final boolean exec()
    {
      this.runnable.run();return true;
    }
    
    void internalPropagateException(Throwable ex)
    {
      rethrow(ex);
    }
  }
  
  static final class AdaptedCallable<T>
    extends ForkJoinTask<T>
    implements RunnableFuture<T>
  {
    final Callable<? extends T> callable;
    T result;
    private static final long serialVersionUID = 2838392045355241008L;
    
    AdaptedCallable(Callable<? extends T> callable)
    {
      if (callable == null) {
        throw new NullPointerException();
      }
      this.callable = callable;
    }
    
    public final T getRawResult()
    {
      return (T)this.result;
    }
    
    public final void setRawResult(T v)
    {
      this.result = v;
    }
    
    public final boolean exec()
    {
      try
      {
        this.result = this.callable.call();
        return true;
      }
      catch (Error err)
      {
        throw err;
      }
      catch (RuntimeException rex)
      {
        throw rex;
      }
      catch (Exception ex)
      {
        throw new RuntimeException(ex);
      }
    }
    
    public final void run()
    {
      invoke();
    }
  }
  
  public static ForkJoinTask<?> adapt(Runnable runnable)
  {
    return new AdaptedRunnableAction(runnable);
  }
  
  public static <T> ForkJoinTask<T> adapt(Runnable runnable, T result)
  {
    return new AdaptedRunnable(runnable, result);
  }
  
  public static <T> ForkJoinTask<T> adapt(Callable<? extends T> callable)
  {
    return new AdaptedCallable(callable);
  }
  
  private void writeObject(ObjectOutputStream s)
    throws IOException
  {
    s.defaultWriteObject();
    s.writeObject(getException());
  }
  
  private void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException
  {
    s.defaultReadObject();
    Object ex = s.readObject();
    if (ex != null) {
      setExceptionalCompletion((Throwable)ex);
    }
  }
  
  private static final ReentrantLock exceptionTableLock = new ReentrantLock();
  private static final ReferenceQueue<Object> exceptionTableRefQueue = new ReferenceQueue();
  private static final int EXCEPTION_MAP_CAPACITY = 32;
  private static final long serialVersionUID = -7721805057305804111L;
  private static final Unsafe U;
  private static final long STATUS;
  
  static
  {
    exceptionTable = new ExceptionNode[32];
    try
    {
      U = getUnsafe();
      Class<?> k = ForkJoinTask.class;
      STATUS = U.objectFieldOffset(k.getDeclaredField("status"));
    }
    catch (Exception e)
    {
      throw new Error(e);
    }
  }
  
  private static Unsafe getUnsafe()
  {
    try
    {
      return Unsafe.getUnsafe();
    }
    catch (SecurityException tryReflectionInstead)
    {
      try
      {
        (Unsafe)AccessController.doPrivileged(new PrivilegedExceptionAction()
        {
          public Unsafe run()
            throws Exception
          {
            Class<Unsafe> k = Unsafe.class;
            for (Field f : k.getDeclaredFields())
            {
              f.setAccessible(true);
              Object x = f.get(null);
              if (k.isInstance(x)) {
                return (Unsafe)k.cast(x);
              }
            }
            throw new NoSuchFieldError("the Unsafe");
          }
        });
      }
      catch (PrivilegedActionException e)
      {
        throw new RuntimeException("Could not initialize intrinsics", e.getCause());
      }
    }
  }
  
  void internalPropagateException(Throwable ex) {}
  
  public abstract V getRawResult();
  
  protected abstract void setRawResult(V paramV);
  
  protected abstract boolean exec();
}
