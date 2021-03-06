package org.spacehq.opennbt.tag.builtin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

public abstract class Tag
  implements Cloneable
{
  private String name;
  
  public Tag(String name)
  {
    this.name = name;
  }
  
  public final String getName()
  {
    return this.name;
  }
  
  public abstract Object getValue();
  
  public abstract void read(DataInputStream paramDataInputStream)
    throws IOException;
  
  public abstract void write(DataOutputStream paramDataOutputStream)
    throws IOException;
  
  public abstract Tag clone();
  
  public boolean equals(Object obj)
  {
    if (!(obj instanceof Tag)) {
      return false;
    }
    Tag tag = (Tag)obj;
    if (!getName().equals(tag.getName())) {
      return false;
    }
    if (getValue() == null) {
      return tag.getValue() == null;
    }
    if (tag.getValue() == null) {
      return false;
    }
    if ((getValue().getClass().isArray()) && (tag.getValue().getClass().isArray()))
    {
      int length = Array.getLength(getValue());
      if (Array.getLength(tag.getValue()) != length) {
        return false;
      }
      for (int index = 0; index < length; index++)
      {
        Object o = Array.get(getValue(), index);
        Object other = Array.get(tag.getValue(), index);
        if (((o == null) && (other != null)) || ((o != null) && (!o.equals(other)))) {
          return false;
        }
      }
      return true;
    }
    return getValue().equals(tag.getValue());
  }
  
  public String toString()
  {
    String name = (getName() != null) && (!getName().equals("")) ? "(" + getName() + ")" : "";
    String value = "";
    if (getValue() != null)
    {
      value = getValue().toString();
      if (getValue().getClass().isArray())
      {
        StringBuilder build = new StringBuilder();
        build.append("[");
        for (int index = 0; index < Array.getLength(getValue()); index++)
        {
          if (index > 0) {
            build.append(", ");
          }
          build.append(Array.get(getValue(), index));
        }
        build.append("]");
        value = build.toString();
      }
    }
    return getClass().getSimpleName() + name + " { " + value + " }";
  }
}
