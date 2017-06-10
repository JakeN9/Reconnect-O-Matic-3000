package io.netty.channel;

final class DefaultChannelHandlerContext
  extends AbstractChannelHandlerContext
{
  private final ChannelHandler handler;
  
  DefaultChannelHandlerContext(DefaultChannelPipeline pipeline, ChannelHandlerInvoker invoker, String name, ChannelHandler handler)
  {
    super(pipeline, invoker, name, skipFlags(checkNull(handler)));
    this.handler = handler;
  }
  
  private static ChannelHandler checkNull(ChannelHandler handler)
  {
    if (handler == null) {
      throw new NullPointerException("handler");
    }
    return handler;
  }
  
  public ChannelHandler handler()
  {
    return this.handler;
  }
}
