package org.spacehq.mc.protocol.packet.ingame.server.window;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerCloseWindowPacket
  implements Packet
{
  private int windowId;
  
  private ServerCloseWindowPacket() {}
  
  public ServerCloseWindowPacket(int windowId)
  {
    this.windowId = windowId;
  }
  
  public int getWindowId()
  {
    return this.windowId;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.windowId = in.readUnsignedByte();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeByte(this.windowId);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
