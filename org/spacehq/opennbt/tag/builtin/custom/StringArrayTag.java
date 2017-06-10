package org.spacehq.opennbt.tag.builtin.custom;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.spacehq.opennbt.tag.builtin.Tag;

public class StringArrayTag
  extends Tag
{
  private String[] value;
  
  public StringArrayTag(String name)
  {
    this(name, new String[0]);
  }
  
  public StringArrayTag(String name, String[] value)
  {
    super(name);
    this.value = value;
  }
  
  public String[] getValue()
  {
    return (String[])this.value.clone();
  }
  
  public void setValue(String[] value)
  {
    if (value == null) {
      return;
    }
    this.value = ((String[])value.clone());
  }
  
  public String getValue(int index)
  {
    return this.value[index];
  }
  
  public void setValue(int index, String value)
  {
    this.value[index] = value;
  }
  
  public int length()
  {
    return this.value.length;
  }
  
  public void read(DataInputStream in)
    throws IOException
  {
    this.value = new String[in.readInt()];
    for (int index = 0; index < this.value.length; index++) {
      this.value[index] = in.readUTF();
    }
  }
  
  public void write(DataOutputStream out)
    throws IOException
  {
    out.writeInt(this.value.length);
    for (int index = 0; index < this.value.length; index++) {
      out.writeUTF(this.value[index]);
    }
  }
  
  public StringArrayTag clone()
  {
    return new StringArrayTag(getName(), getValue());
  }
}
