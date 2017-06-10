package org.spacehq.mc.protocol.packet.ingame.server;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerResourcePackSendPacket
  implements Packet
{
  private String url;
  private String hash;
  
  private ServerResourcePackSendPacket() {}
  
  public ServerResourcePackSendPacket(String url, String hash)
  {
    this.url = url;
    this.hash = hash;
  }
  
  public String getUrl()
  {
    return this.url;
  }
  
  public String getHash()
  {
    return this.hash;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.url = in.readString();
    this.hash = in.readString();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeString(this.url);
    out.writeString(this.hash);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
