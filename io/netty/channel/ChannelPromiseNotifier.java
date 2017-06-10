package io.netty.channel;

import io.netty.util.concurrent.PromiseNotifier;

public final class ChannelPromiseNotifier
  extends PromiseNotifier<Void, ChannelFuture>
  implements ChannelFutureListener
{
  public ChannelPromiseNotifier(ChannelPromise... promises)
  {
    super(promises);
  }
}
