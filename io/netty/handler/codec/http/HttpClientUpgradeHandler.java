package io.netty.handler.codec.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.AsciiString;
import io.netty.util.ReferenceCountUtil;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HttpClientUpgradeHandler
  extends HttpObjectAggregator
{
  private final SourceCodec sourceCodec;
  private final UpgradeCodec upgradeCodec;
  private boolean upgradeRequested;
  
  public static abstract interface UpgradeCodec
  {
    public abstract String protocol();
    
    public abstract Collection<String> setUpgradeHeaders(ChannelHandlerContext paramChannelHandlerContext, HttpRequest paramHttpRequest);
    
    public abstract void upgradeTo(ChannelHandlerContext paramChannelHandlerContext, FullHttpResponse paramFullHttpResponse)
      throws Exception;
  }
  
  public static abstract interface SourceCodec
  {
    public abstract void upgradeFrom(ChannelHandlerContext paramChannelHandlerContext);
  }
  
  public static enum UpgradeEvent
  {
    UPGRADE_ISSUED,  UPGRADE_SUCCESSFUL,  UPGRADE_REJECTED;
    
    private UpgradeEvent() {}
  }
  
  public HttpClientUpgradeHandler(SourceCodec sourceCodec, UpgradeCodec upgradeCodec, int maxContentLength)
  {
    super(maxContentLength);
    if (sourceCodec == null) {
      throw new NullPointerException("sourceCodec");
    }
    if (upgradeCodec == null) {
      throw new NullPointerException("upgradeCodec");
    }
    this.sourceCodec = sourceCodec;
    this.upgradeCodec = upgradeCodec;
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    if (!(msg instanceof HttpRequest))
    {
      super.write(ctx, msg, promise);
      return;
    }
    if (this.upgradeRequested)
    {
      promise.setFailure(new IllegalStateException("Attempting to write HTTP request with upgrade in progress"));
      
      return;
    }
    this.upgradeRequested = true;
    setUpgradeRequestHeaders(ctx, (HttpRequest)msg);
    
    super.write(ctx, msg, promise);
    
    ctx.fireUserEventTriggered(UpgradeEvent.UPGRADE_ISSUED);
  }
  
  protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
    throws Exception
  {
    FullHttpResponse response = null;
    try
    {
      if (!this.upgradeRequested) {
        throw new IllegalStateException("Read HTTP response without requesting protocol switch");
      }
      if ((msg instanceof FullHttpResponse))
      {
        response = (FullHttpResponse)msg;
        
        response.retain();
        out.add(response);
      }
      else
      {
        super.decode(ctx, msg, out);
        if (out.isEmpty()) {
          return;
        }
        assert (out.size() == 1);
        response = (FullHttpResponse)out.get(0);
      }
      if (!HttpResponseStatus.SWITCHING_PROTOCOLS.equals(response.status()))
      {
        ctx.fireUserEventTriggered(UpgradeEvent.UPGRADE_REJECTED);
        removeThisHandler(ctx);
        return;
      }
      CharSequence upgradeHeader = (CharSequence)response.headers().get(HttpHeaderNames.UPGRADE);
      if (upgradeHeader == null) {
        throw new IllegalStateException("Switching Protocols response missing UPGRADE header");
      }
      if (!AsciiString.equalsIgnoreCase(this.upgradeCodec.protocol(), upgradeHeader)) {
        throw new IllegalStateException("Switching Protocols response with unexpected UPGRADE protocol: " + upgradeHeader);
      }
      this.sourceCodec.upgradeFrom(ctx);
      this.upgradeCodec.upgradeTo(ctx, response);
      
      ctx.fireUserEventTriggered(UpgradeEvent.UPGRADE_SUCCESSFUL);
      
      response.release();
      out.clear();
      removeThisHandler(ctx);
    }
    catch (Throwable t)
    {
      ReferenceCountUtil.release(response);
      ctx.fireExceptionCaught(t);
      removeThisHandler(ctx);
    }
  }
  
  private static void removeThisHandler(ChannelHandlerContext ctx)
  {
    ctx.pipeline().remove(ctx.name());
  }
  
  private void setUpgradeRequestHeaders(ChannelHandlerContext ctx, HttpRequest request)
  {
    request.headers().set(HttpHeaderNames.UPGRADE, this.upgradeCodec.protocol());
    
    Set<String> connectionParts = new LinkedHashSet(2);
    connectionParts.addAll(this.upgradeCodec.setUpgradeHeaders(ctx, request));
    
    StringBuilder builder = new StringBuilder();
    for (String part : connectionParts)
    {
      builder.append(part);
      builder.append(',');
    }
    builder.append(HttpHeaderNames.UPGRADE);
    request.headers().set(HttpHeaderNames.CONNECTION, builder.toString());
  }
}
