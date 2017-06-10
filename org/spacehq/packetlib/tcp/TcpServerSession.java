package org.spacehq.packetlib.tcp;

import io.netty.channel.ChannelHandlerContext;
import java.util.Map;
import org.spacehq.packetlib.Server;
import org.spacehq.packetlib.packet.PacketProtocol;

public class TcpServerSession
  extends TcpSession
{
  private Server server;
  
  public TcpServerSession(String host, int port, PacketProtocol protocol, Server server)
  {
    super(host, port, protocol);
    this.server = server;
  }
  
  public Map<String, Object> getFlags()
  {
    Map<String, Object> ret = super.getFlags();
    ret.putAll(this.server.getGlobalFlags());
    return ret;
  }
  
  public void channelActive(ChannelHandlerContext ctx)
    throws Exception
  {
    super.channelActive(ctx);
    
    this.server.addSession(this);
  }
  
  public void channelInactive(ChannelHandlerContext ctx)
    throws Exception
  {
    super.channelInactive(ctx);
    
    this.server.removeSession(this);
  }
}
