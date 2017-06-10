package org.spacehq.mc.protocol.packet.status.client;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class StatusQueryPacket
  implements Packet
{
  public void read(NetInput in)
    throws IOException
  {}
  
  public void write(NetOutput out)
    throws IOException
  {}
  
  public boolean isPriority()
  {
    return false;
  }
}
