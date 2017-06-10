package org.spacehq.mc.protocol.packet.ingame.client;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientChatPacket
  implements Packet
{
  private String message;
  
  private ClientChatPacket() {}
  
  public ClientChatPacket(String message)
  {
    this.message = message;
  }
  
  public String getMessage()
  {
    return this.message;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.message = in.readString();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeString(this.message);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
