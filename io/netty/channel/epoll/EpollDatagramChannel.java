package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoop;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;

public final class EpollDatagramChannel
  extends AbstractEpollChannel
  implements DatagramChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(true);
  private static final String EXPECTED_TYPES = " (expected: " + StringUtil.simpleClassName(DatagramPacket.class) + ", " + StringUtil.simpleClassName(AddressedEnvelope.class) + '<' + StringUtil.simpleClassName(ByteBuf.class) + ", " + StringUtil.simpleClassName(InetSocketAddress.class) + ">, " + StringUtil.simpleClassName(ByteBuf.class) + ')';
  private volatile InetSocketAddress local;
  private volatile InetSocketAddress remote;
  private volatile boolean connected;
  private final EpollDatagramChannelConfig config;
  
  public EpollDatagramChannel()
  {
    super(Native.socketDgramFd(), Native.EPOLLIN);
    this.config = new EpollDatagramChannelConfig(this);
  }
  
  public EpollDatagramChannel(FileDescriptor fd)
  {
    super(null, fd, Native.EPOLLIN, true);
    this.config = new EpollDatagramChannelConfig(this);
    
    this.local = Native.localAddress(fd.intValue());
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public boolean isActive()
  {
    return (fd().isOpen()) && (((((Boolean)this.config.getOption(ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION)).booleanValue()) && (isRegistered())) || (this.active));
  }
  
  public boolean isConnected()
  {
    return this.connected;
  }
  
  public ChannelFuture joinGroup(InetAddress multicastAddress)
  {
    return joinGroup(multicastAddress, newPromise());
  }
  
  public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelPromise promise)
  {
    try
    {
      return joinGroup(multicastAddress, NetworkInterface.getByInetAddress(localAddress().getAddress()), null, promise);
    }
    catch (SocketException e)
    {
      promise.setFailure(e);
    }
    return promise;
  }
  
  public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface)
  {
    return joinGroup(multicastAddress, networkInterface, newPromise());
  }
  
  public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise promise)
  {
    return joinGroup(multicastAddress.getAddress(), networkInterface, null, promise);
  }
  
  public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source)
  {
    return joinGroup(multicastAddress, networkInterface, source, newPromise());
  }
  
  public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise promise)
  {
    if (multicastAddress == null) {
      throw new NullPointerException("multicastAddress");
    }
    if (networkInterface == null) {
      throw new NullPointerException("networkInterface");
    }
    promise.setFailure(new UnsupportedOperationException("Multicast not supported"));
    return promise;
  }
  
  public ChannelFuture leaveGroup(InetAddress multicastAddress)
  {
    return leaveGroup(multicastAddress, newPromise());
  }
  
  public ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelPromise promise)
  {
    try
    {
      return leaveGroup(multicastAddress, NetworkInterface.getByInetAddress(localAddress().getAddress()), null, promise);
    }
    catch (SocketException e)
    {
      promise.setFailure(e);
    }
    return promise;
  }
  
  public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface)
  {
    return leaveGroup(multicastAddress, networkInterface, newPromise());
  }
  
  public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise promise)
  {
    return leaveGroup(multicastAddress.getAddress(), networkInterface, null, promise);
  }
  
  public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source)
  {
    return leaveGroup(multicastAddress, networkInterface, source, newPromise());
  }
  
  public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise promise)
  {
    if (multicastAddress == null) {
      throw new NullPointerException("multicastAddress");
    }
    if (networkInterface == null) {
      throw new NullPointerException("networkInterface");
    }
    promise.setFailure(new UnsupportedOperationException("Multicast not supported"));
    
    return promise;
  }
  
  public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock)
  {
    return block(multicastAddress, networkInterface, sourceToBlock, newPromise());
  }
  
  public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock, ChannelPromise promise)
  {
    if (multicastAddress == null) {
      throw new NullPointerException("multicastAddress");
    }
    if (sourceToBlock == null) {
      throw new NullPointerException("sourceToBlock");
    }
    if (networkInterface == null) {
      throw new NullPointerException("networkInterface");
    }
    promise.setFailure(new UnsupportedOperationException("Multicast not supported"));
    return promise;
  }
  
  public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock)
  {
    return block(multicastAddress, sourceToBlock, newPromise());
  }
  
  public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock, ChannelPromise promise)
  {
    try
    {
      return block(multicastAddress, NetworkInterface.getByInetAddress(localAddress().getAddress()), sourceToBlock, promise);
    }
    catch (Throwable e)
    {
      promise.setFailure(e);
    }
    return promise;
  }
  
  protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe()
  {
    return new EpollDatagramChannelUnsafe();
  }
  
  protected InetSocketAddress localAddress0()
  {
    return this.local;
  }
  
  protected InetSocketAddress remoteAddress0()
  {
    return this.remote;
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {
    InetSocketAddress addr = (InetSocketAddress)localAddress;
    checkResolvable(addr);
    int fd = fd().intValue();
    Native.bind(fd, addr);
    this.local = Native.localAddress(fd);
    this.active = true;
  }
  
  protected void doWrite(ChannelOutboundBuffer in)
    throws Exception
  {
    for (;;)
    {
      Object msg = in.current();
      if (msg == null)
      {
        clearFlag(Native.EPOLLOUT);
        break;
      }
      try
      {
        if ((Native.IS_SUPPORTING_SENDMMSG) && (in.size() > 1))
        {
          NativeDatagramPacketArray array = NativeDatagramPacketArray.getInstance(in);
          int cnt = array.count();
          if (cnt >= 1)
          {
            int offset = 0;
            NativeDatagramPacketArray.NativeDatagramPacket[] packets = array.packets();
            while (cnt > 0)
            {
              int send = Native.sendmmsg(fd().intValue(), packets, offset, cnt);
              if (send == 0)
              {
                setFlag(Native.EPOLLOUT);
                return;
              }
              for (int i = 0; i < send; i++) {
                in.remove();
              }
              cnt -= send;
              offset += send;
            }
            continue;
          }
        }
        boolean done = false;
        for (int i = config().getWriteSpinCount() - 1; i >= 0; i--) {
          if (doWriteMessage(msg))
          {
            done = true;
            break;
          }
        }
        if (done)
        {
          in.remove();
        }
        else
        {
          setFlag(Native.EPOLLOUT);
          break;
        }
      }
      catch (IOException e)
      {
        in.remove(e);
      }
    }
  }
  
  private boolean doWriteMessage(Object msg)
    throws Exception
  {
    InetSocketAddress remoteAddress;
    ByteBuf data;
    InetSocketAddress remoteAddress;
    if ((msg instanceof AddressedEnvelope))
    {
      AddressedEnvelope<ByteBuf, InetSocketAddress> envelope = (AddressedEnvelope)msg;
      
      ByteBuf data = (ByteBuf)envelope.content();
      remoteAddress = (InetSocketAddress)envelope.recipient();
    }
    else
    {
      data = (ByteBuf)msg;
      remoteAddress = null;
    }
    int dataLen = data.readableBytes();
    if (dataLen == 0) {
      return true;
    }
    if (remoteAddress == null)
    {
      remoteAddress = this.remote;
      if (remoteAddress == null) {
        throw new NotYetConnectedException();
      }
    }
    int writtenBytes;
    int writtenBytes;
    if (data.hasMemoryAddress())
    {
      long memoryAddress = data.memoryAddress();
      writtenBytes = Native.sendToAddress(fd().intValue(), memoryAddress, data.readerIndex(), data.writerIndex(), remoteAddress.getAddress(), remoteAddress.getPort());
    }
    else
    {
      int writtenBytes;
      if ((data instanceof CompositeByteBuf))
      {
        IovArray array = IovArrayThreadLocal.get((CompositeByteBuf)data);
        int cnt = array.count();
        assert (cnt != 0);
        
        writtenBytes = Native.sendToAddresses(fd().intValue(), array.memoryAddress(0), cnt, remoteAddress.getAddress(), remoteAddress.getPort());
      }
      else
      {
        ByteBuffer nioData = data.internalNioBuffer(data.readerIndex(), data.readableBytes());
        writtenBytes = Native.sendTo(fd().intValue(), nioData, nioData.position(), nioData.limit(), remoteAddress.getAddress(), remoteAddress.getPort());
      }
    }
    return writtenBytes > 0;
  }
  
  protected Object filterOutboundMessage(Object msg)
  {
    if ((msg instanceof DatagramPacket))
    {
      DatagramPacket packet = (DatagramPacket)msg;
      ByteBuf content = (ByteBuf)packet.content();
      if (content.hasMemoryAddress()) {
        return msg;
      }
      if ((content.isDirect()) && ((content instanceof CompositeByteBuf)))
      {
        CompositeByteBuf comp = (CompositeByteBuf)content;
        if ((comp.isDirect()) && (comp.nioBufferCount() <= Native.IOV_MAX)) {
          return msg;
        }
      }
      return new DatagramPacket(newDirectBuffer(packet, content), (InetSocketAddress)packet.recipient());
    }
    if ((msg instanceof ByteBuf))
    {
      ByteBuf buf = (ByteBuf)msg;
      if ((!buf.hasMemoryAddress()) && ((PlatformDependent.hasUnsafe()) || (!buf.isDirect()))) {
        if ((buf instanceof CompositeByteBuf))
        {
          CompositeByteBuf comp = (CompositeByteBuf)buf;
          if ((!comp.isDirect()) || (comp.nioBufferCount() > Native.IOV_MAX))
          {
            buf = newDirectBuffer(buf);
            assert (buf.hasMemoryAddress());
          }
        }
        else
        {
          buf = newDirectBuffer(buf);
          assert (buf.hasMemoryAddress());
        }
      }
      return buf;
    }
    if ((msg instanceof AddressedEnvelope))
    {
      AddressedEnvelope<Object, SocketAddress> e = (AddressedEnvelope)msg;
      if (((e.content() instanceof ByteBuf)) && ((e.recipient() == null) || ((e.recipient() instanceof InetSocketAddress))))
      {
        ByteBuf content = (ByteBuf)e.content();
        if (content.hasMemoryAddress()) {
          return e;
        }
        if ((content instanceof CompositeByteBuf))
        {
          CompositeByteBuf comp = (CompositeByteBuf)content;
          if ((comp.isDirect()) && (comp.nioBufferCount() <= Native.IOV_MAX)) {
            return e;
          }
        }
        return new DefaultAddressedEnvelope(newDirectBuffer(e, content), (InetSocketAddress)e.recipient());
      }
    }
    throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
  }
  
  public EpollDatagramChannelConfig config()
  {
    return this.config;
  }
  
  protected void doDisconnect()
    throws Exception
  {
    this.connected = false;
  }
  
  final class EpollDatagramChannelUnsafe
    extends AbstractEpollChannel.AbstractEpollUnsafe
  {
    EpollDatagramChannelUnsafe()
    {
      super();
    }
    
    private final List<Object> readBuf = new ArrayList();
    
    public void connect(SocketAddress remote, SocketAddress local, ChannelPromise channelPromise)
    {
      boolean success = false;
      try
      {
        try
        {
          boolean wasActive = EpollDatagramChannel.this.isActive();
          InetSocketAddress remoteAddress = (InetSocketAddress)remote;
          if (local != null)
          {
            InetSocketAddress localAddress = (InetSocketAddress)local;
            EpollDatagramChannel.this.doBind(localAddress);
          }
          AbstractEpollChannel.checkResolvable(remoteAddress);
          EpollDatagramChannel.this.remote = remoteAddress;
          EpollDatagramChannel.this.local = Native.localAddress(EpollDatagramChannel.this.fd().intValue());
          success = true;
          if ((!wasActive) && (EpollDatagramChannel.this.isActive())) {
            EpollDatagramChannel.this.pipeline().fireChannelActive();
          }
        }
        finally
        {
          if (!success)
          {
            EpollDatagramChannel.this.doClose();
          }
          else
          {
            channelPromise.setSuccess();
            EpollDatagramChannel.this.connected = true;
          }
        }
      }
      catch (Throwable cause)
      {
        channelPromise.setFailure(cause);
      }
    }
    
    void epollInReady()
    {
      assert (EpollDatagramChannel.this.eventLoop().inEventLoop());
      DatagramChannelConfig config = EpollDatagramChannel.this.config();
      boolean edgeTriggered = EpollDatagramChannel.this.isFlagSet(Native.EPOLLET);
      if ((!this.readPending) && (!edgeTriggered) && (!config.isAutoRead()))
      {
        clearEpollIn0();
        return;
      }
      RecvByteBufAllocator.Handle allocHandle = EpollDatagramChannel.this.unsafe().recvBufAllocHandle();
      
      ChannelPipeline pipeline = EpollDatagramChannel.this.pipeline();
      Throwable exception = null;
      try
      {
        int maxMessagesPerRead = edgeTriggered ? Integer.MAX_VALUE : config.getMaxMessagesPerRead();
        
        int messages = 0;
        label118:
        ByteBuf data = null;
        try
        {
          data = allocHandle.allocate(config.getAllocator());
          int writerIndex = data.writerIndex();
          EpollDatagramChannel.DatagramSocketAddress remoteAddress;
          EpollDatagramChannel.DatagramSocketAddress remoteAddress;
          if (data.hasMemoryAddress())
          {
            remoteAddress = Native.recvFromAddress(EpollDatagramChannel.this.fd().intValue(), data.memoryAddress(), writerIndex, data.capacity());
          }
          else
          {
            ByteBuffer nioData = data.internalNioBuffer(writerIndex, data.writableBytes());
            remoteAddress = Native.recvFrom(EpollDatagramChannel.this.fd().intValue(), nioData, nioData.position(), nioData.limit());
          }
          if (remoteAddress == null)
          {
            if (data != null) {
              data.release();
            }
            if ((edgeTriggered) || (config.isAutoRead())) {}
          }
          else
          {
            int readBytes = remoteAddress.receivedAmount;
            data.writerIndex(data.writerIndex() + readBytes);
            allocHandle.record(readBytes);
            this.readPending = false;
            
            this.readBuf.add(new DatagramPacket(data, (InetSocketAddress)localAddress(), remoteAddress));
            data = null;
            if (data != null) {
              data.release();
            }
            if ((edgeTriggered) || (config.isAutoRead())) {}
          }
        }
        catch (Throwable t)
        {
          exception = t;
          if (data != null) {
            data.release();
          }
          if ((edgeTriggered) || (config.isAutoRead())) {}
        }
        finally
        {
          if (data != null) {
            data.release();
          }
          if ((edgeTriggered) || (config.isAutoRead()))
          {
            throw ((Throwable)localObject1);
            
            messages++;
            if (messages < maxMessagesPerRead) {
              break label118;
            }
          }
        }
        int size = this.readBuf.size();
        for (int i = 0; i < size; i++) {
          pipeline.fireChannelRead(this.readBuf.get(i));
        }
        this.readBuf.clear();
        pipeline.fireChannelReadComplete();
        if (exception != null) {
          pipeline.fireExceptionCaught(exception);
        }
      }
      finally
      {
        if ((!this.readPending) && (!config.isAutoRead())) {
          EpollDatagramChannel.this.clearEpollIn();
        }
      }
    }
  }
  
  static final class DatagramSocketAddress
    extends InetSocketAddress
  {
    private static final long serialVersionUID = 1348596211215015739L;
    final int receivedAmount;
    
    DatagramSocketAddress(String addr, int port, int receivedAmount)
    {
      super(port);
      this.receivedAmount = receivedAmount;
    }
  }
}
