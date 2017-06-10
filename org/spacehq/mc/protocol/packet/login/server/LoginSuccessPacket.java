package org.spacehq.mc.protocol.packet.login.server;

import java.io.IOException;
import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class LoginSuccessPacket
  implements Packet
{
  private GameProfile profile;
  
  private LoginSuccessPacket() {}
  
  public LoginSuccessPacket(GameProfile profile)
  {
    this.profile = profile;
  }
  
  public GameProfile getProfile()
  {
    return this.profile;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.profile = new GameProfile(in.readString(), in.readString());
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeString(this.profile.getIdAsString());
    out.writeString(this.profile.getName());
  }
  
  public boolean isPriority()
  {
    return true;
  }
}
