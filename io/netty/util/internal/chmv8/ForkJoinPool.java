package io.netty.util.internal.chmv8;

import io.netty.util.internal.ThreadLocalRandom;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

public class ForkJoinPool
  extends AbstractExecutorService
{
  static final ThreadLocal<Submitter> submitters;
  public static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory;
  private static final RuntimePermission modifyThreadPermission;
  static final ForkJoinPool common;
  static final int commonParallelism;
  private static int poolNumberSequence;
  private static final long IDLE_TIMEOUT = 2000000000L;
  private static final long FAST_IDLE_TIMEOUT = 200000000L;
  private static final long TIMEOUT_SLOP = 2000000L;
  private static final int MAX_HELP = 64;
  private static final int SEED_INCREMENT = 1640531527;
  private static final int AC_SHIFT = 48;
  private static final int TC_SHIFT = 32;
  private static final int ST_SHIFT = 31;
  private static final int EC_SHIFT = 16;
  private static final int SMASK = 65535;
  private static final int MAX_CAP = 32767;
  private static final int EVENMASK = 65534;
  private static final int SQMASK = 126;
  private static final int SHORT_SIGN = 32768;
  private static final int INT_SIGN = Integer.MIN_VALUE;
  private static final long STOP_BIT = 2147483648L;
  private static final long AC_MASK = -281474976710656L;
  private static final long TC_MASK = 281470681743360L;
  private static final long TC_UNIT = 4294967296L;
  private static final long AC_UNIT = 281474976710656L;
  private static final int UAC_SHIFT = 16;
  private static final int UTC_SHIFT = 0;
  private static final int UAC_MASK = -65536;
  private static final int UTC_MASK = 65535;
  private static final int UAC_UNIT = 65536;
  private static final int UTC_UNIT = 1;
  private static final int E_MASK = Integer.MAX_VALUE;
  private static final int E_SEQ = 65536;
  private static final int SHUTDOWN = Integer.MIN_VALUE;
  private static final int PL_LOCK = 2;
  private static final int PL_SIGNAL = 1;
  private static final int PL_SPINS = 256;
  static final int LIFO_QUEUE = 0;
  static final int FIFO_QUEUE = 1;
  static final int SHARED_QUEUE = -1;
  volatile long pad00;
  volatile long pad01;
  volatile long pad02;
  volatile long pad03;
  volatile long pad04;
  volatile long pad05;
  volatile long pad06;
  volatile long stealCount;
  volatile long ctl;
  volatile int plock;
  volatile int indexSeed;
  final short parallelism;
  final short mode;
  WorkQueue[] workQueues;
  final ForkJoinWorkerThreadFactory factory;
  final Thread.UncaughtExceptionHandler ueh;
  final String workerNamePrefix;
  volatile Object pad10;
  volatile Object pad11;
  volatile Object pad12;
  volatile Object pad13;
  volatile Object pad14;
  volatile Object pad15;
  volatile Object pad16;
  volatile Object pad17;
  volatile Object pad18;
  volatile Object pad19;
  volatile Object pad1a;
  volatile Object pad1b;
  private static final Unsafe U;
  private static final long CTL;
  private static final long PARKBLOCKER;
  private static final int ABASE;
  private static final int ASHIFT;
  private static final long STEALCOUNT;
  private static final long PLOCK;
  private static final long INDEXSEED;
  private static final long QBASE;
  private static final long QLOCK;
  
  private static void checkPermission()
  {
    SecurityManager security = System.getSecurityManager();
    if (security != null) {
      security.checkPermission(modifyThreadPermission);
    }
  }
  
  public static abstract interface ForkJoinWorkerThreadFactory
  {
    public abstract ForkJoinWorkerThread newThread(ForkJoinPool paramForkJoinPool);
  }
  
  static final class DefaultForkJoinWorkerThreadFactory
    implements ForkJoinPool.ForkJoinWorkerThreadFactory
  {
    public final ForkJoinWorkerThread newThread(ForkJoinPool pool)
    {
      return new ForkJoinWorkerThread(pool);
    }
  }
  
  static final class EmptyTask
    extends ForkJoinTask<Void>
  {
    private static final long serialVersionUID = -7721805057305804111L;
    
    EmptyTask()
    {
      this.status = -268435456;
    }
    
    public final Void getRawResult()
    {
      return null;
    }
    
    public final void setRawResult(Void x) {}
    
    public final boolean exec()
    {
      return true;
    }
  }
  
  static final class WorkQueue
  {
    static final int INITIAL_QUEUE_CAPACITY = 8192;
    static final int MAXIMUM_QUEUE_CAPACITY = 67108864;
    volatile long pad00;
    volatile long pad01;
    volatile long pad02;
    volatile long pad03;
    volatile long pad04;
    volatile long pad05;
    volatile long pad06;
    volatile int eventCount;
    int nextWait;
    int nsteals;
    int hint;
    short poolIndex;
    final short mode;
    volatile int qlock;
    volatile int base;
    int top;
    ForkJoinTask<?>[] array;
    final ForkJoinPool pool;
    final ForkJoinWorkerThread owner;
    volatile Thread parker;
    volatile ForkJoinTask<?> currentJoin;
    ForkJoinTask<?> currentSteal;
    volatile Object pad10;
    volatile Object pad11;
    volatile Object pad12;
    volatile Object pad13;
    volatile Object pad14;
    volatile Object pad15;
    volatile Object pad16;
    volatile Object pad17;
    volatile Object pad18;
    volatile Object pad19;
    volatile Object pad1a;
    volatile Object pad1b;
    volatile Object pad1c;
    volatile Object pad1d;
    private static final Unsafe U;
    private static final long QBASE;
    private static final long QLOCK;
    private static final int ABASE;
    private static final int ASHIFT;
    
    WorkQueue(ForkJoinPool pool, ForkJoinWorkerThread owner, int mode, int seed)
    {
      this.pool = pool;
      this.owner = owner;
      this.mode = ((short)mode);
      this.hint = seed;
      
      this.base = (this.top = 'က');
    }
    
    final int queueSize()
    {
      int n = this.base - this.top;
      return n >= 0 ? 0 : -n;
    }
    
    final boolean isEmpty()
    {
      int s;
      int n = this.base - (s = this.top);
      ForkJoinTask<?>[] a;
      int m;
      return (n >= 0) || ((n == -1) && (((a = this.array) == null) || ((m = a.length - 1) < 0) || (U.getObject(a, ((m & s - 1) << ASHIFT) + ABASE) == null)));
    }
    
    final void push(ForkJoinTask<?> task)
    {
      int s = this.top;
      ForkJoinTask<?>[] a;
      if ((a = this.array) != null)
      {
        int m = a.length - 1;
        U.putOrderedObject(a, ((m & s) << ASHIFT) + ABASE, task);
        int n;
        if ((n = (this.top = s + 1) - this.base) <= 2)
        {
          ForkJoinPool p;
          (p = this.pool).signalWork(p.workQueues, this);
        }
        else if (n >= m)
        {
          growArray();
        }
      }
    }
    
    final ForkJoinTask<?>[] growArray()
    {
      ForkJoinTask<?>[] oldA = this.array;
      int size = oldA != null ? oldA.length << 1 : 8192;
      if (size > 67108864) {
        throw new RejectedExecutionException("Queue capacity exceeded");
      }
      ForkJoinTask<?>[] a = this.array = new ForkJoinTask[size];
      int oldMask;
      int t;
      int b;
      if ((oldA != null) && ((oldMask = oldA.length - 1) >= 0) && ((t = this.top) - (b = this.base) > 0))
      {
        int mask = size - 1;
        do
        {
          int oldj = ((b & oldMask) << ASHIFT) + ABASE;
          int j = ((b & mask) << ASHIFT) + ABASE;
          ForkJoinTask<?> x = (ForkJoinTask)U.getObjectVolatile(oldA, oldj);
          if ((x != null) && (U.compareAndSwapObject(oldA, oldj, x, null))) {
            U.putObjectVolatile(a, j, x);
          }
          b++;
        } while (b != t);
      }
      return a;
    }
    
    final ForkJoinTask<?> pop()
    {
      ForkJoinTask<?>[] a;
      int m;
      if (((a = this.array) != null) && ((m = a.length - 1) >= 0))
      {
        int s;
        while ((s = this.top - 1) - this.base >= 0)
        {
          long j = ((m & s) << ASHIFT) + ABASE;
          ForkJoinTask<?> t;
          if ((t = (ForkJoinTask)U.getObject(a, j)) == null) {
            break;
          }
          if (U.compareAndSwapObject(a, j, t, null))
          {
            this.top = s;
            return t;
          }
        }
      }
      return null;
    }
    
    final ForkJoinTask<?> pollAt(int b)
    {
      ForkJoinTask<?>[] a;
      if ((a = this.array) != null)
      {
        int j = ((a.length - 1 & b) << ASHIFT) + ABASE;
        ForkJoinTask<?> t;
        if (((t = (ForkJoinTask)U.getObjectVolatile(a, j)) != null) && (this.base == b) && (U.compareAndSwapObject(a, j, t, null)))
        {
          U.putOrderedInt(this, QBASE, b + 1);
          return t;
        }
      }
      return null;
    }
    
    final ForkJoinTask<?> poll()
    {
      int b;
      ForkJoinTask<?>[] a;
      while (((b = this.base) - this.top < 0) && ((a = this.array) != null))
      {
        int j = ((a.length - 1 & b) << ASHIFT) + ABASE;
        ForkJoinTask<?> t = (ForkJoinTask)U.getObjectVolatile(a, j);
        if (t != null)
        {
          if (U.compareAndSwapObject(a, j, t, null))
          {
            U.putOrderedInt(this, QBASE, b + 1);
            return t;
          }
        }
        else if (this.base == b)
        {
          if (b + 1 == this.top) {
            break;
          }
          Thread.yield();
        }
      }
      return null;
    }
    
    final ForkJoinTask<?> nextLocalTask()
    {
      return this.mode == 0 ? pop() : poll();
    }
    
    final ForkJoinTask<?> peek()
    {
      ForkJoinTask<?>[] a = this.array;
      int m;
      if ((a == null) || ((m = a.length - 1) < 0)) {
        return null;
      }
      int m;
      int i = this.mode == 0 ? this.top - 1 : this.base;
      int j = ((i & m) << ASHIFT) + ABASE;
      return (ForkJoinTask)U.getObjectVolatile(a, j);
    }
    
    final boolean tryUnpush(ForkJoinTask<?> t)
    {
      ForkJoinTask<?>[] a;
      int s;
      if (((a = this.array) != null) && ((s = this.top) != this.base) && (U.compareAndSwapObject(a, ((a.length - 1 & --s) << ASHIFT) + ABASE, t, null)))
      {
        this.top = s;
        return true;
      }
      return false;
    }
    
    final void cancelAll()
    {
      ForkJoinTask.cancelIgnoringExceptions(this.currentJoin);
      ForkJoinTask.cancelIgnoringExceptions(this.currentSteal);
      ForkJoinTask<?> t;
      while ((t = poll()) != null) {
        ForkJoinTask.cancelIgnoringExceptions(t);
      }
    }
    
    final void pollAndExecAll()
    {
      ForkJoinTask<?> t;
      while ((t = poll()) != null) {
        t.doExec();
      }
    }
    
    final void runTask(ForkJoinTask<?> task)
    {
      if ((this.currentSteal = task) != null)
      {
        task.doExec();
        ForkJoinTask<?>[] a = this.array;
        int md = this.mode;
        this.nsteals += 1;
        this.currentSteal = null;
        if (md != 0)
        {
          pollAndExecAll();
        }
        else if (a != null)
        {
          int m = a.length - 1;
          int s;
          while ((s = this.top - 1) - this.base >= 0)
          {
            long i = ((m & s) << ASHIFT) + ABASE;
            ForkJoinTask<?> t = (ForkJoinTask)U.getObject(a, i);
            if (t == null) {
              break;
            }
            if (U.compareAndSwapObject(a, i, t, null))
            {
              this.top = s;
              t.doExec();
            }
          }
        }
      }
    }
    
    final boolean tryRemoveAndExec(ForkJoinTask<?> task)
    {
      ForkJoinTask<?>[] a;
      int m;
      int s;
      int b;
      int n;
      boolean stat;
      if ((task != null) && ((a = this.array) != null) && ((m = a.length - 1) >= 0) && ((n = (s = this.top) - (b = this.base)) > 0))
      {
        boolean removed = false;boolean empty = true;
        boolean stat = true;
        for (;;)
        {
          s--;long j = ((s & m) << ASHIFT) + ABASE;
          ForkJoinTask<?> t = (ForkJoinTask)U.getObject(a, j);
          if (t == null) {
            break;
          }
          if (t == task)
          {
            if (s + 1 == this.top)
            {
              if (!U.compareAndSwapObject(a, j, task, null)) {
                break;
              }
              this.top = s;
              removed = true; break;
            }
            if (this.base != b) {
              break;
            }
            removed = U.compareAndSwapObject(a, j, task, new ForkJoinPool.EmptyTask()); break;
          }
          if (t.status >= 0)
          {
            empty = false;
          }
          else if (s + 1 == this.top)
          {
            if (!U.compareAndSwapObject(a, j, t, null)) {
              break;
            }
            this.top = s; break;
          }
          n--;
          if (n == 0)
          {
            if ((empty) || (this.base != b)) {
              break;
            }
            stat = false; break;
          }
        }
        if (removed) {
          task.doExec();
        }
      }
      else
      {
        stat = false;
      }
      return stat;
    }
    
    final boolean pollAndExecCC(CountedCompleter<?> root)
    {
      int b;
      ForkJoinTask<?>[] a;
      if (((b = this.base) - this.top < 0) && ((a = this.array) != null))
      {
        long j = ((a.length - 1 & b) << ASHIFT) + ABASE;
        Object o;
        if ((o = U.getObjectVolatile(a, j)) == null) {
          return true;
        }
        if ((o instanceof CountedCompleter))
        {
          CountedCompleter<?> t = (CountedCompleter)o;CountedCompleter<?> r = t;
          for (;;)
          {
            if (r == root)
            {
              if ((this.base == b) && (U.compareAndSwapObject(a, j, t, null)))
              {
                U.putOrderedInt(this, QBASE, b + 1);
                t.doExec();
              }
              return true;
            }
            if ((r = r.completer) == null) {
              break;
            }
          }
        }
      }
      return false;
    }
    
    final boolean externalPopAndExecCC(CountedCompleter<?> root)
    {
      int s;
      ForkJoinTask<?>[] a;
      if ((this.base - (s = this.top) < 0) && ((a = this.array) != null))
      {
        long j = ((a.length - 1 & s - 1) << ASHIFT) + ABASE;
        Object o;
        if (((o = U.getObject(a, j)) instanceof CountedCompleter))
        {
          CountedCompleter<?> t = (CountedCompleter)o;CountedCompleter<?> r = t;
          for (;;)
          {
            if (r == root)
            {
              if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                if ((this.top == s) && (this.array == a) && (U.compareAndSwapObject(a, j, t, null)))
                {
                  this.top = (s - 1);
                  this.qlock = 0;
                  t.doExec();
                }
                else
                {
                  this.qlock = 0;
                }
              }
              return true;
            }
            if ((r = r.completer) == null) {
              break;
            }
          }
        }
      }
      return false;
    }
    
    final boolean internalPopAndExecCC(CountedCompleter<?> root)
    {
      int s;
      ForkJoinTask<?>[] a;
      if ((this.base - (s = this.top) < 0) && ((a = this.array) != null))
      {
        long j = ((a.length - 1 & s - 1) << ASHIFT) + ABASE;
        Object o;
        if (((o = U.getObject(a, j)) instanceof CountedCompleter))
        {
          CountedCompleter<?> t = (CountedCompleter)o;CountedCompleter<?> r = t;
          for (;;)
          {
            if (r == root)
            {
              if (U.compareAndSwapObject(a, j, t, null))
              {
                this.top = (s - 1);
                t.doExec();
              }
              return true;
            }
            if ((r = r.completer) == null) {
              break;
            }
          }
        }
      }
      return false;
    }
    
    final boolean isApparentlyUnblocked()
    {
      Thread wt;
      Thread.State s;
      return (this.eventCount >= 0) && ((wt = this.owner) != null) && ((s = wt.getState()) != Thread.State.BLOCKED) && (s != Thread.State.WAITING) && (s != Thread.State.TIMED_WAITING);
    }
    
    static
    {
      try
      {
        U = ForkJoinPool.access$000();
        Class<?> k = WorkQueue.class;
        Class<?> ak = ForkJoinTask[].class;
        QBASE = U.objectFieldOffset(k.getDeclaredField("base"));
        
        QLOCK = U.objectFieldOffset(k.getDeclaredField("qlock"));
        
        ABASE = U.arrayBaseOffset(ak);
        int scale = U.arrayIndexScale(ak);
        if ((scale & scale - 1) != 0) {
          throw new Error("data type scale not a power of two");
        }
        ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
      }
      catch (Exception e)
      {
        throw new Error(e);
      }
    }
  }
  
  private static final synchronized int nextPoolId()
  {
    return ++poolNumberSequence;
  }
  
  private int acquirePlock()
  {
    int spins = 256;
    for (;;)
    {
      int ps;
      int nps;
      if ((((ps = this.plock) & 0x2) == 0) && (U.compareAndSwapInt(this, PLOCK, ps, nps = ps + 2))) {
        return nps;
      }
      if (spins >= 0)
      {
        if (ThreadLocalRandom.current().nextInt() >= 0) {
          spins--;
        }
      }
      else if (U.compareAndSwapInt(this, PLOCK, ps, ps | 0x1)) {
        synchronized (this)
        {
          if ((this.plock & 0x1) != 0) {
            try
            {
              wait();
            }
            catch (InterruptedException ie)
            {
              try
              {
                Thread.currentThread().interrupt();
              }
              catch (SecurityException ignore) {}
            }
          } else {
            notifyAll();
          }
        }
      }
    }
  }
  
  private void releasePlock(int ps)
  {
    this.plock = ps;
    synchronized (this)
    {
      notifyAll();
    }
  }
  
  private void tryAddWorker()
  {
    long c;
    int u;
    int e;
    while (((u = (int)((c = this.ctl) >>> 32)) < 0) && ((u & 0x8000) != 0) && ((e = (int)c) >= 0))
    {
      long nc = (u + 1 & 0xFFFF | u + 65536 & 0xFFFF0000) << 32 | e;
      if (U.compareAndSwapLong(this, CTL, c, nc))
      {
        Throwable ex = null;
        ForkJoinWorkerThread wt = null;
        try
        {
          ForkJoinWorkerThreadFactory fac;
          if (((fac = this.factory) != null) && ((wt = fac.newThread(this)) != null))
          {
            wt.start();
            break;
          }
        }
        catch (Throwable rex)
        {
          ex = rex;
        }
        deregisterWorker(wt, ex);
        break;
      }
    }
  }
  
  final WorkQueue registerWorker(ForkJoinWorkerThread wt)
  {
    wt.setDaemon(true);
    Thread.UncaughtExceptionHandler handler;
    if ((handler = this.ueh) != null) {
      wt.setUncaughtExceptionHandler(handler);
    }
    int s;
    do
    {
      s += 1640531527;
    } while ((!U.compareAndSwapInt(this, INDEXSEED, s = this.indexSeed, s)) || (s == 0));
    WorkQueue w = new WorkQueue(this, wt, this.mode, s);
    int ps;
    if ((((ps = this.plock) & 0x2) != 0) || (!U.compareAndSwapInt(this, PLOCK, , ps))) {
      ps = acquirePlock();
    }
    int nps = ps & 0x80000000 | ps + 2 & 0x7FFFFFFF;
    try
    {
      WorkQueue[] ws;
      if ((ws = this.workQueues) != null)
      {
        int n = ws.length;int m = n - 1;
        int r = s << 1 | 0x1;
        if (ws[(r &= m)] != null)
        {
          int probes = 0;
          int step = n <= 4 ? 2 : (n >>> 1 & 0xFFFE) + 2;
          while (ws[(r = r + step & m)] != null)
          {
            probes++;
            if (probes >= n)
            {
              this.workQueues = (ws = (WorkQueue[])Arrays.copyOf(ws, n <<= 1));
              m = n - 1;
              probes = 0;
            }
          }
        }
        w.poolIndex = ((short)r);
        w.eventCount = r;
        ws[r] = w;
      }
    }
    finally
    {
      if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
        releasePlock(nps);
      }
    }
    wt.setName(this.workerNamePrefix.concat(Integer.toString(w.poolIndex >>> 1)));
    return w;
  }
  
  final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex)
  {
    WorkQueue w = null;
    if ((wt != null) && ((w = wt.workQueue) != null))
    {
      w.qlock = -1;
      long sc;
      while (!U.compareAndSwapLong(this, STEALCOUNT, sc = this.stealCount, sc + w.nsteals)) {}
      int ps;
      if ((((ps = this.plock) & 0x2) != 0) || (!U.compareAndSwapInt(this, PLOCK, , ps))) {
        ps = acquirePlock();
      }
      int nps = ps & 0x80000000 | ps + 2 & 0x7FFFFFFF;
      try
      {
        int idx = w.poolIndex;
        WorkQueue[] ws = this.workQueues;
        if ((ws != null) && (idx >= 0) && (idx < ws.length) && (ws[idx] == w)) {
          ws[idx] = null;
        }
      }
      finally
      {
        if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
          releasePlock(nps);
        }
      }
    }
    long c;
    while (!U.compareAndSwapLong(this, CTL, c = this.ctl, c - 281474976710656L & 0xFFFF000000000000 | c - 4294967296L & 0xFFFF00000000 | c & 0xFFFFFFFF)) {}
    if ((!tryTerminate(false, false)) && (w != null) && (w.array != null))
    {
      w.cancelAll();
      int u;
      int e;
      while (((u = (int)((c = this.ctl) >>> 32)) < 0) && ((e = (int)c) >= 0)) {
        if (e > 0)
        {
          WorkQueue[] ws;
          int i;
          WorkQueue v;
          if (((ws = this.workQueues) != null) && ((i = e & 0xFFFF) < ws.length) && ((v = ws[i]) != null))
          {
            long nc = v.nextWait & 0x7FFFFFFF | u + 65536 << 32;
            if (v.eventCount == (e | 0x80000000)) {
              if (U.compareAndSwapLong(this, CTL, c, nc))
              {
                v.eventCount = (e + 65536 & 0x7FFFFFFF);
                Thread p;
                if ((p = v.parker) == null) {
                  break;
                }
                U.unpark(p);
              }
            }
          }
        }
        else if ((short)u < 0)
        {
          tryAddWorker();
        }
      }
    }
    if (ex == null) {
      ForkJoinTask.helpExpungeStaleExceptions();
    } else {
      ForkJoinTask.rethrow(ex);
    }
  }
  
  static final class Submitter
  {
    int seed;
    
    Submitter(int s)
    {
      this.seed = s;
    }
  }
  
  final void externalPush(ForkJoinTask<?> task)
  {
    Submitter z = (Submitter)submitters.get();
    
    int ps = this.plock;
    WorkQueue[] ws = this.workQueues;
    int m;
    int r;
    WorkQueue q;
    if ((z != null) && (ps > 0) && (ws != null) && ((m = ws.length - 1) >= 0) && ((q = ws[(m & (r = z.seed) & 0x7E)]) != null) && (r != 0) && (U.compareAndSwapInt(q, QLOCK, 0, 1)))
    {
      ForkJoinTask<?>[] a;
      int am;
      int s;
      int n;
      if (((a = q.array) != null) && ((am = a.length - 1) > (n = (s = q.top) - q.base)))
      {
        int j = ((am & s) << ASHIFT) + ABASE;
        U.putOrderedObject(a, j, task);
        q.top = (s + 1);
        q.qlock = 0;
        if (n <= 1) {
          signalWork(ws, q);
        }
        return;
      }
      q.qlock = 0;
    }
    fullExternalPush(task);
  }
  
  private void fullExternalPush(ForkJoinTask<?> task)
  {
    int r = 0;
    Submitter z = (Submitter)submitters.get();
    for (;;)
    {
      if (z == null)
      {
        r += 1640531527;
        if ((U.compareAndSwapInt(this, INDEXSEED, r = this.indexSeed, r)) && (r != 0)) {
          submitters.set(z = new Submitter(r));
        }
      }
      else if (r == 0)
      {
        r = z.seed;
        r ^= r << 13;
        r ^= r >>> 17;
        z.seed = (r ^= r << 5);
      }
      int ps;
      if ((ps = this.plock) < 0) {
        throw new RejectedExecutionException();
      }
      WorkQueue[] ws;
      int m;
      WorkQueue[] ws;
      if ((ps == 0) || ((ws = this.workQueues) == null) || ((m = ws.length - 1) < 0))
      {
        int p = this.parallelism;
        int n = p > 1 ? p - 1 : 1;
        n |= n >>> 1;n |= n >>> 2;n |= n >>> 4;
        n |= n >>> 8;n |= n >>> 16;n = n + 1 << 1;
        WorkQueue[] nws = ((ws = this.workQueues) == null) || (ws.length == 0) ? new WorkQueue[n] : null;
        if ((((ps = this.plock) & 0x2) != 0) || (!U.compareAndSwapInt(this, PLOCK, , ps))) {
          ps = acquirePlock();
        }
        if ((((ws = this.workQueues) == null) || (ws.length == 0)) && (nws != null)) {
          this.workQueues = nws;
        }
        int nps = ps & 0x80000000 | ps + 2 & 0x7FFFFFFF;
        if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
          releasePlock(nps);
        }
      }
      else
      {
        int m;
        int k;
        WorkQueue q;
        if ((q = ws[(k = r & m & 0x7E)]) != null)
        {
          if ((q.qlock == 0) && (U.compareAndSwapInt(q, QLOCK, 0, 1)))
          {
            ForkJoinTask<?>[] a = q.array;
            int s = q.top;
            boolean submitted = false;
            try
            {
              if (((a != null) && (a.length > s + 1 - q.base)) || ((a = q.growArray()) != null))
              {
                int j = ((a.length - 1 & s) << ASHIFT) + ABASE;
                U.putOrderedObject(a, j, task);
                q.top = (s + 1);
                submitted = true;
              }
            }
            finally
            {
              q.qlock = 0;
            }
            if (submitted)
            {
              signalWork(ws, q);
              return;
            }
          }
          r = 0;
        }
        else if (((ps = this.plock) & 0x2) == 0)
        {
          q = new WorkQueue(this, null, -1, r);
          q.poolIndex = ((short)k);
          if ((((ps = this.plock) & 0x2) != 0) || (!U.compareAndSwapInt(this, PLOCK, , ps))) {
            ps = acquirePlock();
          }
          if (((ws = this.workQueues) != null) && (k < ws.length) && (ws[k] == null)) {
            ws[k] = q;
          }
          int nps = ps & 0x80000000 | ps + 2 & 0x7FFFFFFF;
          if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
            releasePlock(nps);
          }
        }
        else
        {
          r = 0;
        }
      }
    }
  }
  
  final void incrementActiveCount()
  {
    long c;
    while (!U.compareAndSwapLong(this, CTL, c = this.ctl, c & 0xFFFFFFFFFFFF | (c & 0xFFFF000000000000) + 281474976710656L)) {}
  }
  
  final void signalWork(WorkQueue[] ws, WorkQueue q)
  {
    long c;
    int u;
    while ((u = (int)((c = this.ctl) >>> 32)) < 0)
    {
      int e;
      if ((e = (int)c) <= 0)
      {
        if ((short)u < 0) {
          tryAddWorker();
        }
      }
      else
      {
        int i;
        WorkQueue w;
        if ((ws != null) && (ws.length > (i = e & 0xFFFF)) && ((w = ws[i]) != null))
        {
          long nc = w.nextWait & 0x7FFFFFFF | u + 65536 << 32;
          
          int ne = e + 65536 & 0x7FFFFFFF;
          if ((w.eventCount == (e | 0x80000000)) && (U.compareAndSwapLong(this, CTL, c, nc)))
          {
            w.eventCount = ne;
            Thread p;
            if ((p = w.parker) != null) {
              U.unpark(p);
            }
          }
          else
          {
            if ((q != null) && (q.base >= q.top)) {
              break;
            }
          }
        }
      }
    }
  }
  
  final void runWorker(WorkQueue w)
  {
    w.growArray();
    for (int r = w.hint; scan(w, r) == 0; r ^= r << 5)
    {
      r ^= r << 13;r ^= r >>> 17;
    }
  }
  
  private final int scan(WorkQueue w, int r)
  {
    long c = this.ctl;
    WorkQueue[] ws;
    int m;
    if (((ws = this.workQueues) != null) && ((m = ws.length - 1) >= 0) && (w != null))
    {
      int j = m + m + 1;int ec = w.eventCount;
      for (;;)
      {
        WorkQueue q;
        int b;
        ForkJoinTask<?>[] a;
        if (((q = ws[(r - j & m)]) != null) && ((b = q.base) - q.top < 0) && ((a = q.array) != null))
        {
          long i = ((a.length - 1 & b) << ASHIFT) + ABASE;
          ForkJoinTask<?> t;
          if ((t = (ForkJoinTask)U.getObjectVolatile(a, i)) == null) {
            break;
          }
          if (ec < 0)
          {
            helpRelease(c, ws, w, q, b); break;
          }
          if ((q.base != b) || (!U.compareAndSwapObject(a, i, t, null))) {
            break;
          }
          U.putOrderedInt(q, QBASE, b + 1);
          if (b + 1 - q.top < 0) {
            signalWork(ws, q);
          }
          w.runTask(t); break;
        }
        j--;
        if (j < 0)
        {
          int e;
          if ((ec | (e = (int)c)) < 0) {
            return awaitWork(w, c, ec);
          }
          if (this.ctl != c) {
            break;
          }
          long nc = ec | c - 281474976710656L & 0xFFFFFFFF00000000;
          w.nextWait = e;
          w.eventCount = (ec | 0x80000000);
          if (!U.compareAndSwapLong(this, CTL, c, nc)) {
            w.eventCount = ec;
          }
          break;
        }
      }
    }
    return 0;
  }
  
  private final int awaitWork(WorkQueue w, long c, int ec)
  {
    int stat;
    if (((stat = w.qlock) >= 0) && (w.eventCount == ec) && (this.ctl == c) && (!Thread.interrupted()))
    {
      int e = (int)c;
      int u = (int)(c >>> 32);
      int d = (u >> 16) + this.parallelism;
      if ((e < 0) || ((d <= 0) && (tryTerminate(false, false))))
      {
        stat = w.qlock = -1;
      }
      else
      {
        int ns;
        if ((ns = w.nsteals) != 0)
        {
          w.nsteals = 0;
          long sc;
          while (!U.compareAndSwapLong(this, STEALCOUNT, sc = this.stealCount, sc + ns)) {}
        }
        else
        {
          long pc = (d > 0) || (ec != (e | 0x80000000)) ? 0L : w.nextWait & 0x7FFFFFFF | u + 65536 << 32;
          long deadline;
          long deadline;
          long parkTime;
          if (pc != 0L)
          {
            int dc = -(short)(int)(c >>> 32);
            long parkTime = dc < 0 ? 200000000L : (dc + 1) * 2000000000L;
            
            deadline = System.nanoTime() + parkTime - 2000000L;
          }
          else
          {
            parkTime = deadline = 0L;
          }
          if ((w.eventCount == ec) && (this.ctl == c))
          {
            Thread wt = Thread.currentThread();
            U.putObject(wt, PARKBLOCKER, this);
            w.parker = wt;
            if ((w.eventCount == ec) && (this.ctl == c)) {
              U.park(false, parkTime);
            }
            w.parker = null;
            U.putObject(wt, PARKBLOCKER, null);
            if ((parkTime != 0L) && (this.ctl == c) && (deadline - System.nanoTime() <= 0L) && (U.compareAndSwapLong(this, CTL, c, pc))) {
              stat = w.qlock = -1;
            }
          }
        }
      }
    }
    return stat;
  }
  
  private final void helpRelease(long c, WorkQueue[] ws, WorkQueue w, WorkQueue q, int b)
  {
    int e;
    int i;
    WorkQueue v;
    if ((w != null) && (w.eventCount < 0) && ((e = (int)c) > 0) && (ws != null) && (ws.length > (i = e & 0xFFFF)) && ((v = ws[i]) != null) && (this.ctl == c))
    {
      long nc = v.nextWait & 0x7FFFFFFF | (int)(c >>> 32) + 65536 << 32;
      
      int ne = e + 65536 & 0x7FFFFFFF;
      if ((q != null) && (q.base == b) && (w.eventCount < 0) && (v.eventCount == (e | 0x80000000)) && (U.compareAndSwapLong(this, CTL, c, nc)))
      {
        v.eventCount = ne;
        Thread p;
        if ((p = v.parker) != null) {
          U.unpark(p);
        }
      }
    }
  }
  
  private int tryHelpStealer(WorkQueue joiner, ForkJoinTask<?> task)
  {
    int stat = 0;int steps = 0;
    if ((task != null) && (joiner != null) && (joiner.base - joiner.top >= 0))
    {
      break label107;
      label25:
      ForkJoinTask<?> subtask = task;
      WorkQueue j = joiner;
      label107:
      label471:
      label472:
      label474:
      for (;;)
      {
        int s;
        if ((s = task.status) < 0)
        {
          stat = s;
          return stat;
        }
        WorkQueue[] ws;
        int m;
        if (((ws = this.workQueues) == null) || ((m = ws.length - 1) <= 0)) {
          return stat;
        }
        int h;
        WorkQueue v;
        if (((v = ws[(h = (j.hint | 0x1) & m)]) == null) || (v.currentSteal != subtask))
        {
          int origin = h;
          if ((((h = h + 2 & m) & 0xF) == 1) && ((subtask.status < 0) || (j.currentJoin != subtask))) {
            break label25;
          }
          if (((v = ws[h]) != null) && (v.currentSteal == subtask))
          {
            j.hint = h;
          }
          else
          {
            if (h != origin) {
              break;
            }
            return stat;
          }
        }
        for (;;)
        {
          if (subtask.status < 0) {
            break label472;
          }
          int b;
          ForkJoinTask[] a;
          if (((b = v.base) - v.top < 0) && ((a = v.array) != null))
          {
            int i = ((a.length - 1 & b) << ASHIFT) + ABASE;
            ForkJoinTask<?> t = (ForkJoinTask)U.getObjectVolatile(a, i);
            if ((subtask.status < 0) || (j.currentJoin != subtask) || (v.currentSteal != subtask)) {
              break;
            }
            stat = 1;
            if (v.base == b)
            {
              if (t == null) {
                return stat;
              }
              if (U.compareAndSwapObject(a, i, t, null))
              {
                U.putOrderedInt(v, QBASE, b + 1);
                ForkJoinTask<?> ps = joiner.currentSteal;
                int jt = joiner.top;
                do
                {
                  joiner.currentSteal = t;
                  t.doExec();
                } while ((task.status >= 0) && (joiner.top != jt) && ((t = joiner.pop()) != null));
                joiner.currentSteal = ps;
                return stat;
              }
            }
            break label471;
          }
          ForkJoinTask<?> next = v.currentJoin;
          if ((subtask.status < 0) || (j.currentJoin != subtask) || (v.currentSteal != subtask)) {
            break;
          }
          if (next == null) {
            return stat;
          }
          steps++;
          if (steps == 64) {
            return stat;
          }
          subtask = next;
          j = v;
          break label474;
        }
        break label25;
      }
    }
    return stat;
  }
  
  private int helpComplete(WorkQueue joiner, CountedCompleter<?> task)
  {
    int s = 0;
    WorkQueue[] ws;
    int m;
    if (((ws = this.workQueues) != null) && ((m = ws.length - 1) >= 0) && (joiner != null) && (task != null))
    {
      int j = joiner.poolIndex;
      int scans = m + m + 1;
      long c = 0L;
      int k = scans;
      for (; (s = task.status) >= 0; j += 2) {
        if (joiner.internalPopAndExecCC(task))
        {
          k = scans;
        }
        else
        {
          if ((s = task.status) < 0) {
            break;
          }
          WorkQueue q;
          if (((q = ws[(j & m)]) != null) && (q.pollAndExecCC(task)))
          {
            k = scans;
          }
          else
          {
            k--;
            if (k < 0)
            {
              if (c == (c = this.ctl)) {
                break;
              }
              k = scans;
            }
          }
        }
      }
    }
    return s;
  }
  
  final boolean tryCompensate(long c)
  {
    WorkQueue[] ws = this.workQueues;
    int pc = this.parallelism;int e = (int)c;
    int m;
    if ((ws != null) && ((m = ws.length - 1) >= 0) && (e >= 0) && (this.ctl == c))
    {
      WorkQueue w = ws[(e & m)];
      if ((e != 0) && (w != null))
      {
        long nc = w.nextWait & 0x7FFFFFFF | c & 0xFFFFFFFF00000000;
        
        int ne = e + 65536 & 0x7FFFFFFF;
        if ((w.eventCount == (e | 0x80000000)) && (U.compareAndSwapLong(this, CTL, c, nc)))
        {
          w.eventCount = ne;
          Thread p;
          if ((p = w.parker) != null) {
            U.unpark(p);
          }
          return true;
        }
      }
      else
      {
        int tc;
        if (((tc = (short)(int)(c >>> 32)) >= 0) && ((int)(c >> 48) + pc > 1))
        {
          long nc = c - 281474976710656L & 0xFFFF000000000000 | c & 0xFFFFFFFFFFFF;
          if (U.compareAndSwapLong(this, CTL, c, nc)) {
            return true;
          }
        }
        else if (tc + pc < 32767)
        {
          long nc = c + 4294967296L & 0xFFFF00000000 | c & 0xFFFF0000FFFFFFFF;
          if (U.compareAndSwapLong(this, CTL, c, nc))
          {
            Throwable ex = null;
            ForkJoinWorkerThread wt = null;
            try
            {
              ForkJoinWorkerThreadFactory fac;
              if (((fac = this.factory) != null) && ((wt = fac.newThread(this)) != null))
              {
                wt.start();
                return true;
              }
            }
            catch (Throwable rex)
            {
              ex = rex;
            }
            deregisterWorker(wt, ex);
          }
        }
      }
    }
    return false;
  }
  
  final int awaitJoin(WorkQueue joiner, ForkJoinTask<?> task)
  {
    int s = 0;
    if ((task != null) && ((s = task.status) >= 0) && (joiner != null))
    {
      ForkJoinTask<?> prevJoin = joiner.currentJoin;
      joiner.currentJoin = task;
      while ((joiner.tryRemoveAndExec(task)) && ((s = task.status) >= 0)) {}
      if ((s >= 0) && ((task instanceof CountedCompleter))) {
        s = helpComplete(joiner, (CountedCompleter)task);
      }
      long cc = 0L;
      while ((s >= 0) && ((s = task.status) >= 0)) {
        if (((s = tryHelpStealer(joiner, task)) == 0) && ((s = task.status) >= 0)) {
          if (!tryCompensate(cc))
          {
            cc = this.ctl;
          }
          else
          {
            if ((task.trySetSignal()) && ((s = task.status) >= 0)) {
              synchronized (task)
              {
                if (task.status >= 0) {
                  try
                  {
                    task.wait();
                  }
                  catch (InterruptedException ie) {}
                } else {
                  task.notifyAll();
                }
              }
            }
            long c;
            while (!U.compareAndSwapLong(this, CTL, c = this.ctl, c & 0xFFFFFFFFFFFF | (c & 0xFFFF000000000000) + 281474976710656L)) {}
          }
        }
      }
      joiner.currentJoin = prevJoin;
    }
    return s;
  }
  
  final void helpJoinOnce(WorkQueue joiner, ForkJoinTask<?> task)
  {
    int s;
    if ((joiner != null) && (task != null) && ((s = task.status) >= 0))
    {
      ForkJoinTask<?> prevJoin = joiner.currentJoin;
      joiner.currentJoin = task;
      while ((joiner.tryRemoveAndExec(task)) && ((s = task.status) >= 0)) {}
      if (s >= 0)
      {
        if ((task instanceof CountedCompleter)) {
          helpComplete(joiner, (CountedCompleter)task);
        }
        while ((task.status >= 0) && (tryHelpStealer(joiner, task) > 0)) {}
      }
      joiner.currentJoin = prevJoin;
    }
  }
  
  private WorkQueue findNonEmptyStealQueue()
  {
    int r = ThreadLocalRandom.current().nextInt();
    for (;;)
    {
      int ps = this.plock;
      WorkQueue[] ws;
      int m;
      if (((ws = this.workQueues) != null) && ((m = ws.length - 1) >= 0)) {
        for (int j = m + 1 << 2; j >= 0; j--)
        {
          WorkQueue q;
          if (((q = ws[((r - j << 1 | 0x1) & m)]) != null) && (q.base - q.top < 0)) {
            return q;
          }
        }
      }
      if (this.plock == ps) {
        return null;
      }
    }
  }
  
  final void helpQuiescePool(WorkQueue w)
  {
    ForkJoinTask<?> ps = w.currentSteal;
    boolean active = true;
    for (;;)
    {
      ForkJoinTask<?> t;
      if ((t = w.nextLocalTask()) != null)
      {
        t.doExec();
      }
      else
      {
        WorkQueue q;
        if ((q = findNonEmptyStealQueue()) != null)
        {
          if (!active)
          {
            active = true;
            long c;
            while (!U.compareAndSwapLong(this, CTL, c = this.ctl, c & 0xFFFFFFFFFFFF | (c & 0xFFFF000000000000) + 281474976710656L)) {}
          }
          int b;
          if (((b = q.base) - q.top < 0) && ((t = q.pollAt(b)) != null))
          {
            (w.currentSteal = t).doExec();
            w.currentSteal = ps;
          }
        }
        else if (active)
        {
          long c;
          long nc = (c = this.ctl) & 0xFFFFFFFFFFFF | (c & 0xFFFF000000000000) - 281474976710656L;
          if ((int)(nc >> 48) + this.parallelism == 0) {
            break;
          }
          if (U.compareAndSwapLong(this, CTL, c, nc)) {
            active = false;
          }
        }
        else
        {
          long c;
          if (((int)((c = this.ctl) >> 48) + this.parallelism <= 0) && (U.compareAndSwapLong(this, CTL, c, c & 0xFFFFFFFFFFFF | (c & 0xFFFF000000000000) + 281474976710656L))) {
            break;
          }
        }
      }
    }
  }
  
  final ForkJoinTask<?> nextTaskFor(WorkQueue w)
  {
    for (;;)
    {
      ForkJoinTask<?> t;
      if ((t = w.nextLocalTask()) != null) {
        return t;
      }
      WorkQueue q;
      if ((q = findNonEmptyStealQueue()) == null) {
        return null;
      }
      int b;
      if (((b = q.base) - q.top < 0) && ((t = q.pollAt(b)) != null)) {
        return t;
      }
    }
  }
  
  static int getSurplusQueuedTaskCount()
  {
    Thread t;
    if (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread))
    {
      ForkJoinWorkerThread wt;
      ForkJoinPool pool;
      int p = (pool = (wt = (ForkJoinWorkerThread)t).pool).parallelism;
      WorkQueue q;
      int n = (q = wt.workQueue).top - q.base;
      int a = (int)(pool.ctl >> 48) + p;
      return n - (a > p >>>= 1 ? 4 : a > p >>>= 1 ? 2 : a > p >>>= 1 ? 1 : a > p >>>= 1 ? 0 : 8);
    }
    return 0;
  }
  
  private boolean tryTerminate(boolean now, boolean enable)
  {
    if (this == common) {
      return false;
    }
    int ps;
    if ((ps = this.plock) >= 0)
    {
      if (!enable) {
        return false;
      }
      if (((ps & 0x2) != 0) || (!U.compareAndSwapInt(this, PLOCK, , ps))) {
        ps = acquirePlock();
      }
      int nps = ps + 2 & 0x7FFFFFFF | 0x80000000;
      if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
        releasePlock(nps);
      }
    }
    for (;;)
    {
      long c;
      if (((c = this.ctl) & 0x80000000) != 0L)
      {
        if ((short)(int)(c >>> 32) + this.parallelism <= 0) {
          synchronized (this)
          {
            notifyAll();
          }
        }
        return true;
      }
      if (!now)
      {
        if ((int)(c >> 48) + this.parallelism > 0) {
          return false;
        }
        WorkQueue[] ws;
        if ((ws = this.workQueues) != null) {
          for (int i = 0; i < ws.length; i++)
          {
            WorkQueue w;
            if (((w = ws[i]) != null) && ((!w.isEmpty()) || (((i & 0x1) != 0) && (w.eventCount >= 0))))
            {
              signalWork(ws, w);
              return false;
            }
          }
        }
      }
      if (U.compareAndSwapLong(this, CTL, c, c | 0x80000000)) {
        for (int pass = 0; pass < 3; pass++)
        {
          WorkQueue[] ws;
          if ((ws = this.workQueues) != null)
          {
            int n = ws.length;
            for (int i = 0; i < n; i++)
            {
              WorkQueue w;
              if ((w = ws[i]) != null)
              {
                w.qlock = -1;
                if (pass > 0)
                {
                  w.cancelAll();
                  Thread wt;
                  if ((pass > 1) && ((wt = w.owner) != null))
                  {
                    if (!wt.isInterrupted()) {
                      try
                      {
                        wt.interrupt();
                      }
                      catch (Throwable ignore) {}
                    }
                    U.unpark(wt);
                  }
                }
              }
            }
            long cc;
            int e;
            int i;
            WorkQueue w;
            while (((e = (int)(cc = this.ctl) & 0x7FFFFFFF) != 0) && ((i = e & 0xFFFF) < n) && (i >= 0) && ((w = ws[i]) != null))
            {
              long nc = w.nextWait & 0x7FFFFFFF | cc + 281474976710656L & 0xFFFF000000000000 | cc & 0xFFFF80000000;
              if ((w.eventCount == (e | 0x80000000)) && (U.compareAndSwapLong(this, CTL, cc, nc)))
              {
                w.eventCount = (e + 65536 & 0x7FFFFFFF);
                w.qlock = -1;
                Thread p;
                if ((p = w.parker) != null) {
                  U.unpark(p);
                }
              }
            }
          }
        }
      }
    }
  }
  
  static WorkQueue commonSubmitterQueue()
  {
    Submitter z;
    ForkJoinPool p;
    WorkQueue[] ws;
    int m;
    return ((z = (Submitter)submitters.get()) != null) && ((p = common) != null) && ((ws = p.workQueues) != null) && ((m = ws.length - 1) >= 0) ? ws[(m & z.seed & 0x7E)] : null;
  }
  
  final boolean tryExternalUnpush(ForkJoinTask<?> task)
  {
    Submitter z = (Submitter)submitters.get();
    WorkQueue[] ws = this.workQueues;
    boolean popped = false;
    int m;
    WorkQueue joiner;
    int s;
    ForkJoinTask<?>[] a;
    if ((z != null) && (ws != null) && ((m = ws.length - 1) >= 0) && ((joiner = ws[(z.seed & m & 0x7E)]) != null) && (joiner.base != (s = joiner.top)) && ((a = joiner.array) != null))
    {
      long j = ((a.length - 1 & s - 1) << ASHIFT) + ABASE;
      if ((U.getObject(a, j) == task) && (U.compareAndSwapInt(joiner, QLOCK, 0, 1)))
      {
        if ((joiner.top == s) && (joiner.array == a) && (U.compareAndSwapObject(a, j, task, null)))
        {
          joiner.top = (s - 1);
          popped = true;
        }
        joiner.qlock = 0;
      }
    }
    return popped;
  }
  
  final int externalHelpComplete(CountedCompleter<?> task)
  {
    Submitter z = (Submitter)submitters.get();
    WorkQueue[] ws = this.workQueues;
    int s = 0;
    int m;
    int j;
    WorkQueue joiner;
    if ((z != null) && (ws != null) && ((m = ws.length - 1) >= 0) && ((joiner = ws[((j = z.seed) & m & 0x7E)]) != null) && (task != null))
    {
      int scans = m + m + 1;
      long c = 0L;
      j |= 0x1;
      int k = scans;
      for (; (s = task.status) >= 0; j += 2) {
        if (joiner.externalPopAndExecCC(task))
        {
          k = scans;
        }
        else
        {
          if ((s = task.status) < 0) {
            break;
          }
          WorkQueue q;
          if (((q = ws[(j & m)]) != null) && (q.pollAndExecCC(task)))
          {
            k = scans;
          }
          else
          {
            k--;
            if (k < 0)
            {
              if (c == (c = this.ctl)) {
                break;
              }
              k = scans;
            }
          }
        }
      }
    }
    return s;
  }
  
  public ForkJoinPool()
  {
    this(Math.min(32767, Runtime.getRuntime().availableProcessors()), defaultForkJoinWorkerThreadFactory, null, false);
  }
  
  public ForkJoinPool(int parallelism)
  {
    this(parallelism, defaultForkJoinWorkerThreadFactory, null, false);
  }
  
  public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, Thread.UncaughtExceptionHandler handler, boolean asyncMode)
  {
    this(checkParallelism(parallelism), checkFactory(factory), handler, asyncMode ? 1 : 0, "ForkJoinPool-" + nextPoolId() + "-worker-");
    
    checkPermission();
  }
  
  private static int checkParallelism(int parallelism)
  {
    if ((parallelism <= 0) || (parallelism > 32767)) {
      throw new IllegalArgumentException();
    }
    return parallelism;
  }
  
  private static ForkJoinWorkerThreadFactory checkFactory(ForkJoinWorkerThreadFactory factory)
  {
    if (factory == null) {
      throw new NullPointerException();
    }
    return factory;
  }
  
  private ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, Thread.UncaughtExceptionHandler handler, int mode, String workerNamePrefix)
  {
    this.workerNamePrefix = workerNamePrefix;
    this.factory = factory;
    this.ueh = handler;
    this.mode = ((short)mode);
    this.parallelism = ((short)parallelism);
    long np = -parallelism;
    this.ctl = (np << 48 & 0xFFFF000000000000 | np << 32 & 0xFFFF00000000);
  }
  
  public static ForkJoinPool commonPool()
  {
    return common;
  }
  
  public <T> T invoke(ForkJoinTask<T> task)
  {
    if (task == null) {
      throw new NullPointerException();
    }
    externalPush(task);
    return (T)task.join();
  }
  
  public void execute(ForkJoinTask<?> task)
  {
    if (task == null) {
      throw new NullPointerException();
    }
    externalPush(task);
  }
  
  public void execute(Runnable task)
  {
    if (task == null) {
      throw new NullPointerException();
    }
    ForkJoinTask<?> job;
    ForkJoinTask<?> job;
    if ((task instanceof ForkJoinTask)) {
      job = (ForkJoinTask)task;
    } else {
      job = new ForkJoinTask.RunnableExecuteAction(task);
    }
    externalPush(job);
  }
  
  public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task)
  {
    if (task == null) {
      throw new NullPointerException();
    }
    externalPush(task);
    return task;
  }
  
  public <T> ForkJoinTask<T> submit(Callable<T> task)
  {
    ForkJoinTask<T> job = new ForkJoinTask.AdaptedCallable(task);
    externalPush(job);
    return job;
  }
  
  public <T> ForkJoinTask<T> submit(Runnable task, T result)
  {
    ForkJoinTask<T> job = new ForkJoinTask.AdaptedRunnable(task, result);
    externalPush(job);
    return job;
  }
  
  public ForkJoinTask<?> submit(Runnable task)
  {
    if (task == null) {
      throw new NullPointerException();
    }
    ForkJoinTask<?> job;
    ForkJoinTask<?> job;
    if ((task instanceof ForkJoinTask)) {
      job = (ForkJoinTask)task;
    } else {
      job = new ForkJoinTask.AdaptedRunnableAction(task);
    }
    externalPush(job);
    return job;
  }
  
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
  {
    ArrayList<Future<T>> futures = new ArrayList(tasks.size());
    
    boolean done = false;
    try
    {
      for (Callable<T> t : tasks)
      {
        ForkJoinTask<T> f = new ForkJoinTask.AdaptedCallable(t);
        futures.add(f);
        externalPush(f);
      }
      int i = 0;
      for (int size = futures.size(); i < size; i++) {
        ((ForkJoinTask)futures.get(i)).quietlyJoin();
      }
      done = true;
      int i;
      int size;
      return futures;
    }
    finally
    {
      if (!done)
      {
        int i = 0;
        for (int size = futures.size(); i < size; i++) {
          ((Future)futures.get(i)).cancel(false);
        }
      }
    }
  }
  
  public ForkJoinWorkerThreadFactory getFactory()
  {
    return this.factory;
  }
  
  public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler()
  {
    return this.ueh;
  }
  
  public int getParallelism()
  {
    int par;
    return (par = this.parallelism) > 0 ? par : 1;
  }
  
  public static int getCommonPoolParallelism()
  {
    return commonParallelism;
  }
  
  public int getPoolSize()
  {
    return this.parallelism + (short)(int)(this.ctl >>> 32);
  }
  
  public boolean getAsyncMode()
  {
    return this.mode == 1;
  }
  
  public int getRunningThreadCount()
  {
    int rc = 0;
    WorkQueue[] ws;
    if ((ws = this.workQueues) != null) {
      for (int i = 1; i < ws.length; i += 2)
      {
        WorkQueue w;
        if (((w = ws[i]) != null) && (w.isApparentlyUnblocked())) {
          rc++;
        }
      }
    }
    return rc;
  }
  
  public int getActiveThreadCount()
  {
    int r = this.parallelism + (int)(this.ctl >> 48);
    return r <= 0 ? 0 : r;
  }
  
  public boolean isQuiescent()
  {
    return this.parallelism + (int)(this.ctl >> 48) <= 0;
  }
  
  public long getStealCount()
  {
    long count = this.stealCount;
    WorkQueue[] ws;
    if ((ws = this.workQueues) != null) {
      for (int i = 1; i < ws.length; i += 2)
      {
        WorkQueue w;
        if ((w = ws[i]) != null) {
          count += w.nsteals;
        }
      }
    }
    return count;
  }
  
  public long getQueuedTaskCount()
  {
    long count = 0L;
    WorkQueue[] ws;
    if ((ws = this.workQueues) != null) {
      for (int i = 1; i < ws.length; i += 2)
      {
        WorkQueue w;
        if ((w = ws[i]) != null) {
          count += w.queueSize();
        }
      }
    }
    return count;
  }
  
  public int getQueuedSubmissionCount()
  {
    int count = 0;
    WorkQueue[] ws;
    if ((ws = this.workQueues) != null) {
      for (int i = 0; i < ws.length; i += 2)
      {
        WorkQueue w;
        if ((w = ws[i]) != null) {
          count += w.queueSize();
        }
      }
    }
    return count;
  }
  
  public boolean hasQueuedSubmissions()
  {
    WorkQueue[] ws;
    if ((ws = this.workQueues) != null) {
      for (int i = 0; i < ws.length; i += 2)
      {
        WorkQueue w;
        if (((w = ws[i]) != null) && (!w.isEmpty())) {
          return true;
        }
      }
    }
    return false;
  }
  
  protected ForkJoinTask<?> pollSubmission()
  {
    WorkQueue[] ws;
    if ((ws = this.workQueues) != null) {
      for (int i = 0; i < ws.length; i += 2)
      {
        WorkQueue w;
        ForkJoinTask<?> t;
        if (((w = ws[i]) != null) && ((t = w.poll()) != null)) {
          return t;
        }
      }
    }
    return null;
  }
  
  protected int drainTasksTo(Collection<? super ForkJoinTask<?>> c)
  {
    int count = 0;
    WorkQueue[] ws;
    if ((ws = this.workQueues) != null) {
      for (int i = 0; i < ws.length; i++)
      {
        WorkQueue w;
        if ((w = ws[i]) != null)
        {
          ForkJoinTask<?> t;
          while ((t = w.poll()) != null)
          {
            c.add(t);
            count++;
          }
        }
      }
    }
    return count;
  }
  
  public String toString()
  {
    long qt = 0L;long qs = 0L;int rc = 0;
    long st = this.stealCount;
    long c = this.ctl;
    WorkQueue[] ws;
    if ((ws = this.workQueues) != null) {
      for (int i = 0; i < ws.length; i++)
      {
        WorkQueue w;
        if ((w = ws[i]) != null)
        {
          int size = w.queueSize();
          if ((i & 0x1) == 0)
          {
            qs += size;
          }
          else
          {
            qt += size;
            st += w.nsteals;
            if (w.isApparentlyUnblocked()) {
              rc++;
            }
          }
        }
      }
    }
    int pc = this.parallelism;
    int tc = pc + (short)(int)(c >>> 32);
    int ac = pc + (int)(c >> 48);
    if (ac < 0) {
      ac = 0;
    }
    String level;
    String level;
    if ((c & 0x80000000) != 0L) {
      level = tc == 0 ? "Terminated" : "Terminating";
    } else {
      level = this.plock < 0 ? "Shutting down" : "Running";
    }
    return super.toString() + "[" + level + ", parallelism = " + pc + ", size = " + tc + ", active = " + ac + ", running = " + rc + ", steals = " + st + ", tasks = " + qt + ", submissions = " + qs + "]";
  }
  
  public void shutdown()
  {
    checkPermission();
    tryTerminate(false, true);
  }
  
  public List<Runnable> shutdownNow()
  {
    checkPermission();
    tryTerminate(true, true);
    return Collections.emptyList();
  }
  
  public boolean isTerminated()
  {
    long c = this.ctl;
    return ((c & 0x80000000) != 0L) && ((short)(int)(c >>> 32) + this.parallelism <= 0);
  }
  
  public boolean isTerminating()
  {
    long c = this.ctl;
    return ((c & 0x80000000) != 0L) && ((short)(int)(c >>> 32) + this.parallelism > 0);
  }
  
  public boolean isShutdown()
  {
    return this.plock < 0;
  }
  
  public boolean awaitTermination(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    if (this == common)
    {
      awaitQuiescence(timeout, unit);
      return false;
    }
    long nanos = unit.toNanos(timeout);
    if (isTerminated()) {
      return true;
    }
    if (nanos <= 0L) {
      return false;
    }
    long deadline = System.nanoTime() + nanos;
    synchronized (this)
    {
      if (isTerminated()) {
        return true;
      }
      if (nanos <= 0L) {
        return false;
      }
      long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
      wait(millis > 0L ? millis : 1L);
      nanos = deadline - System.nanoTime();
    }
  }
  
  public boolean awaitQuiescence(long timeout, TimeUnit unit)
  {
    long nanos = unit.toNanos(timeout);
    
    Thread thread = Thread.currentThread();
    ForkJoinWorkerThread wt;
    if (((thread instanceof ForkJoinWorkerThread)) && ((wt = (ForkJoinWorkerThread)thread).pool == this))
    {
      helpQuiescePool(wt.workQueue);
      return true;
    }
    long startTime = System.nanoTime();
    
    int r = 0;
    boolean found = true;
    WorkQueue[] ws;
    int m;
    while ((!isQuiescent()) && ((ws = this.workQueues) != null) && ((m = ws.length - 1) >= 0))
    {
      if (!found)
      {
        if (System.nanoTime() - startTime > nanos) {
          return false;
        }
        Thread.yield();
      }
      found = false;
      for (int j = m + 1 << 2; j >= 0; j--)
      {
        WorkQueue q;
        int b;
        if (((q = ws[(r++ & m)]) != null) && ((b = q.base) - q.top < 0))
        {
          found = true;
          ForkJoinTask<?> t;
          if ((t = q.pollAt(b)) == null) {
            break;
          }
          t.doExec(); break;
        }
      }
    }
    return true;
  }
  
  static void quiesceCommonPool()
  {
    common.awaitQuiescence(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
  }
  
  public static void managedBlock(ManagedBlocker blocker)
    throws InterruptedException
  {
    Thread t = Thread.currentThread();
    if ((t instanceof ForkJoinWorkerThread))
    {
      ForkJoinPool p = ((ForkJoinWorkerThread)t).pool;
      while (!blocker.isReleasable()) {
        if (p.tryCompensate(p.ctl)) {
          try
          {
            do
            {
              if (blocker.isReleasable()) {
                break;
              }
            } while (!blocker.block());
          }
          finally
          {
            p.incrementActiveCount();
          }
        }
      }
    }
    while ((!blocker.isReleasable()) && (!blocker.block())) {}
  }
  
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value)
  {
    return new ForkJoinTask.AdaptedRunnable(runnable, value);
  }
  
  protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable)
  {
    return new ForkJoinTask.AdaptedCallable(callable);
  }
  
  static
  {
    try
    {
      U = getUnsafe();
      Class<?> k = ForkJoinPool.class;
      CTL = U.objectFieldOffset(k.getDeclaredField("ctl"));
      
      STEALCOUNT = U.objectFieldOffset(k.getDeclaredField("stealCount"));
      
      PLOCK = U.objectFieldOffset(k.getDeclaredField("plock"));
      
      INDEXSEED = U.objectFieldOffset(k.getDeclaredField("indexSeed"));
      
      Class<?> tk = Thread.class;
      PARKBLOCKER = U.objectFieldOffset(tk.getDeclaredField("parkBlocker"));
      
      Class<?> wk = WorkQueue.class;
      QBASE = U.objectFieldOffset(wk.getDeclaredField("base"));
      
      QLOCK = U.objectFieldOffset(wk.getDeclaredField("qlock"));
      
      Class<?> ak = ForkJoinTask[].class;
      ABASE = U.arrayBaseOffset(ak);
      int scale = U.arrayIndexScale(ak);
      if ((scale & scale - 1) != 0) {
        throw new Error("data type scale not a power of two");
      }
      ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
    }
    catch (Exception e)
    {
      throw new Error(e);
    }
    submitters = new ThreadLocal();
    defaultForkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory();
    
    modifyThreadPermission = new RuntimePermission("modifyThread");
    
    common = (ForkJoinPool)AccessController.doPrivileged(new PrivilegedAction()
    {
      public ForkJoinPool run()
      {
        return ForkJoinPool.access$100();
      }
    });
    int par = common.parallelism;
    commonParallelism = par > 0 ? par : 1;
  }
  
  private static ForkJoinPool makeCommonPool()
  {
    int parallelism = -1;
    ForkJoinWorkerThreadFactory factory = defaultForkJoinWorkerThreadFactory;
    
    Thread.UncaughtExceptionHandler handler = null;
    try
    {
      String pp = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
      
      String fp = System.getProperty("java.util.concurrent.ForkJoinPool.common.threadFactory");
      
      String hp = System.getProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
      if (pp != null) {
        parallelism = Integer.parseInt(pp);
      }
      if (fp != null) {
        factory = (ForkJoinWorkerThreadFactory)ClassLoader.getSystemClassLoader().loadClass(fp).newInstance();
      }
      if (hp != null) {
        handler = (Thread.UncaughtExceptionHandler)ClassLoader.getSystemClassLoader().loadClass(hp).newInstance();
      }
    }
    catch (Exception ignore) {}
    if ((parallelism < 0) && ((parallelism = Runtime.getRuntime().availableProcessors() - 1) < 0)) {
      parallelism = 0;
    }
    if (parallelism > 32767) {
      parallelism = 32767;
    }
    return new ForkJoinPool(parallelism, factory, handler, 0, "ForkJoinPool.commonPool-worker-");
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
  
  public static abstract interface ManagedBlocker
  {
    public abstract boolean block()
      throws InterruptedException;
    
    public abstract boolean isReleasable();
  }
}
