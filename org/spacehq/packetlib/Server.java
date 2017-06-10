package org.spacehq.packetlib;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.spacehq.packetlib.event.server.ServerBoundEvent;
import org.spacehq.packetlib.event.server.ServerClosedEvent;
import org.spacehq.packetlib.event.server.ServerClosingEvent;
import org.spacehq.packetlib.event.server.ServerEvent;
import org.spacehq.packetlib.event.server.ServerListener;
import org.spacehq.packetlib.event.server.SessionAddedEvent;
import org.spacehq.packetlib.event.server.SessionRemovedEvent;
import org.spacehq.packetlib.packet.PacketProtocol;

public class Server
{
  private String host;
  private int port;
  private Class<? extends PacketProtocol> protocol;
  private SessionFactory factory;
  private ConnectionListener listener;
  private List<Session> sessions = new ArrayList();
  private Map<String, Object> flags = new HashMap();
  private List<ServerListener> listeners = new ArrayList();
  
  public Server(String host, int port, Class<? extends PacketProtocol> protocol, SessionFactory factory)
  {
    this.host = host;
    this.port = port;
    this.protocol = protocol;
    this.factory = factory;
  }
  
  public Server bind()
  {
    return bind(true);
  }
  
  public Server bind(boolean wait)
  {
    this.listener = this.factory.createServerListener(this);
    this.listener.bind(wait, new Runnable()
    {
      public void run()
      {
        Server.this.callEvent(new ServerBoundEvent(Server.this));
      }
    });
    return this;
  }
  
  public String getHost()
  {
    return this.host;
  }
  
  public int getPort()
  {
    return this.port;
  }
  
  public Class<? extends PacketProtocol> getPacketProtocol()
  {
    return this.protocol;
  }
  
  public PacketProtocol createPacketProtocol()
  {
    try
    {
      Constructor<? extends PacketProtocol> constructor = this.protocol.getDeclaredConstructor(new Class[0]);
      if (!constructor.isAccessible()) {
        constructor.setAccessible(true);
      }
      return (PacketProtocol)constructor.newInstance(new Object[0]);
    }
    catch (NoSuchMethodError e)
    {
      throw new IllegalStateException("PacketProtocol \"" + this.protocol.getName() + "\" does not have a no-params constructor for instantiation.");
    }
    catch (Exception e)
    {
      throw new IllegalStateException("Failed to instantiate PacketProtocol " + this.protocol.getName() + ".", e);
    }
  }
  
  public Map<String, Object> getGlobalFlags()
  {
    return new HashMap(this.flags);
  }
  
  public boolean hasGlobalFlag(String key)
  {
    return this.flags.containsKey(key);
  }
  
  public <T> T getGlobalFlag(String key)
  {
    Object value = this.flags.get(key);
    if (value == null) {
      return null;
    }
    try
    {
      return (T)value;
    }
    catch (ClassCastException e)
    {
      throw new IllegalStateException("Tried to get flag \"" + key + "\" as the wrong type. Actual type: " + value.getClass().getName());
    }
  }
  
  public void setGlobalFlag(String key, Object value)
  {
    this.flags.put(key, value);
  }
  
  public List<ServerListener> getListeners()
  {
    return new ArrayList(this.listeners);
  }
  
  public void addListener(ServerListener listener)
  {
    this.listeners.add(listener);
  }
  
  public void removeListener(ServerListener listener)
  {
    this.listeners.remove(listener);
  }
  
  public void callEvent(ServerEvent event)
  {
    for (ServerListener listener : this.listeners) {
      event.call(listener);
    }
  }
  
  public List<Session> getSessions()
  {
    return new ArrayList(this.sessions);
  }
  
  public void addSession(Session session)
  {
    this.sessions.add(session);
    callEvent(new SessionAddedEvent(this, session));
  }
  
  public void removeSession(Session session)
  {
    this.sessions.remove(session);
    if (session.isConnected()) {
      session.disconnect("Connection closed.");
    }
    callEvent(new SessionRemovedEvent(this, session));
  }
  
  public boolean isListening()
  {
    return this.listener.isListening();
  }
  
  public void close()
  {
    close(true);
  }
  
  public void close(boolean wait)
  {
    callEvent(new ServerClosingEvent(this));
    for (Session session : getSessions()) {
      if (session.isConnected()) {
        session.disconnect("Server closed.");
      }
    }
    this.listener.close(wait, new Runnable()
    {
      public void run()
      {
        Server.this.callEvent(new ServerClosedEvent(Server.this));
      }
    });
  }
}
