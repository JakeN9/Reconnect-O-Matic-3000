package org.spacehq.mc.protocol.packet.ingame.client.window;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.window.ClickItemParam;
import org.spacehq.mc.protocol.data.game.values.window.CreativeGrabParam;
import org.spacehq.mc.protocol.data.game.values.window.DropItemParam;
import org.spacehq.mc.protocol.data.game.values.window.FillStackParam;
import org.spacehq.mc.protocol.data.game.values.window.MoveToHotbarParam;
import org.spacehq.mc.protocol.data.game.values.window.ShiftClickItemParam;
import org.spacehq.mc.protocol.data.game.values.window.SpreadItemParam;
import org.spacehq.mc.protocol.data.game.values.window.WindowAction;
import org.spacehq.mc.protocol.data.game.values.window.WindowActionParam;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ClientWindowActionPacket
  implements Packet
{
  private int windowId;
  private int slot;
  private WindowActionParam param;
  private int actionId;
  private WindowAction action;
  private ItemStack clicked;
  
  private ClientWindowActionPacket() {}
  
  public ClientWindowActionPacket(int windowId, int actionId, int slot, ItemStack clicked, WindowAction action, WindowActionParam param)
  {
    this.windowId = windowId;
    this.actionId = actionId;
    this.slot = slot;
    this.clicked = clicked;
    this.action = action;
    this.param = param;
  }
  
  public int getWindowId()
  {
    return this.windowId;
  }
  
  public int getActionId()
  {
    return this.actionId;
  }
  
  public int getSlot()
  {
    return this.slot;
  }
  
  public ItemStack getClickedItem()
  {
    return this.clicked;
  }
  
  public WindowAction getAction()
  {
    return this.action;
  }
  
  public WindowActionParam getParam()
  {
    return this.param;
  }
  
  public void read(NetInput in)
    throws IOException
  {
    this.windowId = in.readByte();
    this.slot = in.readShort();
    byte param = in.readByte();
    this.actionId = in.readShort();
    this.action = ((WindowAction)MagicValues.key(WindowAction.class, Byte.valueOf(in.readByte())));
    this.clicked = NetUtil.readItem(in);
    if (this.action == WindowAction.CLICK_ITEM) {
      this.param = ((WindowActionParam)MagicValues.key(ClickItemParam.class, Byte.valueOf(param)));
    } else if (this.action == WindowAction.SHIFT_CLICK_ITEM) {
      this.param = ((WindowActionParam)MagicValues.key(ShiftClickItemParam.class, Byte.valueOf(param)));
    } else if (this.action == WindowAction.MOVE_TO_HOTBAR_SLOT) {
      this.param = ((WindowActionParam)MagicValues.key(MoveToHotbarParam.class, Byte.valueOf(param)));
    } else if (this.action == WindowAction.CREATIVE_GRAB_MAX_STACK) {
      this.param = ((WindowActionParam)MagicValues.key(CreativeGrabParam.class, Byte.valueOf(param)));
    } else if (this.action == WindowAction.DROP_ITEM) {
      this.param = ((WindowActionParam)MagicValues.key(DropItemParam.class, Integer.valueOf(param + (this.slot != 64537 ? 2 : 0))));
    } else if (this.action == WindowAction.SPREAD_ITEM) {
      this.param = ((WindowActionParam)MagicValues.key(SpreadItemParam.class, Byte.valueOf(param)));
    } else if (this.action == WindowAction.FILL_STACK) {
      this.param = ((WindowActionParam)MagicValues.key(FillStackParam.class, Byte.valueOf(param)));
    }
  }
  
  public void write(NetOutput out)
    throws IOException
  {
    out.writeByte(this.windowId);
    out.writeShort(this.slot);
    int param = 0;
    if (this.action == WindowAction.CLICK_ITEM) {
      param = ((Integer)MagicValues.value(Integer.class, (Enum)this.param)).intValue();
    } else if (this.action == WindowAction.SHIFT_CLICK_ITEM) {
      param = ((Integer)MagicValues.value(Integer.class, (Enum)this.param)).intValue();
    } else if (this.action == WindowAction.MOVE_TO_HOTBAR_SLOT) {
      param = ((Integer)MagicValues.value(Integer.class, (Enum)this.param)).intValue();
    } else if (this.action == WindowAction.CREATIVE_GRAB_MAX_STACK) {
      param = ((Integer)MagicValues.value(Integer.class, (Enum)this.param)).intValue();
    } else if (this.action == WindowAction.DROP_ITEM) {
      param = ((Integer)MagicValues.value(Integer.class, (Enum)this.param)).intValue() + (this.slot != 64537 ? 2 : 0);
    } else if (this.action == WindowAction.SPREAD_ITEM) {
      param = ((Integer)MagicValues.value(Integer.class, (Enum)this.param)).intValue();
    } else if (this.action == WindowAction.FILL_STACK) {
      param = ((Integer)MagicValues.value(Integer.class, (Enum)this.param)).intValue();
    }
    out.writeByte(param);
    out.writeShort(this.actionId);
    out.writeByte(((Integer)MagicValues.value(Integer.class, this.action)).intValue());
    NetUtil.writeItem(out, this.clicked);
  }
  
  public boolean isPriority()
  {
    return false;
  }
}
