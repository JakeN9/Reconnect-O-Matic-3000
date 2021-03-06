package org.spacehq.mc.protocol.packet.handshake.client;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.HandshakeIntent;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class HandshakePacket
  implements Packet
{
  private int protocolVersion;
  private String hostname;
  private int port;
  private HandshakeIntent intent;
  
  private HandshakePacket() {}
  
  public HandshakePacket(int protocolVersion, String hostname, int port, HandshakeIntent intent)
  {
    this.protocolVersion = protocolVersion;
    this.hostname = hostname;
    this.port = port;
    this.intent = intent;
  }
  
  public int getProtocolVersion()
  {
    return this.protocolVersion;
  }
  
  public String getHostName()
  {
    return this.hostname;
  }
  
  public int getPort()
  {
    return this.port;
  }
  
  public HandshakeIntent getIntent()
  {
    return this.intent;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.protocolVersion = in.readVarInt();
    this.hostname = in.readString();
    this.port = in.readUnsignedShort();
    this.intent = ((HandshakeIntent)MagicValues.key(HandshakeIntent.class, Integer.valueOf(in.readVarInt())));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.protocolVersion);
    out.writeString(this.hostname);
    out.writeShort(this.port);
    out.writeVarInt(((Integer)MagicValues.value(Integer.class, this.intent)).intValue());
  }
  
  public boolean isPriority()
  {
    return true;
  }
}
