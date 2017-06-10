package org.spacehq.mc.protocol.packet.login.server;

import java.io.IOException;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class LoginDisconnectPacket
  implements Packet
{
  private Message message;
  
  private LoginDisconnectPacket() {}
  
  public LoginDisconnectPacket(String text)
  {
    this(Message.fromString(text));
  }
  
  public LoginDisconnectPacket(Message message)
  {
    this.message = message;
  }
  
  public Message getReason()
  {
    return this.message;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.message = Message.fromString(in.readString());
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeString(this.message.toJsonString());
  }
  
  public boolean isPriority()
  {
    return true;
  }
}
