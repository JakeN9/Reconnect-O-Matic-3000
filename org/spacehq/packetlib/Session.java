package org.spacehq.packetlib;

import java.util.List;
import java.util.Map;
import org.spacehq.packetlib.event.session.SessionEvent;
import org.spacehq.packetlib.event.session.SessionListener;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.packet.PacketProtocol;

public abstract interface Session
{
  public abstract void connect();
  
  public abstract void connect(boolean paramBoolean);
  
  public abstract String getHost();
  
  public abstract int getPort();
  
  public abstract PacketProtocol getPacketProtocol();
  
  public abstract Map<String, Object> getFlags();
  
  public abstract boolean hasFlag(String paramString);
  
  public abstract <T> T getFlag(String paramString);
  
  public abstract void setFlag(String paramString, Object paramObject);
  
  public abstract List<SessionListener> getListeners();
  
  public abstract void addListener(SessionListener paramSessionListener);
  
  public abstract void removeListener(SessionListener paramSessionListener);
  
  public abstract void callEvent(SessionEvent paramSessionEvent);
  
  public abstract int getCompressionThreshold();
  
  public abstract void setCompressionThreshold(int paramInt);
  
  public abstract int getConnectTimeout();
  
  public abstract void setConnectTimeout(int paramInt);
  
  public abstract int getReadTimeout();
  
  public abstract void setReadTimeout(int paramInt);
  
  public abstract int getWriteTimeout();
  
  public abstract void setWriteTimeout(int paramInt);
  
  public abstract boolean isConnected();
  
  public abstract void send(Packet paramPacket);
  
  public abstract void disconnect(String paramString);
  
  public abstract void disconnect(String paramString, boolean paramBoolean);
  
  public abstract void disconnect(String paramString, Throwable paramThrowable);
  
  public abstract void disconnect(String paramString, Throwable paramThrowable, boolean paramBoolean);
}
