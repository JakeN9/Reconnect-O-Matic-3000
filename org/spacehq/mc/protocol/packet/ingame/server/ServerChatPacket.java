package org.spacehq.mc.protocol.packet.ingame.server;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.MessageType;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerChatPacket
  implements Packet
{
  private Message message;
  private MessageType type;
  
  private ServerChatPacket() {}
  
  public ServerChatPacket(String text)
  {
    this(Message.fromString(text));
  }
  
  public ServerChatPacket(Message message)
  {
    this(message, MessageType.SYSTEM);
  }
  
  public ServerChatPacket(String text, MessageType type)
  {
    this(Message.fromString(text), type);
  }
  
  public ServerChatPacket(Message message, MessageType type)
  {
    this.message = message;
    this.type = type;
  }
  
  public Message getMessage()
  {
    return this.message;
  }
  
  public MessageType getType()
  {
    return this.type;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.message = Message.fromString(in.readString());
    this.type = ((MessageType)MagicValues.key(MessageType.class, Byte.valueOf(in.readByte())));
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeString(this.message.toJsonString());
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.type)).intValue());
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
