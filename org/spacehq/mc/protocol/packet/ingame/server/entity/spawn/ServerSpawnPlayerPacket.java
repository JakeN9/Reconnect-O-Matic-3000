package org.spacehq.mc.protocol.packet.ingame.server.entity.spawn;

import java.io.IOException;
import java.util.UUID;
import org.spacehq.mc.protocol.data.game.EntityMetadata;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerSpawnPlayerPacket
  implements Packet
{
  private int entityId;
  private UUID uuid;
  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;
  private int currentItem;
  private EntityMetadata[] metadata;
  
  private ServerSpawnPlayerPacket() {}
  
  public ServerSpawnPlayerPacket(int entityId, UUID uuid, double x, double y, double z, float yaw, float pitch, int currentItem, EntityMetadata[] metadata)
  {
    this.entityId = entityId;
    this.uuid = uuid;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.currentItem = currentItem;
    this.metadata = metadata;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public UUID getUUID()
  {
    return this.uuid;
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
  
  public int getCurrentItem()
  {
    return this.currentItem;
  }
  
  public EntityMetadata[] getMetadata()
  {
    return this.metadata;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.uuid = in.readUUID();
    this.x = (in.readInt() / 32.0D);
    this.y = (in.readInt() / 32.0D);
    this.z = (in.readInt() / 32.0D);
    this.yaw = (in.readByte() * 360 / 256.0F);
    this.pitch = (in.readByte() * 360 / 256.0F);
    this.currentItem = in.readShort();
    this.metadata = NetUtil.readEntityMetadata(in);
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeUUID(this.uuid);
    out.writeInt((int)(this.x * 32.0D));
    out.writeInt((int)(this.y * 32.0D));
    out.writeInt((int)(this.z * 32.0D));
    out.writeByte((byte)(int)(this.yaw * 256.0F / 360.0F));
    out.writeByte((byte)(int)(this.pitch * 256.0F / 360.0F));
    out.writeShort(this.currentItem);
    NetUtil.writeEntityMetadata(out, this.metadata);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
