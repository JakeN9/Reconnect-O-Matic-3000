package io.netty.channel.embedded;

import io.netty.channel.ChannelId;

final class EmbeddedChannelId
  implements ChannelId
{
  private static final long serialVersionUID = -251711922203466130L;
  static final ChannelId INSTANCE = new EmbeddedChannelId();
  
  public String asShortText()
  {
    return toString();
  }
  
  public String asLongText()
  {
    return toString();
  }
  
  public int compareTo(ChannelId o)
  {
    if (o == INSTANCE) {
      return 0;
    }
    return asLongText().compareTo(o.asLongText());
  }
  
  public int hashCode()
  {
    return super.hashCode();
  }
  
  public boolean equals(Object obj)
  {
    return super.equals(obj);
  }
  
  public String toString()
  {
    return "embedded";
  }
}
