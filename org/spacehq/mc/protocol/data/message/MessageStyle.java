package org.spacehq.mc.protocol.data.message;

import java.util.ArrayList;
import java.util.List;

public class MessageStyle
  implements Cloneable
{
  private static final MessageStyle DEFAULT = new MessageStyle();
  private ChatColor color = ChatColor.WHITE;
  private List<ChatFormat> formats = new ArrayList();
  private ClickEvent click;
  private HoverEvent hover;
  private String insertion;
  private MessageStyle parent = DEFAULT;
  
  public boolean isDefault()
  {
    return equals(DEFAULT);
  }
  
  public ChatColor getColor()
  {
    return this.color;
  }
  
  public List<ChatFormat> getFormats()
  {
    return new ArrayList(this.formats);
  }
  
  public ClickEvent getClickEvent()
  {
    return this.click;
  }
  
  public HoverEvent getHoverEvent()
  {
    return this.hover;
  }
  
  public String getInsertion()
  {
    return this.insertion;
  }
  
  public MessageStyle getParent()
  {
    return this.parent;
  }
  
  public MessageStyle setColor(ChatColor color)
  {
    this.color = color;
    return this;
  }
  
  public MessageStyle setFormats(List<ChatFormat> formats)
  {
    this.formats = new ArrayList(formats);
    return this;
  }
  
  public MessageStyle addFormat(ChatFormat format)
  {
    this.formats.add(format);
    return this;
  }
  
  public MessageStyle removeFormat(ChatFormat format)
  {
    this.formats.remove(format);
    return this;
  }
  
  public MessageStyle clearFormats()
  {
    this.formats.clear();
    return this;
  }
  
  public MessageStyle setClickEvent(ClickEvent event)
  {
    this.click = event;
    return this;
  }
  
  public MessageStyle setHoverEvent(HoverEvent event)
  {
    this.hover = event;
    return this;
  }
  
  public MessageStyle setInsertion(String insertion)
  {
    this.insertion = insertion;
    return this;
  }
  
  protected MessageStyle setParent(MessageStyle parent)
  {
    if (parent == null) {
      parent = DEFAULT;
    }
    this.parent = parent;
    return this;
  }
  
  public String toString()
  {
    return "MessageStyle{color=" + this.color + ",formats=" + this.formats + ",clickEvent=" + this.click + ",hoverEvent=" + this.hover + ",insertion=" + this.insertion + "}";
  }
  
  public MessageStyle clone()
  {
    return new MessageStyle().setParent(this.parent).setColor(this.color).setFormats(this.formats).setClickEvent(this.click != null ? this.click.clone() : null).setHoverEvent(this.hover != null ? this.hover.clone() : null).setInsertion(this.insertion);
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    MessageStyle style = (MessageStyle)o;
    if (this.click != null ? !this.click.equals(style.click) : style.click != null) {
      return false;
    }
    if (this.color != style.color) {
      return false;
    }
    if (!this.formats.equals(style.formats)) {
      return false;
    }
    if (this.hover != null ? !this.hover.equals(style.hover) : style.hover != null) {
      return false;
    }
    if (this.insertion != null ? !this.insertion.equals(style.insertion) : style.insertion != null) {
      return false;
    }
    if (!this.parent.equals(style.parent)) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = this.color != null ? this.color.hashCode() : 0;
    result = 31 * result + this.formats.hashCode();
    result = 31 * result + (this.click != null ? this.click.hashCode() : 0);
    result = 31 * result + (this.hover != null ? this.hover.hashCode() : 0);
    result = 31 * result + (this.insertion != null ? this.insertion.hashCode() : 0);
    result = 31 * result + this.parent.hashCode();
    return result;
  }
}
