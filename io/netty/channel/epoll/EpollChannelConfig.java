package io.netty.channel.epoll;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import java.util.Map;

public class EpollChannelConfig
  extends DefaultChannelConfig
{
  final AbstractEpollChannel channel;
  
  EpollChannelConfig(AbstractEpollChannel channel)
  {
    super(channel);
    this.channel = channel;
  }
  
  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(super.getOptions(), new ChannelOption[] { EpollChannelOption.EPOLL_MODE });
  }
  
  public <T> T getOption(ChannelOption<T> option)
  {
    if (option == EpollChannelOption.EPOLL_MODE) {
      return getEpollMode();
    }
    return (T)super.getOption(option);
  }
  
  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    if (option == EpollChannelOption.EPOLL_MODE) {
      setEpollMode((EpollMode)value);
    } else {
      return super.setOption(option, value);
    }
    return true;
  }
  
  public EpollChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  public EpollChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public EpollChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public EpollChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public EpollChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public EpollChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  public EpollChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  public EpollChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  public EpollChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
  
  public EpollMode getEpollMode()
  {
    return this.channel.isFlagSet(Native.EPOLLET) ? EpollMode.EDGE_TRIGGERED : EpollMode.LEVEL_TRIGGERED;
  }
  
  public EpollChannelConfig setEpollMode(EpollMode mode)
  {
    if (mode == null) {
      throw new NullPointerException("mode");
    }
    switch (mode)
    {
    case EDGE_TRIGGERED: 
      checkChannelNotRegistered();
      this.channel.setFlag(Native.EPOLLET);
      break;
    case LEVEL_TRIGGERED: 
      checkChannelNotRegistered();
      this.channel.clearFlag(Native.EPOLLET);
      break;
    default: 
      throw new Error();
    }
    return this;
  }
  
  private void checkChannelNotRegistered()
  {
    if (this.channel.isRegistered()) {
      throw new IllegalStateException("EpollMode can only be changed before channel is registered");
    }
  }
  
  protected final void autoReadCleared()
  {
    this.channel.clearEpollIn();
  }
}
