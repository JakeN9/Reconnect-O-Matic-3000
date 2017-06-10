package org.spacehq.opennbt.tag.builtin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ShortTag
  extends Tag
{
  private short value;
  
  public ShortTag(String name)
  {
    this(name, (short)0);
  }
  
  public ShortTag(String name, short value)
  {
    super(name);
    this.value = value;
  }
  
  public Short getValue()
  {
    return Short.valueOf(this.value);
  }
  
  public void setValue(short value)
  {
    this.value = value;
  }
  
  public void read(DataInputStream in)
    throws IOException
  {
    this.value = in.readShort();
  }
  
  public void write(DataOutputStream out)
    throws IOException
  {
    out.writeShort(this.value);
  }
  
  public ShortTag clone()
  {
    return new ShortTag(getName(), getValue().shortValue());
  }
}
