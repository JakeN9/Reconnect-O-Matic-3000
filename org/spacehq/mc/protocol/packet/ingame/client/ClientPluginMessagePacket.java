package org.spacehq.mc.protocol.packet.ingame.client;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientPluginMessagePacket
  implements Packet
{
  private String channel;
  private byte[] data;
  
  private ClientPluginMessagePacket() {}
  
  public ClientPluginMessagePacket(String channel, byte[] data)
  {
    this.channel = channel;
    this.data = data;
  }
  
  public String getChannel()
  {
    return this.channel;
  }
  
  public byte[] getData()
  {
    return this.data;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.channel = in.readString();
    this.data = in.readBytes(in.available());
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeString(this.channel);
    out.writeBytes(this.data);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
