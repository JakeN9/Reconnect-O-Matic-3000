package org.spacehq.packetlib.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import java.net.Proxy;
import java.util.Hashtable;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.packet.PacketProtocol;

public class TcpClientSession
  extends TcpSession
{
  private Client client;
  private Proxy proxy;
  private EventLoopGroup group;
  
  public TcpClientSession(String host, int port, PacketProtocol protocol, Client client, Proxy proxy)
  {
    super(host, port, protocol);
    this.client = client;
    this.proxy = proxy;
  }
  
  public void connect(boolean wait)
  {
    if (this.disconnected) {
      throw new IllegalStateException("Session has already been disconnected.");
    }
    if (this.group != null) {
      return;
    }
    try
    {
      final Bootstrap bootstrap = new Bootstrap();
      if (this.proxy != null)
      {
        this.group = new OioEventLoopGroup();
        bootstrap.channelFactory(new ProxyOioChannelFactory(this.proxy));
      }
      else
      {
        this.group = new NioEventLoopGroup();
        bootstrap.channel(NioSocketChannel.class);
      }
      ((Bootstrap)((Bootstrap)bootstrap.handler(new ChannelInitializer()
      {
        public void initChannel(Channel channel)
          throws Exception
        {
          TcpClientSession.this.getPacketProtocol().newClientSession(TcpClientSession.this.client, TcpClientSession.this);
          
          channel.config().setOption(ChannelOption.IP_TOS, Integer.valueOf(24));
          channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.valueOf(false));
          
          ChannelPipeline pipeline = channel.pipeline();
          
          TcpClientSession.this.refreshReadTimeoutHandler(channel);
          TcpClientSession.this.refreshWriteTimeoutHandler(channel);
          
          pipeline.addLast("encryption", new TcpPacketEncryptor(TcpClientSession.this));
          pipeline.addLast("sizer", new TcpPacketSizer(TcpClientSession.this));
          pipeline.addLast("codec", new TcpPacketCodec(TcpClientSession.this));
          pipeline.addLast("manager", TcpClientSession.this);
        }
      }
      
        )).group(this.group)).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.valueOf(getConnectTimeout() * 1000));
      
      Runnable connectTask = new Runnable()
      {
        public void run()
        {
          try
          {
            String host = TcpClientSession.this.getHost();
            int port = TcpClientSession.this.getPort();
            try
            {
              Hashtable<String, String> environment = new Hashtable();
              environment.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
              environment.put("java.naming.provider.url", "dns:");
              
              String[] result = new InitialDirContext(environment).getAttributes(TcpClientSession.this.getPacketProtocol().getSRVRecordPrefix() + "._tcp." + host, new String[] { "SRV" }).get("srv").get().toString().split(" ", 4);
              host = result[3];
              port = Integer.parseInt(result[2]);
            }
            catch (Throwable localThrowable1) {}
            bootstrap.remoteAddress(host, port);
            
            ChannelFuture future = bootstrap.connect().sync();
            if (future.isSuccess()) {
              while ((!TcpClientSession.this.isConnected()) && (!TcpClientSession.this.disconnected)) {
                try
                {
                  Thread.sleep(5L);
                }
                catch (InterruptedException localInterruptedException) {}
              }
            }
          }
          catch (Throwable t)
          {
            TcpClientSession.this.exceptionCaught(null, t);
          }
        }
      };
      if (wait) {
        connectTask.run();
      } else {
        new Thread(connectTask).start();
      }
    }
    catch (Throwable t)
    {
      exceptionCaught(null, t);
    }
  }
  
  public void disconnect(String reason, Throwable cause, boolean wait)
  {
    super.disconnect(reason, cause, wait);
    if (this.group != null)
    {
      Future<?> future = this.group.shutdownGracefully();
      if (wait) {
        try
        {
          future.await();
        }
        catch (InterruptedException localInterruptedException) {}
      }
      this.group = null;
    }
  }
}
