package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ThreadLocalRandom;
import java.nio.ByteBuffer;

public abstract class AbstractConstant<T extends AbstractConstant<T>>
  implements Constant<T>
{
  private final int id;
  private final String name;
  private volatile long uniquifier;
  private ByteBuffer directBuffer;
  
  protected AbstractConstant(int id, String name)
  {
    this.id = id;
    this.name = name;
  }
  
  public final String name()
  {
    return this.name;
  }
  
  public final int id()
  {
    return this.id;
  }
  
  public final String toString()
  {
    return name();
  }
  
  public final int hashCode()
  {
    return super.hashCode();
  }
  
  public final boolean equals(Object obj)
  {
    return super.equals(obj);
  }
  
  public final int compareTo(T o)
  {
    if (this == o) {
      return 0;
    }
    AbstractConstant<T> other = o;
    
    int returnCode = hashCode() - other.hashCode();
    if (returnCode != 0) {
      return returnCode;
    }
    long thisUV = uniquifier();
    long otherUV = other.uniquifier();
    if (thisUV < otherUV) {
      return -1;
    }
    if (thisUV > otherUV) {
      return 1;
    }
    throw new Error("failed to compare two different constants");
  }
  
  private long uniquifier()
  {
    long uniquifier;
    if ((uniquifier = this.uniquifier) == 0L) {
      synchronized (this)
      {
        while ((uniquifier = this.uniquifier) == 0L) {
          if (PlatformDependent.hasUnsafe())
          {
            this.directBuffer = ByteBuffer.allocateDirect(1);
            this.uniquifier = PlatformDependent.directBufferAddress(this.directBuffer);
          }
          else
          {
            this.directBuffer = null;
            this.uniquifier = ThreadLocalRandom.current().nextLong();
          }
        }
      }
    }
    return uniquifier;
  }
}
