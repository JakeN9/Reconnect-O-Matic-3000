package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ResourceLeakDetector<T>
{
  private static final String PROP_LEVEL = "io.netty.leakDetectionLevel";
  private static final Level DEFAULT_LEVEL = Level.SIMPLE;
  private static Level level;
  
  public static enum Level
  {
    DISABLED,  SIMPLE,  ADVANCED,  PARANOID;
    
    private Level() {}
  }
  
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ResourceLeakDetector.class);
  private static final int DEFAULT_SAMPLING_INTERVAL = 113;
  
  static
  {
    String levelStr = SystemPropertyUtil.get("io.netty.leakDetectionLevel", DEFAULT_LEVEL.name()).trim().toUpperCase();
    Level level = DEFAULT_LEVEL;
    for (Level l : EnumSet.allOf(Level.class)) {
      if ((levelStr.equals(l.name())) || (levelStr.equals(String.valueOf(l.ordinal())))) {
        level = l;
      }
    }
    level = level;
    if (logger.isDebugEnabled()) {
      logger.debug("-D{}: {}", "io.netty.leakDetectionLevel", level.name().toLowerCase());
    }
  }
  
  public static void setLevel(Level level)
  {
    if (level == null) {
      throw new NullPointerException("level");
    }
    level = level;
  }
  
  public static Level getLevel()
  {
    return level;
  }
  
  private final ResourceLeakDetector<T>.DefaultResourceLeak head = new DefaultResourceLeak(null);
  private final ResourceLeakDetector<T>.DefaultResourceLeak tail = new DefaultResourceLeak(null);
  private final ReferenceQueue<Object> refQueue = new ReferenceQueue();
  private final ConcurrentMap<String, Boolean> reportedLeaks = PlatformDependent.newConcurrentHashMap();
  private final String resourceType;
  private final int samplingInterval;
  private final long maxActive;
  private long active;
  private final AtomicBoolean loggedTooManyActive = new AtomicBoolean();
  private long leakCheckCnt;
  
  public ResourceLeakDetector(Class<?> resourceType)
  {
    this(StringUtil.simpleClassName(resourceType));
  }
  
  public ResourceLeakDetector(String resourceType)
  {
    this(resourceType, 113, Long.MAX_VALUE);
  }
  
  public ResourceLeakDetector(Class<?> resourceType, int samplingInterval, long maxActive)
  {
    this(StringUtil.simpleClassName(resourceType), samplingInterval, maxActive);
  }
  
  public ResourceLeakDetector(String resourceType, int samplingInterval, long maxActive)
  {
    if (resourceType == null) {
      throw new NullPointerException("resourceType");
    }
    if (samplingInterval <= 0) {
      throw new IllegalArgumentException("samplingInterval: " + samplingInterval + " (expected: 1+)");
    }
    if (maxActive <= 0L) {
      throw new IllegalArgumentException("maxActive: " + maxActive + " (expected: 1+)");
    }
    this.resourceType = resourceType;
    this.samplingInterval = samplingInterval;
    this.maxActive = maxActive;
    
    this.head.next = this.tail;
    this.tail.prev = this.head;
  }
  
  public ResourceLeak open(T obj)
  {
    Level level = level;
    if (level == Level.DISABLED) {
      return null;
    }
    if (level.ordinal() < Level.PARANOID.ordinal())
    {
      if (this.leakCheckCnt++ % this.samplingInterval == 0L)
      {
        reportLeak(level);
        return new DefaultResourceLeak(obj);
      }
      return null;
    }
    reportLeak(level);
    return new DefaultResourceLeak(obj);
  }
  
  private void reportLeak(Level level)
  {
    if (!logger.isErrorEnabled())
    {
      for (;;)
      {
        ResourceLeakDetector<T>.DefaultResourceLeak ref = (DefaultResourceLeak)this.refQueue.poll();
        if (ref == null) {
          break;
        }
        ref.close();
      }
      return;
    }
    int samplingInterval = level == Level.PARANOID ? 1 : this.samplingInterval;
    if ((this.active * samplingInterval > this.maxActive) && (this.loggedTooManyActive.compareAndSet(false, true))) {
      logger.error("LEAK: You are creating too many " + this.resourceType + " instances.  " + this.resourceType + " is a shared resource that must be reused across the JVM," + "so that only a few instances are created.");
    }
    for (;;)
    {
      ResourceLeakDetector<T>.DefaultResourceLeak ref = (DefaultResourceLeak)this.refQueue.poll();
      if (ref == null) {
        break;
      }
      ref.clear();
      if (ref.close())
      {
        String records = ref.toString();
        if (this.reportedLeaks.putIfAbsent(records, Boolean.TRUE) == null) {
          if (records.isEmpty()) {
            logger.error("LEAK: {}.release() was not called before it's garbage-collected. Enable advanced leak reporting to find out where the leak occurred. To enable advanced leak reporting, specify the JVM option '-D{}={}' or call {}.setLevel() See http://netty.io/wiki/reference-counted-objects.html for more information.", new Object[] { this.resourceType, "io.netty.leakDetectionLevel", Level.ADVANCED.name().toLowerCase(), StringUtil.simpleClassName(this) });
          } else {
            logger.error("LEAK: {}.release() was not called before it's garbage-collected. See http://netty.io/wiki/reference-counted-objects.html for more information.{}", this.resourceType, records);
          }
        }
      }
    }
  }
  
  private final class DefaultResourceLeak
    extends PhantomReference<Object>
    implements ResourceLeak
  {
    private static final int MAX_RECORDS = 4;
    private final String creationRecord;
    private final Deque<String> lastRecords = new ArrayDeque();
    private final AtomicBoolean freed;
    private ResourceLeakDetector<T>.DefaultResourceLeak prev;
    private ResourceLeakDetector<T>.DefaultResourceLeak next;
    
    DefaultResourceLeak(Object referent)
    {
      super(referent != null ? ResourceLeakDetector.this.refQueue : null);
      ResourceLeakDetector.Level level;
      if (referent != null)
      {
        level = ResourceLeakDetector.getLevel();
        if (level.ordinal() >= ResourceLeakDetector.Level.ADVANCED.ordinal()) {
          this.creationRecord = ResourceLeakDetector.newRecord(null, 3);
        } else {
          this.creationRecord = null;
        }
        synchronized (ResourceLeakDetector.this.head)
        {
          this.prev = ResourceLeakDetector.this.head;
          this.next = ResourceLeakDetector.this.head.next;
          ResourceLeakDetector.this.head.next.prev = this;
          ResourceLeakDetector.this.head.next = this;
          ResourceLeakDetector.access$408(ResourceLeakDetector.this);
        }
        this.freed = new AtomicBoolean();
      }
      else
      {
        this.creationRecord = null;
        this.freed = new AtomicBoolean(true);
      }
    }
    
    public void record()
    {
      record0(null, 3);
    }
    
    public void record(Object hint)
    {
      record0(hint, 3);
    }
    
    private void record0(Object hint, int recordsToSkip)
    {
      if (this.creationRecord != null)
      {
        String value = ResourceLeakDetector.newRecord(hint, recordsToSkip);
        synchronized (this.lastRecords)
        {
          int size = this.lastRecords.size();
          if ((size == 0) || (!((String)this.lastRecords.getLast()).equals(value))) {
            this.lastRecords.add(value);
          }
          if (size > 4) {
            this.lastRecords.removeFirst();
          }
        }
      }
    }
    
    public boolean close()
    {
      if (this.freed.compareAndSet(false, true))
      {
        synchronized (ResourceLeakDetector.this.head)
        {
          ResourceLeakDetector.access$410(ResourceLeakDetector.this);
          this.prev.next = this.next;
          this.next.prev = this.prev;
          this.prev = null;
          this.next = null;
        }
        return true;
      }
      return false;
    }
    
    public String toString()
    {
      if (this.creationRecord == null) {
        return "";
      }
      Object[] array;
      synchronized (this.lastRecords)
      {
        array = this.lastRecords.toArray();
      }
      StringBuilder buf = new StringBuilder(16384).append(StringUtil.NEWLINE).append("Recent access records: ").append(array.length).append(StringUtil.NEWLINE);
      if (array.length > 0) {
        for (int i = array.length - 1; i >= 0; i--) {
          buf.append('#').append(i + 1).append(':').append(StringUtil.NEWLINE).append(array[i]);
        }
      }
      buf.append("Created at:").append(StringUtil.NEWLINE).append(this.creationRecord);
      
      buf.setLength(buf.length() - StringUtil.NEWLINE.length());
      return buf.toString();
    }
  }
  
  private static final String[] STACK_TRACE_ELEMENT_EXCLUSIONS = { "io.netty.util.ReferenceCountUtil.touch(", "io.netty.buffer.AdvancedLeakAwareByteBuf.touch(", "io.netty.buffer.AbstractByteBufAllocator.toLeakAwareBuffer(" };
  
  static String newRecord(Object hint, int recordsToSkip)
  {
    StringBuilder buf = new StringBuilder(4096);
    if (hint != null)
    {
      buf.append("\tHint: ");
      if ((hint instanceof ResourceLeakHint)) {
        buf.append(((ResourceLeakHint)hint).toHintString());
      } else {
        buf.append(hint);
      }
      buf.append(StringUtil.NEWLINE);
    }
    StackTraceElement[] array = new Throwable().getStackTrace();
    for (StackTraceElement e : array) {
      if (recordsToSkip > 0)
      {
        recordsToSkip--;
      }
      else
      {
        String estr = e.toString();
        
        boolean excluded = false;
        for (String exclusion : STACK_TRACE_ELEMENT_EXCLUSIONS) {
          if (estr.startsWith(exclusion))
          {
            excluded = true;
            break;
          }
        }
        if (!excluded)
        {
          buf.append('\t');
          buf.append(estr);
          buf.append(StringUtil.NEWLINE);
        }
      }
    }
    return buf.toString();
  }
}
