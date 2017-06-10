package org.spacehq.mc.protocol.packet.ingame.server.entity.spawn;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.EntityMetadata;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.MobType;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerSpawnMobPacket
  implements Packet
{
  private int entityId;
  private MobType type;
  private double x;
  private double y;
  private double z;
  private float pitch;
  private float yaw;
  private float headYaw;
  private double motX;
  private double motY;
  private double motZ;
  private EntityMetadata[] metadata;
  
  private ServerSpawnMobPacket() {}
  
  public ServerSpawnMobPacket(int entityId, MobType type, double x, double y, double z, float yaw, float pitch, float headYaw, double motX, double motY, double motZ, EntityMetadata[] metadata)
  {
    this.entityId = entityId;
    this.type = type;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.headYaw = headYaw;
    this.motX = motX;
    this.motY = motY;
    this.motZ = motZ;
    this.metadata = metadata;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public MobType getType()
  {
    return this.type;
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
  
  public float getHeadYaw()
  {
    return this.headYaw;
  }
  
  public double getMotionX()
  {
    return this.motX;
  }
  
  public double getMotionY()
  {
    return this.motY;
  }
  
  public double getMotionZ()
  {
    return this.motZ;
  }
  
  public EntityMetadata[] getMetadata()
  {
    return this.metadata;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.type = ((MobType)MagicValues.key(MobType.class, Byte.valueOf(in.readByte())));
    this.x = (in.readInt() / 32.0D);
    this.y = (in.readInt() / 32.0D);
    this.z = (in.readInt() / 32.0D);
    this.yaw = (in.readByte() * 360 / 256.0F);
    this.pitch = (in.readByte() * 360 / 256.0F);
    this.headYaw = (in.readByte() * 360 / 256.0F);
    this.motX = (in.readShort() / 8000.0D);
    this.motY = (in.readShort() / 8000.0D);
    this.motZ = (in.readShort() / 8000.0D);
    this.metadata = NetUtil.readEntityMetadata(in);
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.type)).intValue());
    out.writeInt((int)(this.x * 32.0D));
    out.writeInt((int)(this.y * 32.0D));
    out.writeInt((int)(this.z * 32.0D));
    out.writeByte((byte)(int)(this.yaw * 256.0F / 360.0F));
    out.writeByte((byte)(int)(this.pitch * 256.0F / 360.0F));
    out.writeByte((byte)(int)(this.headYaw * 256.0F / 360.0F));
    out.writeShort((int)(this.motX * 8000.0D));
    out.writeShort((int)(this.motY * 8000.0D));
    out.writeShort((int)(this.motZ * 8000.0D));
    NetUtil.writeEntityMetadata(out, this.metadata);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
