package org.spacehq.mc.protocol.packet.ingame.server.entity;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerEntityMovementPacket
  implements Packet
{
  protected int entityId;
  protected double moveX;
  protected double moveY;
  protected double moveZ;
  protected float yaw;
  protected float pitch;
  private boolean onGround;
  protected boolean pos = false;
  protected boolean rot = false;
  
  protected ServerEntityMovementPacket() {}
  
  public ServerEntityMovementPacket(int entityId, boolean onGround)
  {
    this.entityId = entityId;
    this.onGround = onGround;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public double getMovementX()
  {
    return this.moveX;
  }
  
  public double getMovementY()
  {
    return this.moveY;
  }
  
  public double getMovementZ()
  {
    return this.moveZ;
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
    if (this.pos)
    {
      this.moveX = (in.readByte() / 32.0D);
      this.moveY = (in.readByte() / 32.0D);
      this.moveZ = (in.readByte() / 32.0D);
    }
    if (this.rot)
    {
      this.yaw = (in.readByte() * 360 / 256.0F);
      this.pitch = (in.readByte() * 360 / 256.0F);
    }
    this.onGround = in.readBoolean();
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    if (this.pos)
    {
      out.writeByte((int)(this.moveX * 32.0D));
      out.writeByte((int)(this.moveY * 32.0D));
      out.writeByte((int)(this.moveZ * 32.0D));
    }
    if (this.rot)
    {
      out.writeByte((byte)(int)(this.yaw * 256.0F / 360.0F));
      out.writeByte((byte)(int)(this.pitch * 256.0F / 360.0F));
    }
    out.writeBoolean(this.onGround);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
