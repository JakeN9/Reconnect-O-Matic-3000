package io.netty.channel;

import java.util.ArrayList;
import java.util.List;

public class ChannelHandlerAppender
  extends ChannelHandlerAdapter
{
  private final boolean selfRemoval;
  
  private static final class Entry
  {
    final String name;
    final ChannelHandler handler;
    
    Entry(String name, ChannelHandler handler)
    {
      this.name = name;
      this.handler = handler;
    }
  }
  
  private final List<Entry> handlers = new ArrayList();
  private boolean added;
  
  protected ChannelHandlerAppender()
  {
    this(true);
  }
  
  protected ChannelHandlerAppender(boolean selfRemoval)
  {
    this.selfRemoval = selfRemoval;
  }
  
  public ChannelHandlerAppender(Iterable<? extends ChannelHandler> handlers)
  {
    this(true, handlers);
  }
  
  public ChannelHandlerAppender(ChannelHandler... handlers)
  {
    this(true, handlers);
  }
  
  public ChannelHandlerAppender(boolean selfRemoval, Iterable<? extends ChannelHandler> handlers)
  {
    this.selfRemoval = selfRemoval;
    add(handlers);
  }
  
  public ChannelHandlerAppender(boolean selfRemoval, ChannelHandler... handlers)
  {
    this.selfRemoval = selfRemoval;
    add(handlers);
  }
  
  protected final ChannelHandlerAppender add(String name, ChannelHandler handler)
  {
    if (handler == null) {
      throw new NullPointerException("handler");
    }
    if (this.added) {
      throw new IllegalStateException("added to the pipeline already");
    }
    this.handlers.add(new Entry(name, handler));
    return this;
  }
  
  protected final ChannelHandlerAppender add(ChannelHandler handler)
  {
    return add(null, handler);
  }
  
  protected final ChannelHandlerAppender add(Iterable<? extends ChannelHandler> handlers)
  {
    if (handlers == null) {
      throw new NullPointerException("handlers");
    }
    for (ChannelHandler h : handlers)
    {
      if (h == null) {
        break;
      }
      add(h);
    }
    return this;
  }
  
  protected final ChannelHandlerAppender add(ChannelHandler... handlers)
  {
    if (handlers == null) {
      throw new NullPointerException("handlers");
    }
    for (ChannelHandler h : handlers)
    {
      if (h == null) {
        break;
      }
      add(h);
    }
    return this;
  }
  
  protected final <T extends ChannelHandler> T handlerAt(int index)
  {
    return ((Entry)this.handlers.get(index)).handler;
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {
    this.added = true;
    
    AbstractChannelHandlerContext dctx = (AbstractChannelHandlerContext)ctx;
    DefaultChannelPipeline pipeline = (DefaultChannelPipeline)dctx.pipeline();
    String name = dctx.name();
    try
    {
      for (Entry e : this.handlers)
      {
        String oldName = name;
        if (e.name == null) {
          name = pipeline.generateName(e.handler);
        } else {
          name = e.name;
        }
        pipeline.addAfter(dctx.invoker, oldName, name, e.handler);
      }
    }
    finally
    {
      if (this.selfRemoval) {
        pipeline.remove(this);
      }
    }
  }
}
