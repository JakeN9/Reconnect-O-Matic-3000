package io.netty.util.concurrent;

import io.netty.util.Signal;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.ArrayDeque;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public class DefaultPromise<V>
  extends AbstractFuture<V>
  implements Promise<V>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultPromise.class);
  private static final InternalLogger rejectedExecutionLogger = InternalLoggerFactory.getInstance(DefaultPromise.class.getName() + ".rejectedExecution");
  private static final int MAX_LISTENER_STACK_DEPTH = 8;
  private static final Signal SUCCESS = Signal.valueOf(DefaultPromise.class, "SUCCESS");
  private static final Signal UNCANCELLABLE = Signal.valueOf(DefaultPromise.class, "UNCANCELLABLE");
  private static final CauseHolder CANCELLATION_CAUSE_HOLDER = new CauseHolder(new CancellationException());
  EventExecutor executor;
  private volatile Object result;
  private Object listeners;
  private DefaultPromise<V>.LateListeners lateListeners;
  private short waiters;
  
  static
  {
    CANCELLATION_CAUSE_HOLDER.cause.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
  }
  
  public DefaultPromise(EventExecutor executor)
  {
    if (executor == null) {
      throw new NullPointerException("executor");
    }
    this.executor = executor;
  }
  
  protected DefaultPromise()
  {
    this.executor = null;
  }
  
  protected EventExecutor executor()
  {
    return this.executor;
  }
  
  public boolean isCancelled()
  {
    return isCancelled0(this.result);
  }
  
  private static boolean isCancelled0(Object result)
  {
    return ((result instanceof CauseHolder)) && ((((CauseHolder)result).cause instanceof CancellationException));
  }
  
  public boolean isCancellable()
  {
    return this.result == null;
  }
  
  public boolean isDone()
  {
    return isDone0(this.result);
  }
  
  private static boolean isDone0(Object result)
  {
    return (result != null) && (result != UNCANCELLABLE);
  }
  
  public boolean isSuccess()
  {
    Object result = this.result;
    if ((result == null) || (result == UNCANCELLABLE)) {
      return false;
    }
    return !(result instanceof CauseHolder);
  }
  
  public Throwable cause()
  {
    Object result = this.result;
    if ((result instanceof CauseHolder)) {
      return ((CauseHolder)result).cause;
    }
    return null;
  }
  
  public Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener)
  {
    if (listener == null) {
      throw new NullPointerException("listener");
    }
    if (isDone())
    {
      notifyLateListener(listener);
      return this;
    }
    synchronized (this)
    {
      if (!isDone())
      {
        if (this.listeners == null)
        {
          this.listeners = listener;
        }
        else if ((this.listeners instanceof DefaultFutureListeners))
        {
          ((DefaultFutureListeners)this.listeners).add(listener);
        }
        else
        {
          GenericFutureListener<? extends Future<V>> firstListener = (GenericFutureListener)this.listeners;
          
          this.listeners = new DefaultFutureListeners(firstListener, listener);
        }
        return this;
      }
    }
    notifyLateListener(listener);
    return this;
  }
  
  public Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners)
  {
    if (listeners == null) {
      throw new NullPointerException("listeners");
    }
    for (GenericFutureListener<? extends Future<? super V>> l : listeners)
    {
      if (l == null) {
        break;
      }
      addListener(l);
    }
    return this;
  }
  
  public Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener)
  {
    if (listener == null) {
      throw new NullPointerException("listener");
    }
    if (isDone()) {
      return this;
    }
    synchronized (this)
    {
      if (!isDone()) {
        if ((this.listeners instanceof DefaultFutureListeners)) {
          ((DefaultFutureListeners)this.listeners).remove(listener);
        } else if (this.listeners == listener) {
          this.listeners = null;
        }
      }
    }
    return this;
  }
  
  public Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners)
  {
    if (listeners == null) {
      throw new NullPointerException("listeners");
    }
    for (GenericFutureListener<? extends Future<? super V>> l : listeners)
    {
      if (l == null) {
        break;
      }
      removeListener(l);
    }
    return this;
  }
  
  public Promise<V> sync()
    throws InterruptedException
  {
    await();
    rethrowIfFailed();
    return this;
  }
  
  public Promise<V> syncUninterruptibly()
  {
    awaitUninterruptibly();
    rethrowIfFailed();
    return this;
  }
  
  private void rethrowIfFailed()
  {
    Throwable cause = cause();
    if (cause == null) {
      return;
    }
    PlatformDependent.throwException(cause);
  }
  
  public Promise<V> await()
    throws InterruptedException
  {
    if (isDone()) {
      return this;
    }
    if (Thread.interrupted()) {
      throw new InterruptedException(toString());
    }
    synchronized (this)
    {
      while (!isDone())
      {
        checkDeadLock();
        incWaiters();
        try
        {
          wait();
        }
        finally
        {
          decWaiters();
        }
      }
    }
    return this;
  }
  
  public boolean await(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    return await0(unit.toNanos(timeout), true);
  }
  
  public boolean await(long timeoutMillis)
    throws InterruptedException
  {
    return await0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), true);
  }
  
  public Promise<V> awaitUninterruptibly()
  {
    if (isDone()) {
      return this;
    }
    boolean interrupted = false;
    synchronized (this)
    {
      while (!isDone())
      {
        checkDeadLock();
        incWaiters();
        try
        {
          wait();
        }
        catch (InterruptedException e)
        {
          interrupted = true;
        }
        finally
        {
          decWaiters();
        }
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    return this;
  }
  
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit)
  {
    try
    {
      return await0(unit.toNanos(timeout), false);
    }
    catch (InterruptedException e)
    {
      throw new InternalError();
    }
  }
  
  public boolean awaitUninterruptibly(long timeoutMillis)
  {
    try
    {
      return await0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), false);
    }
    catch (InterruptedException e)
    {
      throw new InternalError();
    }
  }
  
  protected void checkDeadLock()
  {
    EventExecutor e = executor();
    if ((e != null) && (e.inEventLoop())) {
      throw new BlockingOperationException(toString());
    }
  }
  
  public Promise<V> setSuccess(V result)
  {
    if (setSuccess0(result))
    {
      notifyListeners();
      return this;
    }
    throw new IllegalStateException("complete already: " + this);
  }
  
  public boolean trySuccess(V result)
  {
    if (setSuccess0(result))
    {
      notifyListeners();
      return true;
    }
    return false;
  }
  
  public Promise<V> setFailure(Throwable cause)
  {
    if (setFailure0(cause))
    {
      notifyListeners();
      return this;
    }
    throw new IllegalStateException("complete already: " + this, cause);
  }
  
  public boolean tryFailure(Throwable cause)
  {
    if (setFailure0(cause))
    {
      notifyListeners();
      return true;
    }
    return false;
  }
  
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    Object result = this.result;
    if ((isDone0(result)) || (result == UNCANCELLABLE)) {
      return false;
    }
    synchronized (this)
    {
      result = this.result;
      if ((isDone0(result)) || (result == UNCANCELLABLE)) {
        return false;
      }
      this.result = CANCELLATION_CAUSE_HOLDER;
      if (hasWaiters()) {
        notifyAll();
      }
    }
    notifyListeners();
    return true;
  }
  
  public boolean setUncancellable()
  {
    Object result = this.result;
    if (isDone0(result)) {
      return !isCancelled0(result);
    }
    synchronized (this)
    {
      result = this.result;
      if (isDone0(result)) {
        return !isCancelled0(result);
      }
      this.result = UNCANCELLABLE;
    }
    return true;
  }
  
  private boolean setFailure0(Throwable cause)
  {
    if (cause == null) {
      throw new NullPointerException("cause");
    }
    if (isDone()) {
      return false;
    }
    synchronized (this)
    {
      if (isDone()) {
        return false;
      }
      this.result = new CauseHolder(cause);
      if (hasWaiters()) {
        notifyAll();
      }
    }
    return true;
  }
  
  private boolean setSuccess0(V result)
  {
    if (isDone()) {
      return false;
    }
    synchronized (this)
    {
      if (isDone()) {
        return false;
      }
      if (result == null) {
        this.result = SUCCESS;
      } else {
        this.result = result;
      }
      if (hasWaiters()) {
        notifyAll();
      }
    }
    return true;
  }
  
  public V getNow()
  {
    Object result = this.result;
    if (((result instanceof CauseHolder)) || (result == SUCCESS)) {
      return null;
    }
    return (V)result;
  }
  
  private boolean hasWaiters()
  {
    return this.waiters > 0;
  }
  
  private void incWaiters()
  {
    if (this.waiters == Short.MAX_VALUE) {
      throw new IllegalStateException("too many waiters: " + this);
    }
    this.waiters = ((short)(this.waiters + 1));
  }
  
  private void decWaiters()
  {
    this.waiters = ((short)(this.waiters - 1));
  }
  
  private void notifyListeners()
  {
    Object listeners = this.listeners;
    if (listeners == null) {
      return;
    }
    EventExecutor executor = executor();
    if (executor.inEventLoop())
    {
      InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
      int stackDepth = threadLocals.futureListenerStackDepth();
      if (stackDepth < 8)
      {
        threadLocals.setFutureListenerStackDepth(stackDepth + 1);
        try
        {
          if ((listeners instanceof DefaultFutureListeners))
          {
            notifyListeners0(this, (DefaultFutureListeners)listeners);
          }
          else
          {
            GenericFutureListener<? extends Future<V>> l = (GenericFutureListener)listeners;
            
            notifyListener0(this, l);
          }
        }
        finally
        {
          this.listeners = null;
          threadLocals.setFutureListenerStackDepth(stackDepth);
        }
        return;
      }
    }
    if ((listeners instanceof DefaultFutureListeners))
    {
      final DefaultFutureListeners dfl = (DefaultFutureListeners)listeners;
      execute(executor, new Runnable()
      {
        public void run()
        {
          DefaultPromise.notifyListeners0(DefaultPromise.this, dfl);
          DefaultPromise.this.listeners = null;
        }
      });
    }
    else
    {
      final GenericFutureListener<? extends Future<V>> l = (GenericFutureListener)listeners;
      
      execute(executor, new Runnable()
      {
        public void run()
        {
          DefaultPromise.notifyListener0(DefaultPromise.this, l);
          DefaultPromise.this.listeners = null;
        }
      });
    }
  }
  
  private static void notifyListeners0(Future<?> future, DefaultFutureListeners listeners)
  {
    GenericFutureListener<?>[] a = listeners.listeners();
    int size = listeners.size();
    for (int i = 0; i < size; i++) {
      notifyListener0(future, a[i]);
    }
  }
  
  private void notifyLateListener(GenericFutureListener<?> l)
  {
    EventExecutor executor = executor();
    if (executor.inEventLoop()) {
      if ((this.listeners == null) && (this.lateListeners == null))
      {
        InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
        int stackDepth = threadLocals.futureListenerStackDepth();
        if (stackDepth < 8)
        {
          threadLocals.setFutureListenerStackDepth(stackDepth + 1);
          try
          {
            notifyListener0(this, l);
          }
          finally
          {
            threadLocals.setFutureListenerStackDepth(stackDepth);
          }
          return;
        }
      }
      else
      {
        DefaultPromise<V>.LateListeners lateListeners = this.lateListeners;
        if (lateListeners == null) {
          this.lateListeners = (lateListeners = new LateListeners());
        }
        lateListeners.add(l);
        execute(executor, lateListeners);
        return;
      }
    }
    execute(executor, new LateListenerNotifier(l));
  }
  
  protected static void notifyListener(EventExecutor eventExecutor, Future<?> future, final GenericFutureListener<?> l)
  {
    if (eventExecutor.inEventLoop())
    {
      InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
      int stackDepth = threadLocals.futureListenerStackDepth();
      if (stackDepth < 8)
      {
        threadLocals.setFutureListenerStackDepth(stackDepth + 1);
        try
        {
          notifyListener0(future, l);
        }
        finally
        {
          threadLocals.setFutureListenerStackDepth(stackDepth);
        }
        return;
      }
    }
    execute(eventExecutor, new Runnable()
    {
      public void run()
      {
        DefaultPromise.notifyListener0(this.val$future, l);
      }
    });
  }
  
  private static void execute(EventExecutor executor, Runnable task)
  {
    try
    {
      executor.execute(task);
    }
    catch (Throwable t)
    {
      rejectedExecutionLogger.error("Failed to submit a listener notification task. Event loop shut down?", t);
    }
  }
  
  static void notifyListener0(Future future, GenericFutureListener l)
  {
    try
    {
      l.operationComplete(future);
    }
    catch (Throwable t)
    {
      if (logger.isWarnEnabled()) {
        logger.warn("An exception was thrown by " + l.getClass().getName() + ".operationComplete()", t);
      }
    }
  }
  
  private synchronized Object progressiveListeners()
  {
    Object listeners = this.listeners;
    if (listeners == null) {
      return null;
    }
    if ((listeners instanceof DefaultFutureListeners))
    {
      DefaultFutureListeners dfl = (DefaultFutureListeners)listeners;
      int progressiveSize = dfl.progressiveSize();
      switch (progressiveSize)
      {
      case 0: 
        return null;
      case 1: 
        for (GenericFutureListener<?> l : dfl.listeners()) {
          if ((l instanceof GenericProgressiveFutureListener)) {
            return l;
          }
        }
        return null;
      }
      GenericFutureListener<?>[] array = dfl.listeners();
      GenericProgressiveFutureListener<?>[] copy = new GenericProgressiveFutureListener[progressiveSize];
      int i = 0;
      for (int j = 0; j < progressiveSize; i++)
      {
        GenericFutureListener<?> l = array[i];
        if ((l instanceof GenericProgressiveFutureListener)) {
          copy[(j++)] = ((GenericProgressiveFutureListener)l);
        }
      }
      return copy;
    }
    if ((listeners instanceof GenericProgressiveFutureListener)) {
      return listeners;
    }
    return null;
  }
  
  void notifyProgressiveListeners(final long progress, long total)
  {
    Object listeners = progressiveListeners();
    if (listeners == null) {
      return;
    }
    final ProgressiveFuture<V> self = (ProgressiveFuture)this;
    
    EventExecutor executor = executor();
    if (executor.inEventLoop())
    {
      if ((listeners instanceof GenericProgressiveFutureListener[])) {
        notifyProgressiveListeners0(self, (GenericProgressiveFutureListener[])listeners, progress, total);
      } else {
        notifyProgressiveListener0(self, (GenericProgressiveFutureListener)listeners, progress, total);
      }
    }
    else if ((listeners instanceof GenericProgressiveFutureListener[]))
    {
      final GenericProgressiveFutureListener<?>[] array = (GenericProgressiveFutureListener[])listeners;
      
      execute(executor, new Runnable()
      {
        public void run()
        {
          DefaultPromise.notifyProgressiveListeners0(self, array, progress, this.val$total);
        }
      });
    }
    else
    {
      final GenericProgressiveFutureListener<ProgressiveFuture<V>> l = (GenericProgressiveFutureListener)listeners;
      
      execute(executor, new Runnable()
      {
        public void run()
        {
          DefaultPromise.notifyProgressiveListener0(self, l, progress, this.val$total);
        }
      });
    }
  }
  
  private static void notifyProgressiveListeners0(ProgressiveFuture<?> future, GenericProgressiveFutureListener<?>[] listeners, long progress, long total)
  {
    for (GenericProgressiveFutureListener<?> l : listeners)
    {
      if (l == null) {
        break;
      }
      notifyProgressiveListener0(future, l, progress, total);
    }
  }
  
  private static void notifyProgressiveListener0(ProgressiveFuture future, GenericProgressiveFutureListener l, long progress, long total)
  {
    try
    {
      l.operationProgressed(future, progress, total);
    }
    catch (Throwable t)
    {
      if (logger.isWarnEnabled()) {
        logger.warn("An exception was thrown by " + l.getClass().getName() + ".operationProgressed()", t);
      }
    }
  }
  
  private static final class CauseHolder
  {
    final Throwable cause;
    
    CauseHolder(Throwable cause)
    {
      this.cause = cause;
    }
  }
  
  public String toString()
  {
    return toStringBuilder().toString();
  }
  
  protected StringBuilder toStringBuilder()
  {
    StringBuilder buf = new StringBuilder(64).append(StringUtil.simpleClassName(this)).append('@').append(Integer.toHexString(hashCode()));
    
    Object result = this.result;
    if (result == SUCCESS) {
      buf.append("(success)");
    } else if (result == UNCANCELLABLE) {
      buf.append("(uncancellable)");
    } else if ((result instanceof CauseHolder)) {
      buf.append("(failure(").append(((CauseHolder)result).cause).append(')');
    } else {
      buf.append("(incomplete)");
    }
    return buf;
  }
  
  /* Error */
  private boolean await0(long timeoutNanos, boolean interruptable)
    throws InterruptedException
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 20	io/netty/util/concurrent/DefaultPromise:isDone	()Z
    //   4: ifeq +5 -> 9
    //   7: iconst_1
    //   8: ireturn
    //   9: lload_1
    //   10: lconst_0
    //   11: lcmp
    //   12: ifgt +8 -> 20
    //   15: aload_0
    //   16: invokevirtual 20	io/netty/util/concurrent/DefaultPromise:isDone	()Z
    //   19: ireturn
    //   20: iload_3
    //   21: ifeq +21 -> 42
    //   24: invokestatic 35	java/lang/Thread:interrupted	()Z
    //   27: ifeq +15 -> 42
    //   30: new 36	java/lang/InterruptedException
    //   33: dup
    //   34: aload_0
    //   35: invokevirtual 37	io/netty/util/concurrent/DefaultPromise:toString	()Ljava/lang/String;
    //   38: invokespecial 38	java/lang/InterruptedException:<init>	(Ljava/lang/String;)V
    //   41: athrow
    //   42: invokestatic 50	java/lang/System:nanoTime	()J
    //   45: lstore 4
    //   47: lload_1
    //   48: lstore 6
    //   50: iconst_0
    //   51: istore 8
    //   53: aload_0
    //   54: dup
    //   55: astore 9
    //   57: monitorenter
    //   58: aload_0
    //   59: invokevirtual 20	io/netty/util/concurrent/DefaultPromise:isDone	()Z
    //   62: ifeq +23 -> 85
    //   65: iconst_1
    //   66: istore 10
    //   68: aload 9
    //   70: monitorexit
    //   71: iload 8
    //   73: ifeq +9 -> 82
    //   76: invokestatic 46	java/lang/Thread:currentThread	()Ljava/lang/Thread;
    //   79: invokevirtual 47	java/lang/Thread:interrupt	()V
    //   82: iload 10
    //   84: ireturn
    //   85: lload 6
    //   87: lconst_0
    //   88: lcmp
    //   89: ifgt +26 -> 115
    //   92: aload_0
    //   93: invokevirtual 20	io/netty/util/concurrent/DefaultPromise:isDone	()Z
    //   96: istore 10
    //   98: aload 9
    //   100: monitorexit
    //   101: iload 8
    //   103: ifeq +9 -> 112
    //   106: invokestatic 46	java/lang/Thread:currentThread	()Ljava/lang/Thread;
    //   109: invokevirtual 47	java/lang/Thread:interrupt	()V
    //   112: iload 10
    //   114: ireturn
    //   115: aload_0
    //   116: invokevirtual 39	io/netty/util/concurrent/DefaultPromise:checkDeadLock	()V
    //   119: aload_0
    //   120: invokespecial 40	io/netty/util/concurrent/DefaultPromise:incWaiters	()V
    //   123: aload_0
    //   124: lload 6
    //   126: ldc2_w 51
    //   129: ldiv
    //   130: lload 6
    //   132: ldc2_w 51
    //   135: lrem
    //   136: l2i
    //   137: invokevirtual 53	java/lang/Object:wait	(JI)V
    //   140: goto +15 -> 155
    //   143: astore 10
    //   145: iload_3
    //   146: ifeq +6 -> 152
    //   149: aload 10
    //   151: athrow
    //   152: iconst_1
    //   153: istore 8
    //   155: aload_0
    //   156: invokevirtual 20	io/netty/util/concurrent/DefaultPromise:isDone	()Z
    //   159: ifeq +27 -> 186
    //   162: iconst_1
    //   163: istore 10
    //   165: aload_0
    //   166: invokespecial 42	io/netty/util/concurrent/DefaultPromise:decWaiters	()V
    //   169: aload 9
    //   171: monitorexit
    //   172: iload 8
    //   174: ifeq +9 -> 183
    //   177: invokestatic 46	java/lang/Thread:currentThread	()Ljava/lang/Thread;
    //   180: invokevirtual 47	java/lang/Thread:interrupt	()V
    //   183: iload 10
    //   185: ireturn
    //   186: lload_1
    //   187: invokestatic 50	java/lang/System:nanoTime	()J
    //   190: lload 4
    //   192: lsub
    //   193: lsub
    //   194: lstore 6
    //   196: lload 6
    //   198: lconst_0
    //   199: lcmp
    //   200: ifgt -77 -> 123
    //   203: aload_0
    //   204: invokevirtual 20	io/netty/util/concurrent/DefaultPromise:isDone	()Z
    //   207: istore 10
    //   209: aload_0
    //   210: invokespecial 42	io/netty/util/concurrent/DefaultPromise:decWaiters	()V
    //   213: aload 9
    //   215: monitorexit
    //   216: iload 8
    //   218: ifeq +9 -> 227
    //   221: invokestatic 46	java/lang/Thread:currentThread	()Ljava/lang/Thread;
    //   224: invokevirtual 47	java/lang/Thread:interrupt	()V
    //   227: iload 10
    //   229: ireturn
    //   230: astore 11
    //   232: aload_0
    //   233: invokespecial 42	io/netty/util/concurrent/DefaultPromise:decWaiters	()V
    //   236: aload 11
    //   238: athrow
    //   239: astore 12
    //   241: aload 9
    //   243: monitorexit
    //   244: aload 12
    //   246: athrow
    //   247: astore 13
    //   249: iload 8
    //   251: ifeq +9 -> 260
    //   254: invokestatic 46	java/lang/Thread:currentThread	()Ljava/lang/Thread;
    //   257: invokevirtual 47	java/lang/Thread:interrupt	()V
    //   260: aload 13
    //   262: athrow
    // Line number table:
    //   Java source line #324	-> byte code offset #0
    //   Java source line #325	-> byte code offset #7
    //   Java source line #328	-> byte code offset #9
    //   Java source line #329	-> byte code offset #15
    //   Java source line #332	-> byte code offset #20
    //   Java source line #333	-> byte code offset #30
    //   Java source line #336	-> byte code offset #42
    //   Java source line #337	-> byte code offset #47
    //   Java source line #338	-> byte code offset #50
    //   Java source line #341	-> byte code offset #53
    //   Java source line #342	-> byte code offset #58
    //   Java source line #343	-> byte code offset #65
    //   Java source line #378	-> byte code offset #71
    //   Java source line #379	-> byte code offset #76
    //   Java source line #346	-> byte code offset #85
    //   Java source line #347	-> byte code offset #92
    //   Java source line #378	-> byte code offset #101
    //   Java source line #379	-> byte code offset #106
    //   Java source line #350	-> byte code offset #115
    //   Java source line #351	-> byte code offset #119
    //   Java source line #355	-> byte code offset #123
    //   Java source line #362	-> byte code offset #140
    //   Java source line #356	-> byte code offset #143
    //   Java source line #357	-> byte code offset #145
    //   Java source line #358	-> byte code offset #149
    //   Java source line #360	-> byte code offset #152
    //   Java source line #364	-> byte code offset #155
    //   Java source line #365	-> byte code offset #162
    //   Java source line #374	-> byte code offset #165
    //   Java source line #378	-> byte code offset #172
    //   Java source line #379	-> byte code offset #177
    //   Java source line #367	-> byte code offset #186
    //   Java source line #368	-> byte code offset #196
    //   Java source line #369	-> byte code offset #203
    //   Java source line #374	-> byte code offset #209
    //   Java source line #378	-> byte code offset #216
    //   Java source line #379	-> byte code offset #221
    //   Java source line #374	-> byte code offset #230
    //   Java source line #376	-> byte code offset #239
    //   Java source line #378	-> byte code offset #247
    //   Java source line #379	-> byte code offset #254
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	263	0	this	DefaultPromise<V>
    //   0	263	1	timeoutNanos	long
    //   0	263	3	interruptable	boolean
    //   45	146	4	startTime	long
    //   48	149	6	waitTime	long
    //   51	199	8	interrupted	boolean
    //   66	47	10	bool1	boolean
    //   143	85	10	e	InterruptedException
    //   163	65	10	bool2	boolean
    //   230	7	11	localObject1	Object
    //   239	6	12	localObject2	Object
    //   247	14	13	localObject3	Object
    // Exception table:
    //   from	to	target	type
    //   123	140	143	java/lang/InterruptedException
    //   123	165	230	finally
    //   186	209	230	finally
    //   230	232	230	finally
    //   58	71	239	finally
    //   85	101	239	finally
    //   115	172	239	finally
    //   186	216	239	finally
    //   230	244	239	finally
    //   53	71	247	finally
    //   85	101	247	finally
    //   115	172	247	finally
    //   186	216	247	finally
    //   230	249	247	finally
  }
  
  private final class LateListeners
    extends ArrayDeque<GenericFutureListener<?>>
    implements Runnable
  {
    private static final long serialVersionUID = -687137418080392244L;
    
    LateListeners()
    {
      super();
    }
    
    public void run()
    {
      if (DefaultPromise.this.listeners == null) {
        for (;;)
        {
          GenericFutureListener<?> l = (GenericFutureListener)poll();
          if (l == null) {
            break;
          }
          DefaultPromise.notifyListener0(DefaultPromise.this, l);
        }
      }
      DefaultPromise.execute(DefaultPromise.this.executor(), this);
    }
  }
  
  private final class LateListenerNotifier
    implements Runnable
  {
    private GenericFutureListener<?> l;
    
    LateListenerNotifier()
    {
      this.l = l;
    }
    
    public void run()
    {
      DefaultPromise<V>.LateListeners lateListeners = DefaultPromise.this.lateListeners;
      if (this.l != null)
      {
        if (lateListeners == null) {
          DefaultPromise.this.lateListeners = (lateListeners = new DefaultPromise.LateListeners(DefaultPromise.this));
        }
        lateListeners.add(this.l);
        this.l = null;
      }
      lateListeners.run();
    }
  }
}
