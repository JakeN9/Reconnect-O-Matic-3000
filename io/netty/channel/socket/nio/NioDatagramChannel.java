package io.netty.channel.socket.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.channel.nio.AbstractNioChannel.NioUnsafe;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.MembershipKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class NioDatagramChannel
  extends AbstractNioMessageChannel
  implements io.netty.channel.socket.DatagramChannel
{
  private static final ChannelMetadata METADATA = new ChannelMetadata(true);
  private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();
  private static final String EXPECTED_TYPES = " (expected: " + StringUtil.simpleClassName(DatagramPacket.class) + ", " + StringUtil.simpleClassName(AddressedEnvelope.class) + '<' + StringUtil.simpleClassName(ByteBuf.class) + ", " + StringUtil.simpleClassName(SocketAddress.class) + ">, " + StringUtil.simpleClassName(ByteBuf.class) + ')';
  private final DatagramChannelConfig config;
  private Map<InetAddress, List<MembershipKey>> memberships;
  
  private static java.nio.channels.DatagramChannel newSocket(SelectorProvider provider)
  {
    try
    {
      return provider.openDatagramChannel();
    }
    catch (IOException e)
    {
      throw new ChannelException("Failed to open a socket.", e);
    }
  }
  
  private static java.nio.channels.DatagramChannel newSocket(SelectorProvider provider, InternetProtocolFamily ipFamily)
  {
    if (ipFamily == null) {
      return newSocket(provider);
    }
    checkJavaVersion();
    try
    {
      return provider.openDatagramChannel(ProtocolFamilyConverter.convert(ipFamily));
    }
    catch (IOException e)
    {
      throw new ChannelException("Failed to open a socket.", e);
    }
  }
  
  private static void checkJavaVersion()
  {
    if (PlatformDependent.javaVersion() < 7) {
      throw new UnsupportedOperationException("Only supported on java 7+.");
    }
  }
  
  public NioDatagramChannel()
  {
    this(newSocket(DEFAULT_SELECTOR_PROVIDER));
  }
  
  public NioDatagramChannel(SelectorProvider provider)
  {
    this(newSocket(provider));
  }
  
  public NioDatagramChannel(InternetProtocolFamily ipFamily)
  {
    this(newSocket(DEFAULT_SELECTOR_PROVIDER, ipFamily));
  }
  
  public NioDatagramChannel(SelectorProvider provider, InternetProtocolFamily ipFamily)
  {
    this(newSocket(provider, ipFamily));
  }
  
  public NioDatagramChannel(java.nio.channels.DatagramChannel socket)
  {
    super(null, socket, 1);
    this.config = new NioDatagramChannelConfig(this, socket);
  }
  
  public ChannelMetadata metadata()
  {
    return METADATA;
  }
  
  public DatagramChannelConfig config()
  {
    return this.config;
  }
  
  public boolean isActive()
  {
    java.nio.channels.DatagramChannel ch = javaChannel();
    return (ch.isOpen()) && (((((Boolean)this.config.getOption(ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION)).booleanValue()) && (isRegistered())) || (ch.socket().isBound()));
  }
  
  public boolean isConnected()
  {
    return javaChannel().isConnected();
  }
  
  protected java.nio.channels.DatagramChannel javaChannel()
  {
    return (java.nio.channels.DatagramChannel)super.javaChannel();
  }
  
  protected SocketAddress localAddress0()
  {
    return javaChannel().socket().getLocalSocketAddress();
  }
  
  protected SocketAddress remoteAddress0()
  {
    return javaChannel().socket().getRemoteSocketAddress();
  }
  
  protected void doBind(SocketAddress localAddress)
    throws Exception
  {
    javaChannel().socket().bind(localAddress);
  }
  
  protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws Exception
  {
    if (localAddress != null) {
      javaChannel().socket().bind(localAddress);
    }
    boolean success = false;
    try
    {
      javaChannel().connect(remoteAddress);
      success = true;
      return true;
    }
    finally
    {
      if (!success) {
        doClose();
      }
    }
  }
  
  protected void doFinishConnect()
    throws Exception
  {
    throw new Error();
  }
  
  protected void doDisconnect()
    throws Exception
  {
    javaChannel().disconnect();
  }
  
  protected void doClose()
    throws Exception
  {
    javaChannel().close();
  }
  
  protected int doReadMessages(List<Object> buf)
    throws Exception
  {
    java.nio.channels.DatagramChannel ch = javaChannel();
    DatagramChannelConfig config = config();
    RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
    
    ByteBuf data = allocHandle.allocate(config.getAllocator());
    boolean free = true;
    try
    {
      ByteBuffer nioData = data.internalNioBuffer(data.writerIndex(), data.writableBytes());
      pos = nioData.position();
      InetSocketAddress remoteAddress = (InetSocketAddress)ch.receive(nioData);
      if (remoteAddress == null) {
        return 0;
      }
      int readBytes = nioData.position() - pos;
      data.writerIndex(data.writerIndex() + readBytes);
      allocHandle.record(readBytes);
      
      buf.add(new DatagramPacket(data, localAddress(), remoteAddress));
      free = false;
      return 1;
    }
    catch (Throwable cause)
    {
      int pos;
      PlatformDependent.throwException(cause);
      return -1;
    }
    finally
    {
      if (free) {
        data.release();
      }
    }
  }
  
  protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in)
    throws Exception
  {
    ByteBuf data;
    ByteBuf data;
    SocketAddress remoteAddress;
    if ((msg instanceof AddressedEnvelope))
    {
      AddressedEnvelope<ByteBuf, SocketAddress> envelope = (AddressedEnvelope)msg;
      SocketAddress remoteAddress = envelope.recipient();
      data = (ByteBuf)envelope.content();
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
    ByteBuffer nioData = data.internalNioBuffer(data.readerIndex(), dataLen);
    int writtenBytes;
    int writtenBytes;
    if (remoteAddress != null) {
      writtenBytes = javaChannel().send(nioData, remoteAddress);
    } else {
      writtenBytes = javaChannel().write(nioData);
    }
    return writtenBytes > 0;
  }
  
  protected Object filterOutboundMessage(Object msg)
  {
    if ((msg instanceof DatagramPacket))
    {
      DatagramPacket p = (DatagramPacket)msg;
      ByteBuf content = (ByteBuf)p.content();
      if (isSingleDirectBuffer(content)) {
        return p;
      }
      return new DatagramPacket(newDirectBuffer(p, content), (InetSocketAddress)p.recipient());
    }
    if ((msg instanceof ByteBuf))
    {
      ByteBuf buf = (ByteBuf)msg;
      if (isSingleDirectBuffer(buf)) {
        return buf;
      }
      return newDirectBuffer(buf);
    }
    if ((msg instanceof AddressedEnvelope))
    {
      AddressedEnvelope<Object, SocketAddress> e = (AddressedEnvelope)msg;
      if ((e.content() instanceof ByteBuf))
      {
        ByteBuf content = (ByteBuf)e.content();
        if (isSingleDirectBuffer(content)) {
          return e;
        }
        return new DefaultAddressedEnvelope(newDirectBuffer(e, content), e.recipient());
      }
    }
    throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
  }
  
  private static boolean isSingleDirectBuffer(ByteBuf buf)
  {
    return (buf.isDirect()) && (buf.nioBufferCount() == 1);
  }
  
  protected boolean continueOnWriteError()
  {
    return true;
  }
  
  public InetSocketAddress localAddress()
  {
    return (InetSocketAddress)super.localAddress();
  }
  
  public InetSocketAddress remoteAddress()
  {
    return (InetSocketAddress)super.remoteAddress();
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
    try
    {
      MembershipKey key;
      MembershipKey key;
      if (source == null) {
        key = javaChannel().join(multicastAddress, networkInterface);
      } else {
        key = javaChannel().join(multicastAddress, networkInterface, source);
      }
      synchronized (this)
      {
        List<MembershipKey> keys = null;
        if (this.memberships == null) {
          this.memberships = new HashMap();
        } else {
          keys = (List)this.memberships.get(multicastAddress);
        }
        if (keys == null)
        {
          keys = new ArrayList();
          this.memberships.put(multicastAddress, keys);
        }
        keys.add(key);
      }
      promise.setSuccess();
    }
    catch (Throwable e)
    {
      promise.setFailure(e);
    }
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
    synchronized (this)
    {
      if (this.memberships != null)
      {
        List<MembershipKey> keys = (List)this.memberships.get(multicastAddress);
        if (keys != null)
        {
          Iterator<MembershipKey> keyIt = keys.iterator();
          while (keyIt.hasNext())
          {
            MembershipKey key = (MembershipKey)keyIt.next();
            if ((networkInterface.equals(key.networkInterface())) && (
              ((source == null) && (key.sourceAddress() == null)) || ((source != null) && (source.equals(key.sourceAddress())))))
            {
              key.drop();
              keyIt.remove();
            }
          }
          if (keys.isEmpty()) {
            this.memberships.remove(multicastAddress);
          }
        }
      }
    }
    promise.setSuccess();
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
    synchronized (this)
    {
      if (this.memberships != null)
      {
        List<MembershipKey> keys = (List)this.memberships.get(multicastAddress);
        for (MembershipKey key : keys) {
          if (networkInterface.equals(key.networkInterface())) {
            try
            {
              key.block(sourceToBlock);
            }
            catch (IOException e)
            {
              promise.setFailure(e);
            }
          }
        }
      }
    }
    promise.setSuccess();
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
    catch (SocketException e)
    {
      promise.setFailure(e);
    }
    return promise;
  }
  
  protected void setReadPending(boolean readPending)
  {
    super.setReadPending(readPending);
  }
}
