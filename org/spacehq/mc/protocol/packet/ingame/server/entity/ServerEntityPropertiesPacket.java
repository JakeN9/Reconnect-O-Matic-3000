package org.spacehq.mc.protocol.packet.ingame.server.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.spacehq.mc.protocol.data.game.attribute.Attribute;
import org.spacehq.mc.protocol.data.game.attribute.AttributeModifier;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.AttributeType;
import org.spacehq.mc.protocol.data.game.values.entity.ModifierOperation;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerEntityPropertiesPacket
  implements Packet
{
  private int entityId;
  private List<Attribute> attributes;
  
  private ServerEntityPropertiesPacket() {}
  
  public ServerEntityPropertiesPacket(int entityId, List<Attribute> attributes)
  {
    this.entityId = entityId;
    this.attributes = attributes;
  }
  
  public int getEntityId()
  {
    return this.entityId;
  }
  
  public List<Attribute> getAttributes()
  {
    return this.attributes;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.entityId = in.readVarInt();
    this.attributes = new ArrayList();
    int length = in.readInt();
    for (int index = 0; index < length; index++)
    {
      String key = in.readString();
      double value = in.readDouble();
      List<AttributeModifier> modifiers = new ArrayList();
      int len = in.readVarInt();
      for (int ind = 0; ind < len; ind++) {
        modifiers.add(new AttributeModifier(in.readUUID(), in.readDouble(), (ModifierOperation)MagicValues.key(ModifierOperation.class, Byte.valueOf(in.readByte()))));
      }
      this.attributes.add(new Attribute((AttributeType)MagicValues.key(AttributeType.class, key), value, modifiers));
    }
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeVarInt(this.entityId);
    out.writeInt(this.attributes.size());
    for (Attribute attribute : this.attributes)
    {
      out.writeString((String)MagicValues.value(String.class, attribute.getType()));
      out.writeDouble(attribute.getValue());
      out.writeVarInt(attribute.getModifiers().size());
      for (AttributeModifier modifier : attribute.getModifiers())
      {
        out.writeUUID(modifier.getUUID());
        out.writeDouble(modifier.getAmount());
        out.writeByte(((Integer)MagicValues.value(Integer.class, modifier.getOperation())).intValue());
      }
    }
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
