package org.spacehq.mc.protocol.packet.ingame.client;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.ClientRequest;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientRequestPacket
  implements Packet
{
  private ClientRequest request;
  
  private ClientRequestPacket() {}
  
  public ClientRequestPacket(ClientRequest request)
  {
    this.request = request;
  }
  
  public ClientRequest getRequest()
  {
    return this.request;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.request = ((ClientRequest)MagicValues.key(ClientRequest.class, Integer.valueOf(in.readUnsignedByte())));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.request)).intValue());
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
