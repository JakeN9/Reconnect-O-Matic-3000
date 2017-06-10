package org.spacehq.mc.protocol.packet.ingame.client.player;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientSteerVehiclePacket
  implements Packet
{
  private float sideways;
  private float forward;
  private boolean jump;
  private boolean dismount;
  
  private ClientSteerVehiclePacket() {}
  
  public ClientSteerVehiclePacket(float sideways, float forward, boolean jump, boolean dismount)
  {
    this.sideways = sideways;
    this.forward = forward;
    this.jump = jump;
    this.dismount = dismount;
  }
  
  public float getSideways()
  {
    return this.sideways;
  }
  
  public float getForward()
  {
    return this.forward;
  }
  
  public boolean getJumping()
  {
    return this.jump;
  }
  
  public boolean getDismounting()
  {
    return this.dismount;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.sideways = in.readFloat();
    this.forward = in.readFloat();
    int flags = in.readUnsignedByte();
    this.jump = ((flags & 0x1) > 0);
    this.dismount = ((flags & 0x2) > 0);
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeFloat(this.sideways);
    out.writeFloat(this.forward);
    byte flags = 0;
    if (this.jump) {
      flags = (byte)(flags | 0x1);
    }
    if (this.dismount) {
      flags = (byte)(flags | 0x2);
    }
    out.writeByte(flags);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
