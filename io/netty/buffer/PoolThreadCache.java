package io.netty.buffer;

import io.netty.util.ThreadDeathWatcher;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteBuffer;

final class PoolThreadCache
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(PoolThreadCache.class);
  final PoolArena<byte[]> heapArena;
  final PoolArena<ByteBuffer> directArena;
  private final MemoryRegionCache<byte[]>[] tinySubPageHeapCaches;
  private final MemoryRegionCache<byte[]>[] smallSubPageHeapCaches;
  private final MemoryRegionCache<ByteBuffer>[] tinySubPageDirectCaches;
  private final MemoryRegionCache<ByteBuffer>[] smallSubPageDirectCaches;
  private final MemoryRegionCache<byte[]>[] normalHeapCaches;
  private final MemoryRegionCache<ByteBuffer>[] normalDirectCaches;
  private final int numShiftsNormalDirect;
  private final int numShiftsNormalHeap;
  private final int freeSweepAllocationThreshold;
  private int allocations;
  private final Thread thread = Thread.currentThread();
  private final Runnable freeTask = new Runnable()
  {
    public void run()
    {
      PoolThreadCache.this.free0();
    }
  };
  
  PoolThreadCache(PoolArena<byte[]> heapArena, PoolArena<ByteBuffer> directArena, int tinyCacheSize, int smallCacheSize, int normalCacheSize, int maxCachedBufferCapacity, int freeSweepAllocationThreshold)
  {
    if (maxCachedBufferCapacity < 0) {
      throw new IllegalArgumentException("maxCachedBufferCapacity: " + maxCachedBufferCapacity + " (expected: >= 0)");
    }
    if (freeSweepAllocationThreshold < 1) {
      throw new IllegalArgumentException("freeSweepAllocationThreshold: " + maxCachedBufferCapacity + " (expected: > 0)");
    }
    this.freeSweepAllocationThreshold = freeSweepAllocationThreshold;
    this.heapArena = heapArena;
    this.directArena = directArena;
    if (directArena != null)
    {
      this.tinySubPageDirectCaches = createSubPageCaches(tinyCacheSize, 32);
      this.smallSubPageDirectCaches = createSubPageCaches(smallCacheSize, directArena.numSmallSubpagePools);
      
      this.numShiftsNormalDirect = log2(directArena.pageSize);
      this.normalDirectCaches = createNormalCaches(normalCacheSize, maxCachedBufferCapacity, directArena);
    }
    else
    {
      this.tinySubPageDirectCaches = null;
      this.smallSubPageDirectCaches = null;
      this.normalDirectCaches = null;
      this.numShiftsNormalDirect = -1;
    }
    if (heapArena != null)
    {
      this.tinySubPageHeapCaches = createSubPageCaches(tinyCacheSize, 32);
      this.smallSubPageHeapCaches = createSubPageCaches(smallCacheSize, heapArena.numSmallSubpagePools);
      
      this.numShiftsNormalHeap = log2(heapArena.pageSize);
      this.normalHeapCaches = createNormalCaches(normalCacheSize, maxCachedBufferCapacity, heapArena);
    }
    else
    {
      this.tinySubPageHeapCaches = null;
      this.smallSubPageHeapCaches = null;
      this.normalHeapCaches = null;
      this.numShiftsNormalHeap = -1;
    }
    ThreadDeathWatcher.watch(this.thread, this.freeTask);
  }
  
  private static <T> SubPageMemoryRegionCache<T>[] createSubPageCaches(int cacheSize, int numCaches)
  {
    if (cacheSize > 0)
    {
      SubPageMemoryRegionCache<T>[] cache = new SubPageMemoryRegionCache[numCaches];
      for (int i = 0; i < cache.length; i++) {
        cache[i] = new SubPageMemoryRegionCache(cacheSize);
      }
      return cache;
    }
    return null;
  }
  
  private static <T> NormalMemoryRegionCache<T>[] createNormalCaches(int cacheSize, int maxCachedBufferCapacity, PoolArena<T> area)
  {
    if (cacheSize > 0)
    {
      int max = Math.min(area.chunkSize, maxCachedBufferCapacity);
      int arraySize = Math.max(1, max / area.pageSize);
      
      NormalMemoryRegionCache<T>[] cache = new NormalMemoryRegionCache[arraySize];
      for (int i = 0; i < cache.length; i++) {
        cache[i] = new NormalMemoryRegionCache(cacheSize);
      }
      return cache;
    }
    return null;
  }
  
  private static int log2(int val)
  {
    int res = 0;
    while (val > 1)
    {
      val >>= 1;
      res++;
    }
    return res;
  }
  
  boolean allocateTiny(PoolArena<?> area, PooledByteBuf<?> buf, int reqCapacity, int normCapacity)
  {
    return allocate(cacheForTiny(area, normCapacity), buf, reqCapacity);
  }
  
  boolean allocateSmall(PoolArena<?> area, PooledByteBuf<?> buf, int reqCapacity, int normCapacity)
  {
    return allocate(cacheForSmall(area, normCapacity), buf, reqCapacity);
  }
  
  boolean allocateNormal(PoolArena<?> area, PooledByteBuf<?> buf, int reqCapacity, int normCapacity)
  {
    return allocate(cacheForNormal(area, normCapacity), buf, reqCapacity);
  }
  
  private boolean allocate(MemoryRegionCache<?> cache, PooledByteBuf buf, int reqCapacity)
  {
    if (cache == null) {
      return false;
    }
    boolean allocated = cache.allocate(buf, reqCapacity);
    if (++this.allocations >= this.freeSweepAllocationThreshold)
    {
      this.allocations = 0;
      trim();
    }
    return allocated;
  }
  
  boolean add(PoolArena<?> area, PoolChunk chunk, long handle, int normCapacity)
  {
    MemoryRegionCache<?> cache;
    MemoryRegionCache<?> cache;
    if (area.isTinyOrSmall(normCapacity))
    {
      MemoryRegionCache<?> cache;
      if (PoolArena.isTiny(normCapacity)) {
        cache = cacheForTiny(area, normCapacity);
      } else {
        cache = cacheForSmall(area, normCapacity);
      }
    }
    else
    {
      cache = cacheForNormal(area, normCapacity);
    }
    if (cache == null) {
      return false;
    }
    return cache.add(chunk, handle);
  }
  
  void free()
  {
    ThreadDeathWatcher.unwatch(this.thread, this.freeTask);
    free0();
  }
  
  private void free0()
  {
    int numFreed = free(this.tinySubPageDirectCaches) + free(this.smallSubPageDirectCaches) + free(this.normalDirectCaches) + free(this.tinySubPageHeapCaches) + free(this.smallSubPageHeapCaches) + free(this.normalHeapCaches);
    if ((numFreed > 0) && (logger.isDebugEnabled())) {
      logger.debug("Freed {} thread-local buffer(s) from thread: {}", Integer.valueOf(numFreed), this.thread.getName());
    }
  }
  
  private static int free(MemoryRegionCache<?>[] caches)
  {
    if (caches == null) {
      return 0;
    }
    int numFreed = 0;
    for (MemoryRegionCache<?> c : caches) {
      numFreed += free(c);
    }
    return numFreed;
  }
  
  private static int free(MemoryRegionCache<?> cache)
  {
    if (cache == null) {
      return 0;
    }
    return cache.free();
  }
  
  void trim()
  {
    trim(this.tinySubPageDirectCaches);
    trim(this.smallSubPageDirectCaches);
    trim(this.normalDirectCaches);
    trim(this.tinySubPageHeapCaches);
    trim(this.smallSubPageHeapCaches);
    trim(this.normalHeapCaches);
  }
  
  private static void trim(MemoryRegionCache<?>[] caches)
  {
    if (caches == null) {
      return;
    }
    for (MemoryRegionCache<?> c : caches) {
      trim(c);
    }
  }
  
  private static void trim(MemoryRegionCache<?> cache)
  {
    if (cache == null) {
      return;
    }
    cache.trim();
  }
  
  private MemoryRegionCache<?> cacheForTiny(PoolArena<?> area, int normCapacity)
  {
    int idx = PoolArena.tinyIdx(normCapacity);
    if (area.isDirect()) {
      return cache(this.tinySubPageDirectCaches, idx);
    }
    return cache(this.tinySubPageHeapCaches, idx);
  }
  
  private MemoryRegionCache<?> cacheForSmall(PoolArena<?> area, int normCapacity)
  {
    int idx = PoolArena.smallIdx(normCapacity);
    if (area.isDirect()) {
      return cache(this.smallSubPageDirectCaches, idx);
    }
    return cache(this.smallSubPageHeapCaches, idx);
  }
  
  private MemoryRegionCache<?> cacheForNormal(PoolArena<?> area, int normCapacity)
  {
    if (area.isDirect())
    {
      int idx = log2(normCapacity >> this.numShiftsNormalDirect);
      return cache(this.normalDirectCaches, idx);
    }
    int idx = log2(normCapacity >> this.numShiftsNormalHeap);
    return cache(this.normalHeapCaches, idx);
  }
  
  private static <T> MemoryRegionCache<T> cache(MemoryRegionCache<T>[] cache, int idx)
  {
    if ((cache == null) || (idx > cache.length - 1)) {
      return null;
    }
    return cache[idx];
  }
  
  private static final class SubPageMemoryRegionCache<T>
    extends PoolThreadCache.MemoryRegionCache<T>
  {
    SubPageMemoryRegionCache(int size)
    {
      super();
    }
    
    protected void initBuf(PoolChunk<T> chunk, long handle, PooledByteBuf<T> buf, int reqCapacity)
    {
      chunk.initBufWithSubpage(buf, handle, reqCapacity);
    }
  }
  
  private static final class NormalMemoryRegionCache<T>
    extends PoolThreadCache.MemoryRegionCache<T>
  {
    NormalMemoryRegionCache(int size)
    {
      super();
    }
    
    protected void initBuf(PoolChunk<T> chunk, long handle, PooledByteBuf<T> buf, int reqCapacity)
    {
      chunk.initBuf(buf, handle, reqCapacity);
    }
  }
  
  private static abstract class MemoryRegionCache<T>
  {
    private final Entry<T>[] entries;
    private final int maxUnusedCached;
    private int head;
    private int tail;
    private int maxEntriesInUse;
    private int entriesInUse;
    
    MemoryRegionCache(int size)
    {
      this.entries = new Entry[powerOfTwo(size)];
      for (int i = 0; i < this.entries.length; i++) {
        this.entries[i] = new Entry(null);
      }
      this.maxUnusedCached = (size / 2);
    }
    
    private static int powerOfTwo(int res)
    {
      if (res <= 2) {
        return 2;
      }
      res--;
      res |= res >> 1;
      res |= res >> 2;
      res |= res >> 4;
      res |= res >> 8;
      res |= res >> 16;
      res++;
      return res;
    }
    
    protected abstract void initBuf(PoolChunk<T> paramPoolChunk, long paramLong, PooledByteBuf<T> paramPooledByteBuf, int paramInt);
    
    public boolean add(PoolChunk<T> chunk, long handle)
    {
      Entry<T> entry = this.entries[this.tail];
      if (entry.chunk != null) {
        return false;
      }
      this.entriesInUse -= 1;
      
      entry.chunk = chunk;
      entry.handle = handle;
      this.tail = nextIdx(this.tail);
      return true;
    }
    
    public boolean allocate(PooledByteBuf<T> buf, int reqCapacity)
    {
      Entry<T> entry = this.entries[this.head];
      if (entry.chunk == null) {
        return false;
      }
      this.entriesInUse += 1;
      if (this.maxEntriesInUse < this.entriesInUse) {
        this.maxEntriesInUse = this.entriesInUse;
      }
      initBuf(entry.chunk, entry.handle, buf, reqCapacity);
      
      entry.chunk = null;
      this.head = nextIdx(this.head);
      return true;
    }
    
    public int free()
    {
      int numFreed = 0;
      this.entriesInUse = 0;
      this.maxEntriesInUse = 0;
      for (int i = this.head;; i = nextIdx(i)) {
        if (freeEntry(this.entries[i])) {
          numFreed++;
        } else {
          return numFreed;
        }
      }
    }
    
    private void trim()
    {
      int free = size() - this.maxEntriesInUse;
      this.entriesInUse = 0;
      this.maxEntriesInUse = 0;
      if (free <= this.maxUnusedCached) {
        return;
      }
      int i = this.head;
      for (; free > 0; free--)
      {
        if (!freeEntry(this.entries[i])) {
          break;
        }
        i = nextIdx(i);
      }
      this.head = i;
    }
    
    private static boolean freeEntry(Entry entry)
    {
      PoolChunk chunk = entry.chunk;
      if (chunk == null) {
        return false;
      }
      synchronized (chunk.arena)
      {
        chunk.parent.free(chunk, entry.handle);
      }
      entry.chunk = null;
      return true;
    }
    
    private int size()
    {
      return this.tail - this.head & this.entries.length - 1;
    }
    
    private int nextIdx(int index)
    {
      return index + 1 & this.entries.length - 1;
    }
    
    private static final class Entry<T>
    {
      PoolChunk<T> chunk;
      long handle;
    }
  }
}
