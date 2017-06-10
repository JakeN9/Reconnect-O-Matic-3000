package org.spacehq.mc.protocol.packet.ingame.server;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerSwitchCameraPacket
  implements Packet
{
  private int cameraEntityId;
  
  private ServerSwitchCameraPacket() {}
  
  public ServerSwitchCameraPacket(int cameraEntityId)
  {
    this.cameraEntityId = cameraEntityId;
  }
  
  public int getCameraEntityId()
  {
    return this.cameraEntityId;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.cameraEntityId = in.readVarInt();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.cameraEntityId);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
