package com.jaken9.reconbot;

import java.io.PrintStream;
import java.net.Proxy;
import java.util.Arrays;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.SubProtocol;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.data.status.PlayerInfo;
import org.spacehq.mc.protocol.data.status.ServerStatusInfo;
import org.spacehq.mc.protocol.data.status.VersionInfo;
import org.spacehq.mc.protocol.data.status.handler.ServerPingTimeHandler;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.event.session.ConnectedEvent;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

public class reconbot
{
	
	/*
		This script was pieced together using limited knowledge of MCProtocol, 2 cups of coffee and example code provided in the MCProtocol library.
	
	*/
	
  private static final Proxy PROXY = Proxy.NO_PROXY;
  private static final Proxy AUTH_PROXY = Proxy.NO_PROXY;
  
  public static void main(String[] args) {
	  // the main portion of code.
    status();
    login();
  }
  
  private static void status()
  {
	  // Connects to the server and gets information about it.
    MinecraftProtocol protocol = new MinecraftProtocol(SubProtocol.STATUS);
    Client client = new Client("INSERT IP ADDRESS HERE", 25565, protocol, new TcpSessionFactory(PROXY));
    client.getSession().setFlag("auth-proxy", AUTH_PROXY);
    client.getSession().setFlag("server-info-handler", new org.spacehq.mc.protocol.data.status.handler.ServerInfoHandler()
    {
		// Prints server information for debugging.
      public void handle(Session session, ServerStatusInfo info)
      {
        System.out.println("Version: " + info.getVersionInfo().getVersionName() + ", " + info.getVersionInfo().getProtocolVersion());
        System.out.println("Player Count: " + info.getPlayerInfo().getOnlinePlayers() + " / " + info.getPlayerInfo().getMaxPlayers());
        System.out.println("Players: " + Arrays.toString(info.getPlayerInfo().getPlayers()));
        System.out.println("Description: " + info.getDescription().getFullText());
        System.out.println("Icon: " + info.getIcon());
      }
    });
    client.getSession().setFlag("server-ping-time-handler", new ServerPingTimeHandler()
    {
      public void handle(Session session, long pingTime)
      {
        System.out.println("Server ping took " + pingTime + "ms");
      }
    });
	// Makes the client connect to the server.
    client.getSession().connect();
    while (client.getSession().isConnected()) {
      try
      {
		  // This script was written for a raspberry pi, this code helped it cope for some strange reason.
        Thread.sleep(10L);
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
      }
    }
  }
  
  private static void login()
  {
    MinecraftProtocol protocol = null;
    try
    {
		// Authenticates the bot with minecraft.net
      protocol = new MinecraftProtocol("INSERT USERNAME HERE", "INSERT PASSWORD HERE", false);
      System.out.println("Bot > Authenticated bot.");
    }
    catch (RequestException e)
    {
      e.printStackTrace();
      return;
    }
	// creates a connection to the server.
    Client client = new Client("mc.snapcraft.net", 25565, protocol, new TcpSessionFactory(PROXY));
    client.getSession().setFlag("auth-proxy", AUTH_PROXY);
    client.getSession().addListener(new org.spacehq.packetlib.event.session.SessionAdapter()
    {
      public void packetReceived(PacketReceivedEvent event)
      {
        if ((event.getPacket() instanceof ServerChatPacket))
        {
			// if instance of a chat packet is recieved print it out to the console.
          Message message = ((ServerChatPacket)event.getPacket()).getMessage();
          System.out.println(message.getFullText());
        }
      }
      
      public void disconnected(DisconnectedEvent event)
      {
		  // Print the reason for disconnection in the case of a disconnect.
        System.out.println("Disconnected: " + Message.fromString(event.getReason()).getFullText());
        if (event.getCause() != null) {
          event.getCause().printStackTrace();
        }
      }
      
      public void connected(ConnectedEvent event)
      {
		  // Create a new thread for anti-afking.
        final Session session = event.getSession();
        Thread thread = new Thread()
        {
          public void run()
          {
            for (;;)
            {
              try
              {
				  // wait 40 seconds to prevent spam.
                sleep(40000L);
              }
              catch (InterruptedException e)
              {
                e.printStackTrace();
              }
			  // make the bot go back to the factions server.
              System.out.println("Bot > Typing /factions.");
              session.send(new org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket("/factions"));
            }
            
          }
        };
        thread.start();
      }
    });
    client.getSession().connect();
  }
}

