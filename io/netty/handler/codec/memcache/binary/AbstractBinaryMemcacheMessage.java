package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.memcache.AbstractMemcacheObject;

public abstract class AbstractBinaryMemcacheMessage
  extends AbstractMemcacheObject
  implements BinaryMemcacheMessage
{
  private String key;
  private ByteBuf extras;
  private byte magic;
  private byte opcode;
  private short keyLength;
  private byte extrasLength;
  private byte dataType;
  private int totalBodyLength;
  private int opaque;
  private long cas;
  
  protected AbstractBinaryMemcacheMessage(String key, ByteBuf extras)
  {
    this.key = key;
    this.extras = extras;
  }
  
  public String key()
  {
    return this.key;
  }
  
  public ByteBuf extras()
  {
    return this.extras;
  }
  
  public BinaryMemcacheMessage setKey(String key)
  {
    this.key = key;
    return this;
  }
  
  public BinaryMemcacheMessage setExtras(ByteBuf extras)
  {
    this.extras = extras;
    return this;
  }
  
  public byte magic()
  {
    return this.magic;
  }
  
  public BinaryMemcacheMessage setMagic(byte magic)
  {
    this.magic = magic;
    return this;
  }
  
  public long cas()
  {
    return this.cas;
  }
  
  public BinaryMemcacheMessage setCas(long cas)
  {
    this.cas = cas;
    return this;
  }
  
  public int opaque()
  {
    return this.opaque;
  }
  
  public BinaryMemcacheMessage setOpaque(int opaque)
  {
    this.opaque = opaque;
    return this;
  }
  
  public int totalBodyLength()
  {
    return this.totalBodyLength;
  }
  
  public BinaryMemcacheMessage setTotalBodyLength(int totalBodyLength)
  {
    this.totalBodyLength = totalBodyLength;
    return this;
  }
  
  public byte dataType()
  {
    return this.dataType;
  }
  
  public BinaryMemcacheMessage setDataType(byte dataType)
  {
    this.dataType = dataType;
    return this;
  }
  
  public byte extrasLength()
  {
    return this.extrasLength;
  }
  
  public BinaryMemcacheMessage setExtrasLength(byte extrasLength)
  {
    this.extrasLength = extrasLength;
    return this;
  }
  
  public short keyLength()
  {
    return this.keyLength;
  }
  
  public BinaryMemcacheMessage setKeyLength(short keyLength)
  {
    this.keyLength = keyLength;
    return this;
  }
  
  public byte opcode()
  {
    return this.opcode;
  }
  
  public BinaryMemcacheMessage setOpcode(byte opcode)
  {
    this.opcode = opcode;
    return this;
  }
  
  public int refCnt()
  {
    if (this.extras != null) {
      return this.extras.refCnt();
    }
    return 1;
  }
  
  public BinaryMemcacheMessage retain()
  {
    if (this.extras != null) {
      this.extras.retain();
    }
    return this;
  }
  
  public BinaryMemcacheMessage retain(int increment)
  {
    if (this.extras != null) {
      this.extras.retain(increment);
    }
    return this;
  }
  
  public boolean release()
  {
    if (this.extras != null) {
      return this.extras.release();
    }
    return false;
  }
  
  public boolean release(int decrement)
  {
    if (this.extras != null) {
      return this.extras.release(decrement);
    }
    return false;
  }
  
  public BinaryMemcacheMessage touch()
  {
    return touch(null);
  }
  
  public BinaryMemcacheMessage touch(Object hint)
  {
    if (this.extras != null) {
      this.extras.touch(hint);
    }
    return this;
  }
}
