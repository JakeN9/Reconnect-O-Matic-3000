package org.spacehq.mc.protocol.packet.ingame.client.player;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.values.Face;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientPlayerPlaceBlockPacket
  implements Packet
{
  private Position position;
  private Face face;
  private ItemStack held;
  private float cursorX;
  private float cursorY;
  private float cursorZ;
  
  private ClientPlayerPlaceBlockPacket() {}
  
  public ClientPlayerPlaceBlockPacket(Position position, Face face, ItemStack held, float cursorX, float cursorY, float cursorZ)
  {
    this.position = position;
    this.face = face;
    this.held = held;
    this.cursorX = cursorX;
    this.cursorY = cursorY;
    this.cursorZ = cursorZ;
  }
  
  public Position getPosition()
  {
    return this.position;
  }
  
  public Face getFace()
  {
    return this.face;
  }
  
  public ItemStack getHeldItem()
  {
    return this.held;
  }
  
  public float getCursorX()
  {
    return this.cursorX;
  }
  
  public float getCursorY()
  {
    return this.cursorY;
  }
  
  public float getCursorZ()
  {
    return this.cursorZ;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.position = NetUtil.readPosition(in);
    this.face = ((Face)MagicValues.key(Face.class, Integer.valueOf(in.readUnsignedByte())));
    this.held = NetUtil.readItem(in);
    this.cursorX = (in.readByte() / 16.0F);
    this.cursorY = (in.readByte() / 16.0F);
    this.cursorZ = (in.readByte() / 16.0F);
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    NetUtil.writePosition(out, this.position);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.face)).intValue());
    NetUtil.writeItem(out, this.held);
    out.writeByte((int)(this.cursorX * 16.0F));
    out.writeByte((int)(this.cursorY * 16.0F));
    out.writeByte((int)(this.cursorZ * 16.0F));
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
