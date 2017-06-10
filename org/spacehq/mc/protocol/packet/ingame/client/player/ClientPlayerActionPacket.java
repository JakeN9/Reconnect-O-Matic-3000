package org.spacehq.mc.protocol.packet.ingame.client.player;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.values.Face;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.PlayerAction;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientPlayerActionPacket
  implements Packet
{
  private PlayerAction action;
  private Position position;
  private Face face;
  
  private ClientPlayerActionPacket() {}
  
  public ClientPlayerActionPacket(PlayerAction action, Position position, Face face)
  {
    this.action = action;
    this.position = position;
    this.face = face;
  }
  
  public PlayerAction getAction()
  {
    return this.action;
  }
  
  public Position getPosition()
  {
    return this.position;
  }
  
  public Face getFace()
  {
    return this.face;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.action = ((PlayerAction)MagicValues.key(PlayerAction.class, Integer.valueOf(in.readUnsignedByte())));
    this.position = NetUtil.readPosition(in);
    this.face = ((Face)MagicValues.key(Face.class, Integer.valueOf(in.readUnsignedByte())));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.action)).intValue());
    NetUtil.writePosition(out, this.position);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.face)).intValue());
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
