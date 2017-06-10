package org.spacehq.mc.protocol.packet.ingame.server.window;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.window.property.WindowProperty;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerWindowPropertyPacket
  implements Packet
{
  private int windowId;
  private int property;
  private int value;
  
  private ServerWindowPropertyPacket() {}
  
  public ServerWindowPropertyPacket(int windowId, int property, int value)
  {
    this.windowId = windowId;
    this.property = property;
    this.value = value;
  }
  
  public <T extends Enum<T>,  extends WindowProperty> ServerWindowPropertyPacket(int windowId, T property, int value)
  {
    this.windowId = windowId;
    this.property = ((Integer)MagicValues.value(Integer.class, property)).intValue();
    this.value = value;
  }
  
  public int getWindowId()
  {
    return this.windowId;
  }
  
  public int getRawProperty()
  {
    return this.property;
  }
  
  public <T extends Enum<T>,  extends WindowProperty> T getProperty(Class<T> type)
  {
    return MagicValues.key(type, Integer.valueOf(this.value));
  }
  
  public int getValue()
  {
    return this.value;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.windowId = in.readUnsignedByte();
    this.property = in.readShort();
    this.value = in.readShort();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeByte(this.windowId);
    out.writeShort(this.property);
    out.writeShort(this.value);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
