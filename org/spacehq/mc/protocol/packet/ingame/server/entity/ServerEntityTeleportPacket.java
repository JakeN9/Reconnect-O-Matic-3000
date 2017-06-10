package org.spacehq.mc.protocol.packet.ingame.server.entity;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerEntityTeleportPacket
  implements Packet
{
  private int entityId;
  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;
  private boolean onGround;
  
  private ServerEntityTeleportPacket() {}
  
  public ServerEntityTeleportPacket(int entityId, double x, double y, double z, float yaw, float pitch, boolean onGround)
  {
    this.entityId = entityId;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.onGround = onGround;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public double getX()
  {
    return this.x;
  }
  
  public double getY()
  {
    return this.y;
  }
  
  public double getZ()
  {
    return this.z;
  }
  
  public float getYaw()
  {
    return this.yaw;
  }
  
  public float getPitch()
  {
    return this.pitch;
  }
  
  public boolean isOnGround()
  {
    return this.onGround;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.x = (in.readInt() / 32.0D);
    this.y = (in.readInt() / 32.0D);
    this.z = (in.readInt() / 32.0D);
    this.yaw = (in.readByte() * 360 / 256.0F);
    this.pitch = (in.readByte() * 360 / 256.0F);
    this.onGround = in.readBoolean();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeInt((int)(this.x * 32.0D));
    out.writeInt((int)(this.y * 32.0D));
    out.writeInt((int)(this.z * 32.0D));
    out.writeByte((byte)(int)(this.yaw * 256.0F / 360.0F));
    out.writeByte((byte)(int)(this.pitch * 256.0F / 360.0F));
    out.writeBoolean(this.onGround);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
