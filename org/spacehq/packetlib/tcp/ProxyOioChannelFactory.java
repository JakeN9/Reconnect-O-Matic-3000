package org.spacehq.packetlib.tcp;

import io.netty.channel.ChannelFactory;
import io.netty.channel.socket.oio.OioSocketChannel;
import java.net.Proxy;
import java.net.Socket;

public class ProxyOioChannelFactory
  implements ChannelFactory<OioSocketChannel>
{
  private Proxy proxy;
  
  public ProxyOioChannelFactory(Proxy proxy)
  {
    this.proxy = proxy;
  }
  
  public OioSocketChannel newChannel()
  {
    return new OioSocketChannel(new Socket(this.proxy));
  }
}
