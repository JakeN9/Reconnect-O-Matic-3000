package org.spacehq.mc.protocol;

import java.math.BigInteger;
import java.net.Proxy;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.auth.service.SessionService;
import org.spacehq.mc.protocol.data.SubProtocol;
import org.spacehq.mc.protocol.data.status.ServerStatusInfo;
import org.spacehq.mc.protocol.data.status.handler.ServerInfoBuilder;
import org.spacehq.mc.protocol.packet.handshake.client.HandshakePacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerDisconnectPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import org.spacehq.mc.protocol.packet.login.client.EncryptionResponsePacket;
import org.spacehq.mc.protocol.packet.login.client.LoginStartPacket;
import org.spacehq.mc.protocol.packet.login.server.EncryptionRequestPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginDisconnectPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginSetCompressionPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginSuccessPacket;
import org.spacehq.mc.protocol.packet.status.client.StatusPingPacket;
import org.spacehq.mc.protocol.packet.status.client.StatusQueryPacket;
import org.spacehq.mc.protocol.packet.status.server.StatusPongPacket;
import org.spacehq.mc.protocol.packet.status.server.StatusResponsePacket;
import org.spacehq.mc.protocol.util.CryptUtil;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.event.session.ConnectedEvent;
import org.spacehq.packetlib.event.session.DisconnectingEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.SessionAdapter;

public class ServerListener
  extends SessionAdapter
{
  private static final KeyPair KEY_PAIR = ;
  private byte[] verifyToken = new byte[4];
  private String serverId = "";
  private String username = "";
  private long lastPingTime = 0L;
  private int lastPingId = 0;
  
  public ServerListener()
  {
    new Random().nextBytes(this.verifyToken);
  }
  
  public void connected(ConnectedEvent event)
  {
    event.getSession().setFlag("ping", Integer.valueOf(0));
  }
  
  public void packetReceived(PacketReceivedEvent event)
  {
    MinecraftProtocol protocol = (MinecraftProtocol)event.getSession().getPacketProtocol();
    if ((protocol.getSubProtocol() == SubProtocol.HANDSHAKE) && 
      ((event.getPacket() instanceof HandshakePacket)))
    {
      HandshakePacket packet = (HandshakePacket)event.getPacket();
      switch (packet.getIntent())
      {
      case STATUS: 
        protocol.setSubProtocol(SubProtocol.STATUS, false, event.getSession());
        break;
      case LOGIN: 
        protocol.setSubProtocol(SubProtocol.LOGIN, false, event.getSession());
        if (packet.getProtocolVersion() > 47) {
          event.getSession().disconnect("Outdated server! I'm still on 1.8.8.");
        } else if (packet.getProtocolVersion() < 47) {
          event.getSession().disconnect("Outdated client! Please use 1.8.8.");
        }
        break;
      default: 
        throw new UnsupportedOperationException("Invalid client intent: " + packet.getIntent());
      }
    }
    if (protocol.getSubProtocol() == SubProtocol.LOGIN) {
      if ((event.getPacket() instanceof LoginStartPacket))
      {
        this.username = ((LoginStartPacket)event.getPacket()).getUsername();
        
        boolean verify = event.getSession().hasFlag("verify-users") ? ((Boolean)event.getSession().getFlag("verify-users")).booleanValue() : true;
        if (verify) {
          event.getSession().send(new EncryptionRequestPacket(this.serverId, KEY_PAIR.getPublic(), this.verifyToken));
        } else {
          new Thread(new UserAuthTask(event.getSession(), null)).start();
        }
      }
      else if ((event.getPacket() instanceof EncryptionResponsePacket))
      {
        EncryptionResponsePacket packet = (EncryptionResponsePacket)event.getPacket();
        PrivateKey privateKey = KEY_PAIR.getPrivate();
        if (!Arrays.equals(this.verifyToken, packet.getVerifyToken(privateKey)))
        {
          event.getSession().disconnect("Invalid nonce!");
          return;
        }
        SecretKey key = packet.getSecretKey(privateKey);
        protocol.enableEncryption(key);
        new Thread(new UserAuthTask(event.getSession(), key)).start();
      }
    }
    if (protocol.getSubProtocol() == SubProtocol.STATUS) {
      if ((event.getPacket() instanceof StatusQueryPacket))
      {
        ServerInfoBuilder builder = (ServerInfoBuilder)event.getSession().getFlag("info-builder");
        if (builder == null)
        {
          event.getSession().disconnect("No server info builder set.");
          return;
        }
        ServerStatusInfo info = builder.buildInfo(event.getSession());
        event.getSession().send(new StatusResponsePacket(info));
      }
      else if ((event.getPacket() instanceof StatusPingPacket))
      {
        event.getSession().send(new StatusPongPacket(((StatusPingPacket)event.getPacket()).getPingTime()));
      }
    }
    if ((protocol.getSubProtocol() == SubProtocol.GAME) && 
      ((event.getPacket() instanceof ClientKeepAlivePacket)))
    {
      ClientKeepAlivePacket packet = (ClientKeepAlivePacket)event.getPacket();
      if (packet.getPingId() == this.lastPingId)
      {
        long time = System.currentTimeMillis() - this.lastPingTime;
        event.getSession().setFlag("ping", Long.valueOf(time));
      }
    }
  }
  
  public void disconnecting(DisconnectingEvent event)
  {
    MinecraftProtocol protocol = (MinecraftProtocol)event.getSession().getPacketProtocol();
    if (protocol.getSubProtocol() == SubProtocol.LOGIN) {
      event.getSession().send(new LoginDisconnectPacket(event.getReason()));
    } else if (protocol.getSubProtocol() == SubProtocol.GAME) {
      event.getSession().send(new ServerDisconnectPacket(event.getReason()));
    }
  }
  
  private class UserAuthTask
    implements Runnable
  {
    private Session session;
    private SecretKey key;
    
    public UserAuthTask(Session session, SecretKey key)
    {
      this.key = key;
      this.session = session;
    }
    
    public void run()
    {
      boolean verify = this.session.hasFlag("verify-users") ? ((Boolean)this.session.getFlag("verify-users")).booleanValue() : true;
      
      GameProfile profile = null;
      if ((verify) && (this.key != null))
      {
        Proxy proxy = (Proxy)this.session.getFlag("auth-proxy");
        if (proxy == null) {
          proxy = Proxy.NO_PROXY;
        }
        try
        {
          profile = new SessionService(proxy).getProfileByServer(ServerListener.this.username, new BigInteger(CryptUtil.getServerIdHash(ServerListener.this.serverId, ServerListener.KEY_PAIR.getPublic(), this.key)).toString(16));
        }
        catch (RequestException e)
        {
          this.session.disconnect("Failed to make session service request.", e);
          return;
        }
        if (profile == null) {
          this.session.disconnect("Failed to verify username.");
        }
      }
      else
      {
        profile = new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + ServerListener.this.username).getBytes()), ServerListener.this.username);
      }
      int threshold = ((Integer)this.session.getFlag("compression-threshold")).intValue();
      this.session.send(new LoginSetCompressionPacket(threshold));
      this.session.setCompressionThreshold(threshold);
      this.session.send(new LoginSuccessPacket(profile));
      this.session.setFlag("profile", profile);
      ((MinecraftProtocol)this.session.getPacketProtocol()).setSubProtocol(SubProtocol.GAME, false, this.session);
      ServerLoginHandler handler = (ServerLoginHandler)this.session.getFlag("login-handler");
      if (handler != null) {
        handler.loggedIn(this.session);
      }
      new Thread(new ServerListener.KeepAliveTask(ServerListener.this, this.session)).start();
    }
  }
  
  private class KeepAliveTask
    implements Runnable
  {
    private Session session;
    
    public KeepAliveTask(Session session)
    {
      this.session = session;
    }
    
    public void run()
    {
      for (;;)
      {
        if (this.session.isConnected())
        {
          ServerListener.this.lastPingTime = System.currentTimeMillis();
          ServerListener.this.lastPingId = ((int)ServerListener.this.lastPingTime);
          this.session.send(new ServerKeepAlivePacket(ServerListener.this.lastPingId));
          try
          {
            Thread.sleep(2000L);
          }
          catch (InterruptedException e) {}
        }
      }
    }
  }
}
