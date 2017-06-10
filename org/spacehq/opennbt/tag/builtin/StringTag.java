package org.spacehq.opennbt.tag.builtin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StringTag
  extends Tag
{
  private String value;
  
  public StringTag(String name)
  {
    this(name, "");
  }
  
  public StringTag(String name, String value)
  {
    super(name);
    this.value = value;
  }
  
  public String getValue()
  {
    return this.value;
  }
  
  public void setValue(String value)
  {
    this.value = value;
  }
  
  public void read(DataInputStream in)
    throws IOException
  {
    this.value = in.readUTF();
  }
  
  public void write(DataOutputStream out)
    throws IOException
  {
    out.writeUTF(this.value);
  }
  
  public StringTag clone()
  {
    return new StringTag(getName(), getValue());
  }
}
