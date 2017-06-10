package io.netty.channel.oio;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelPipeline;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOioMessageChannel
  extends AbstractOioChannel
{
  private final List<Object> readBuf = new ArrayList();
  
  protected AbstractOioMessageChannel(Channel parent)
  {
    super(parent);
  }
  
  protected void doRead()
  {
    ChannelConfig config = config();
    ChannelPipeline pipeline = pipeline();
    boolean closed = false;
    int maxMessagesPerRead = config.getMaxMessagesPerRead();
    
    Throwable exception = null;
    int localRead = 0;
    int totalRead = 0;
    try
    {
      for (;;)
      {
        localRead = doReadMessages(this.readBuf);
        if (localRead == 0) {
          break;
        }
        if (localRead < 0)
        {
          closed = true;
        }
        else
        {
          int size = this.readBuf.size();
          for (int i = 0; i < size; i++) {
            pipeline.fireChannelRead(this.readBuf.get(i));
          }
          this.readBuf.clear();
          
          totalRead += localRead;
          if ((totalRead >= maxMessagesPerRead) || (!config.isAutoRead())) {
            break;
          }
        }
      }
    }
    catch (Throwable t)
    {
      exception = t;
    }
    pipeline.fireChannelReadComplete();
    if (exception != null)
    {
      if ((exception instanceof IOException)) {
        closed = true;
      }
      pipeline().fireExceptionCaught(exception);
    }
    if (closed)
    {
      if (isOpen()) {
        unsafe().close(unsafe().voidPromise());
      }
    }
    else if ((localRead == 0) && (isActive())) {
      read();
    }
  }
  
  protected abstract int doReadMessages(List<Object> paramList)
    throws Exception;
}
