package org.spacehq.mc.protocol.packet.login.server;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class LoginSetCompressionPacket
  implements Packet
{
  private int threshold;
  
  private LoginSetCompressionPacket() {}
  
  public LoginSetCompressionPacket(int threshold)
  {
    this.threshold = threshold;
  }
  
  public int getThreshold()
  {
    return this.threshold;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.threshold = in.readVarInt();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.threshold);
  }
  
  public boolean isPriority()
  {
    return true;
  }
}
