package org.spacehq.packetlib.tcp;

import java.net.Proxy;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.ConnectionListener;
import org.spacehq.packetlib.Server;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.SessionFactory;

public class TcpSessionFactory
  implements SessionFactory
{
  private Proxy clientProxy;
  
  public TcpSessionFactory() {}
  
  public TcpSessionFactory(Proxy clientProxy)
  {
    this.clientProxy = clientProxy;
  }
  
  public Session createClientSession(Client client)
  {
    return new TcpClientSession(client.getHost(), client.getPort(), client.getPacketProtocol(), client, this.clientProxy);
  }
  
  public ConnectionListener createServerListener(Server server)
  {
    return new TcpConnectionListener(server.getHost(), server.getPort(), server);
  }
}
