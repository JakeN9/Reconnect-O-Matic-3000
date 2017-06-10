package org.spacehq.mc.protocol.packet.ingame.server.world;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.data.game.values.world.notify.ClientNotification;
import org.spacehq.mc.protocol.data.game.values.world.notify.ClientNotificationValue;
import org.spacehq.mc.protocol.data.game.values.world.notify.DemoMessageValue;
import org.spacehq.mc.protocol.data.game.values.world.notify.RainStrengthValue;
import org.spacehq.mc.protocol.data.game.values.world.notify.ThunderStrengthValue;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerNotifyClientPacket
  implements Packet
{
  private ClientNotification notification;
  private ClientNotificationValue value;
  
  private ServerNotifyClientPacket() {}
  
  public ServerNotifyClientPacket(ClientNotification notification, ClientNotificationValue value)
  {
    this.notification = notification;
    this.value = value;
  }
  
  public ClientNotification getNotification()
  {
    return this.notification;
  }
  
  public ClientNotificationValue getValue()
  {
    return this.value;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.notification = ((ClientNotification)MagicValues.key(ClientNotification.class, Integer.valueOf(in.readUnsignedByte())));
    float value = in.readFloat();
    if (this.notification == ClientNotification.CHANGE_GAMEMODE) {
      this.value = ((ClientNotificationValue)MagicValues.key(GameMode.class, Integer.valueOf((int)value)));
    } else if (this.notification == ClientNotification.DEMO_MESSAGE) {
      this.value = ((ClientNotificationValue)MagicValues.key(DemoMessageValue.class, Integer.valueOf((int)value)));
    } else if (this.notification == ClientNotification.RAIN_STRENGTH) {
      this.value = new RainStrengthValue(value);
    } else if (this.notification == ClientNotification.THUNDER_STRENGTH) {
      this.value = new ThunderStrengthValue(value);
    }
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.notification)).intValue());
    float value = 0.0F;
    if ((this.value instanceof GameMode)) {
      value = ((Integer)MagicValues.value(Integer.class, (Enum)this.value)).intValue();
    }
    if ((this.value instanceof DemoMessageValue)) {
      value = ((Integer)MagicValues.value(Integer.class, (Enum)this.value)).intValue();
    }
    if ((this.value instanceof RainStrengthValue)) {
      value = ((RainStrengthValue)this.value).getStrength();
    }
    if ((this.value instanceof ThunderStrengthValue)) {
      value = ((ThunderStrengthValue)this.value).getStrength();
    }
    out.writeFloat(value);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
