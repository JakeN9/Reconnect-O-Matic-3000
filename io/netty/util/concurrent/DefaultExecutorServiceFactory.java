package io.netty.util.concurrent;

import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.chmv8.ForkJoinPool;
import io.netty.util.internal.chmv8.ForkJoinPool.ForkJoinWorkerThreadFactory;
import io.netty.util.internal.chmv8.ForkJoinWorkerThread;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public final class DefaultExecutorServiceFactory
  implements ExecutorServiceFactory
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultExecutorServiceFactory.class);
  private static final AtomicInteger executorId = new AtomicInteger();
  private final String namePrefix;
  
  public DefaultExecutorServiceFactory(Class<?> clazzNamePrefix)
  {
    this(toName(clazzNamePrefix));
  }
  
  public DefaultExecutorServiceFactory(String namePrefix)
  {
    this.namePrefix = namePrefix;
  }
  
  public ExecutorService newExecutorService(int parallelism)
  {
    ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = new DefaultForkJoinWorkerThreadFactory(this.namePrefix + '-' + executorId.getAndIncrement());
    
    return new ForkJoinPool(parallelism, threadFactory, DefaultUncaughtExceptionHandler.INSTANCE, true);
  }
  
  private static String toName(Class<?> clazz)
  {
    if (clazz == null) {
      throw new NullPointerException("clazz");
    }
    String clazzName = StringUtil.simpleClassName(clazz);
    switch (clazzName.length())
    {
    case 0: 
      return "unknown";
    case 1: 
      return clazzName.toLowerCase(Locale.US);
    }
    if ((Character.isUpperCase(clazzName.charAt(0))) && (Character.isLowerCase(clazzName.charAt(1)))) {
      return Character.toLowerCase(clazzName.charAt(0)) + clazzName.substring(1);
    }
    return clazzName;
  }
  
  private static final class DefaultUncaughtExceptionHandler
    implements Thread.UncaughtExceptionHandler
  {
    private static final DefaultUncaughtExceptionHandler INSTANCE = new DefaultUncaughtExceptionHandler();
    
    public void uncaughtException(Thread t, Throwable e)
    {
      if (DefaultExecutorServiceFactory.logger.isErrorEnabled()) {
        DefaultExecutorServiceFactory.logger.error("Uncaught exception in thread: {}", t.getName(), e);
      }
    }
  }
  
  private static final class DefaultForkJoinWorkerThreadFactory
    implements ForkJoinPool.ForkJoinWorkerThreadFactory
  {
    private final AtomicInteger idx = new AtomicInteger();
    private final String namePrefix;
    
    DefaultForkJoinWorkerThreadFactory(String namePrefix)
    {
      this.namePrefix = namePrefix;
    }
    
    public ForkJoinWorkerThread newThread(ForkJoinPool pool)
    {
      ForkJoinWorkerThread thread = new DefaultExecutorServiceFactory.DefaultForkJoinWorkerThread(pool);
      thread.setName(this.namePrefix + '-' + this.idx.getAndIncrement());
      thread.setPriority(10);
      return thread;
    }
  }
  
  private static final class DefaultForkJoinWorkerThread
    extends ForkJoinWorkerThread
    implements FastThreadLocalAccess
  {
    private InternalThreadLocalMap threadLocalMap;
    
    DefaultForkJoinWorkerThread(ForkJoinPool pool)
    {
      super();
    }
    
    public InternalThreadLocalMap threadLocalMap()
    {
      return this.threadLocalMap;
    }
    
    public void setThreadLocalMap(InternalThreadLocalMap threadLocalMap)
    {
      this.threadLocalMap = threadLocalMap;
    }
  }
}
