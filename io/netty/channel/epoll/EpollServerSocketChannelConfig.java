package io.netty.channel.epoll;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.channel.unix.FileDescriptor;
import java.util.Map;

public final class EpollServerSocketChannelConfig
  extends EpollServerChannelConfig
  implements ServerSocketChannelConfig
{
  EpollServerSocketChannelConfig(EpollServerSocketChannel channel)
  {
    super(channel);
    
    setReuseAddress(true);
  }
  
  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(super.getOptions(), new ChannelOption[] { EpollChannelOption.SO_REUSEPORT });
  }
  
  public <T> T getOption(ChannelOption<T> option)
  {
    if (option == EpollChannelOption.SO_REUSEPORT) {
      return Boolean.valueOf(isReusePort());
    }
    return (T)super.getOption(option);
  }
  
  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    if (option == EpollChannelOption.SO_REUSEPORT) {
      setReusePort(((Boolean)value).booleanValue());
    } else {
      return super.setOption(option, value);
    }
    return true;
  }
  
  public EpollServerSocketChannelConfig setReuseAddress(boolean reuseAddress)
  {
    super.setReuseAddress(reuseAddress);
    return this;
  }
  
  public EpollServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize)
  {
    super.setReceiveBufferSize(receiveBufferSize);
    return this;
  }
  
  public EpollServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth)
  {
    return this;
  }
  
  public EpollServerSocketChannelConfig setBacklog(int backlog)
  {
    super.setBacklog(backlog);
    return this;
  }
  
  public EpollServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  public EpollServerSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public EpollServerSocketChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public EpollServerSocketChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public EpollServerSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public EpollServerSocketChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  public EpollServerSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  public EpollServerSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  public EpollServerSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
  
  public boolean isReusePort()
  {
    return Native.isReusePort(this.channel.fd().intValue()) == 1;
  }
  
  public EpollServerSocketChannelConfig setReusePort(boolean reusePort)
  {
    Native.setReusePort(this.channel.fd().intValue(), reusePort ? 1 : 0);
    return this;
  }
}
