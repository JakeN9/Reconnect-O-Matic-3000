package io.netty.util;

import java.util.HashMap;
import java.util.Map;

public abstract class ConstantPool<T extends Constant<T>>
{
  private final Map<String, T> constants = new HashMap();
  private int nextId = 1;
  
  public T valueOf(Class<?> firstNameComponent, String secondNameComponent)
  {
    if (firstNameComponent == null) {
      throw new NullPointerException("firstNameComponent");
    }
    if (secondNameComponent == null) {
      throw new NullPointerException("secondNameComponent");
    }
    return valueOf(firstNameComponent.getName() + '#' + secondNameComponent);
  }
  
  public T valueOf(String name)
  {
    if (name == null) {
      throw new NullPointerException("name");
    }
    if (name.isEmpty()) {
      throw new IllegalArgumentException("empty name");
    }
    synchronized (this.constants)
    {
      T c = (Constant)this.constants.get(name);
      if (c == null)
      {
        c = newConstant(this.nextId, name);
        this.constants.put(name, c);
        this.nextId += 1;
      }
      return c;
    }
  }
  
  /* Error */
  public boolean exists(String name)
  {
    // Byte code:
    //   0: aload_1
    //   1: ldc 17
    //   3: invokestatic 26	io/netty/util/internal/ObjectUtil:checkNotNull	(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
    //   6: pop
    //   7: aload_0
    //   8: getfield 4	io/netty/util/ConstantPool:constants	Ljava/util/Map;
    //   11: dup
    //   12: astore_2
    //   13: monitorenter
    //   14: aload_0
    //   15: getfield 4	io/netty/util/ConstantPool:constants	Ljava/util/Map;
    //   18: aload_1
    //   19: invokeinterface 27 2 0
    //   24: aload_2
    //   25: monitorexit
    //   26: ireturn
    //   27: astore_3
    //   28: aload_2
    //   29: monitorexit
    //   30: aload_3
    //   31: athrow
    // Line number table:
    //   Java source line #82	-> byte code offset #0
    //   Java source line #83	-> byte code offset #7
    //   Java source line #84	-> byte code offset #14
    //   Java source line #85	-> byte code offset #27
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	32	0	this	ConstantPool<T>
    //   0	32	1	name	String
    //   12	17	2	Ljava/lang/Object;	Object
    //   27	4	3	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   14	26	27	finally
    //   27	30	27	finally
  }
  
  public T newInstance(String name)
  {
    if (name == null) {
      throw new NullPointerException("name");
    }
    if (name.isEmpty()) {
      throw new IllegalArgumentException("empty name");
    }
    synchronized (this.constants)
    {
      T c = (Constant)this.constants.get(name);
      if (c == null)
      {
        c = newConstant(this.nextId, name);
        this.constants.put(name, c);
        this.nextId += 1;
      }
      else
      {
        throw new IllegalArgumentException(String.format("'%s' is already in use", new Object[] { name }));
      }
      return c;
    }
  }
  
  protected abstract T newConstant(int paramInt, String paramString);
}
