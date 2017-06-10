package io.netty.channel;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

@ChannelHandler.Sharable
public abstract class ChannelInitializer<C extends Channel>
  extends ChannelHandlerAdapter
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChannelInitializer.class);
  
  protected abstract void initChannel(C paramC)
    throws Exception;
  
  public final void channelRegistered(ChannelHandlerContext ctx)
    throws Exception
  {
    ChannelPipeline pipeline = ctx.pipeline();
    boolean success = false;
    try
    {
      initChannel(ctx.channel());
      pipeline.remove(this);
      ctx.fireChannelRegistered();
      success = true;
    }
    catch (Throwable t)
    {
      logger.warn("Failed to initialize a channel. Closing: " + ctx.channel(), t);
    }
    finally
    {
      if (pipeline.context(this) != null) {
        pipeline.remove(this);
      }
      if (!success) {
        ctx.close();
      }
    }
  }
}
