package io.netty.handler.codec.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class HttpServerUpgradeHandler
  extends HttpObjectAggregator
{
  private final Map<String, UpgradeCodec> upgradeCodecMap;
  private final SourceCodec sourceCodec;
  private boolean handlingUpgrade;
  
  public static final class UpgradeEvent
    implements ReferenceCounted
  {
    private final String protocol;
    private final FullHttpRequest upgradeRequest;
    
    private UpgradeEvent(String protocol, FullHttpRequest upgradeRequest)
    {
      this.protocol = protocol;
      this.upgradeRequest = upgradeRequest;
    }
    
    public String protocol()
    {
      return this.protocol;
    }
    
    public FullHttpRequest upgradeRequest()
    {
      return this.upgradeRequest;
    }
    
    public int refCnt()
    {
      return this.upgradeRequest.refCnt();
    }
    
    public UpgradeEvent retain()
    {
      this.upgradeRequest.retain();
      return this;
    }
    
    public UpgradeEvent retain(int increment)
    {
      this.upgradeRequest.retain(increment);
      return this;
    }
    
    public UpgradeEvent touch()
    {
      this.upgradeRequest.touch();
      return this;
    }
    
    public UpgradeEvent touch(Object hint)
    {
      this.upgradeRequest.touch(hint);
      return this;
    }
    
    public boolean release()
    {
      return this.upgradeRequest.release();
    }
    
    public boolean release(int decrement)
    {
      return this.upgradeRequest.release();
    }
    
    public String toString()
    {
      return "UpgradeEvent [protocol=" + this.protocol + ", upgradeRequest=" + this.upgradeRequest + ']';
    }
  }
  
  public HttpServerUpgradeHandler(SourceCodec sourceCodec, Collection<UpgradeCodec> upgradeCodecs, int maxContentLength)
  {
    super(maxContentLength);
    if (sourceCodec == null) {
      throw new NullPointerException("sourceCodec");
    }
    if (upgradeCodecs == null) {
      throw new NullPointerException("upgradeCodecs");
    }
    this.sourceCodec = sourceCodec;
    this.upgradeCodecMap = new LinkedHashMap(upgradeCodecs.size());
    for (UpgradeCodec upgradeCodec : upgradeCodecs)
    {
      String name = upgradeCodec.protocol().toUpperCase(Locale.US);
      this.upgradeCodecMap.put(name, upgradeCodec);
    }
  }
  
  protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
    throws Exception
  {
    this.handlingUpgrade |= isUpgradeRequest(msg);
    if (!this.handlingUpgrade)
    {
      ReferenceCountUtil.retain(msg);
      out.add(msg); return;
    }
    FullHttpRequest fullRequest;
    if ((msg instanceof FullHttpRequest))
    {
      FullHttpRequest fullRequest = (FullHttpRequest)msg;
      ReferenceCountUtil.retain(msg);
      out.add(msg);
    }
    else
    {
      super.decode(ctx, msg, out);
      if (out.isEmpty()) {
        return;
      }
      assert (out.size() == 1);
      this.handlingUpgrade = false;
      fullRequest = (FullHttpRequest)out.get(0);
    }
    if (upgrade(ctx, fullRequest)) {
      out.clear();
    }
  }
  
  private static boolean isUpgradeRequest(HttpObject msg)
  {
    return ((msg instanceof HttpRequest)) && (((HttpRequest)msg).headers().get(HttpHeaderNames.UPGRADE) != null);
  }
  
  private boolean upgrade(final ChannelHandlerContext ctx, final FullHttpRequest request)
  {
    CharSequence upgradeHeader = (CharSequence)request.headers().get(HttpHeaderNames.UPGRADE);
    final UpgradeCodec upgradeCodec = selectUpgradeCodec(upgradeHeader);
    if (upgradeCodec == null) {
      return false;
    }
    CharSequence connectionHeader = (CharSequence)request.headers().get(HttpHeaderNames.CONNECTION);
    if (connectionHeader == null) {
      return false;
    }
    Collection<String> requiredHeaders = upgradeCodec.requiredUpgradeHeaders();
    Set<CharSequence> values = splitHeader(connectionHeader);
    if ((!values.contains(HttpHeaderNames.UPGRADE)) || (!values.containsAll(requiredHeaders))) {
      return false;
    }
    for (String requiredHeader : requiredHeaders) {
      if (!request.headers().contains(requiredHeader)) {
        return false;
      }
    }
    final UpgradeEvent event = new UpgradeEvent(upgradeCodec.protocol(), request, null);
    
    final FullHttpResponse upgradeResponse = createUpgradeResponse(upgradeCodec);
    upgradeCodec.prepareUpgradeResponse(ctx, request, upgradeResponse);
    ctx.writeAndFlush(upgradeResponse).addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future)
        throws Exception
      {
        try
        {
          if (future.isSuccess())
          {
            HttpServerUpgradeHandler.this.sourceCodec.upgradeFrom(ctx);
            upgradeCodec.upgradeTo(ctx, request, upgradeResponse);
            
            ctx.fireUserEventTriggered(event.retain());
            
            ctx.pipeline().remove(HttpServerUpgradeHandler.this);
          }
          else
          {
            future.channel().close();
          }
        }
        finally
        {
          event.release();
        }
      }
    });
    return true;
  }
  
  private UpgradeCodec selectUpgradeCodec(CharSequence upgradeHeader)
  {
    Set<CharSequence> requestedProtocols = splitHeader(upgradeHeader);
    
    Set<String> supportedProtocols = new LinkedHashSet(this.upgradeCodecMap.keySet());
    supportedProtocols.retainAll(requestedProtocols);
    if (!supportedProtocols.isEmpty())
    {
      String protocol = ((String)supportedProtocols.iterator().next()).toUpperCase(Locale.US);
      return (UpgradeCodec)this.upgradeCodecMap.get(protocol);
    }
    return null;
  }
  
  private static FullHttpResponse createUpgradeResponse(UpgradeCodec upgradeCodec)
  {
    DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS);
    res.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
    res.headers().add(HttpHeaderNames.UPGRADE, upgradeCodec.protocol());
    res.headers().add(HttpHeaderNames.CONTENT_LENGTH, "0");
    return res;
  }
  
  private static Set<CharSequence> splitHeader(CharSequence header)
  {
    StringBuilder builder = new StringBuilder(header.length());
    Set<CharSequence> protocols = new TreeSet(AsciiString.CHARSEQUENCE_CASE_INSENSITIVE_ORDER);
    for (int i = 0; i < header.length(); i++)
    {
      char c = header.charAt(i);
      if (!Character.isWhitespace(c)) {
        if (c == ',')
        {
          protocols.add(builder.toString());
          builder.setLength(0);
        }
        else
        {
          builder.append(c);
        }
      }
    }
    if (builder.length() > 0) {
      protocols.add(builder.toString());
    }
    return protocols;
  }
  
  public static abstract interface UpgradeCodec
  {
    public abstract String protocol();
    
    public abstract Collection<String> requiredUpgradeHeaders();
    
    public abstract void prepareUpgradeResponse(ChannelHandlerContext paramChannelHandlerContext, FullHttpRequest paramFullHttpRequest, FullHttpResponse paramFullHttpResponse);
    
    public abstract void upgradeTo(ChannelHandlerContext paramChannelHandlerContext, FullHttpRequest paramFullHttpRequest, FullHttpResponse paramFullHttpResponse);
  }
  
  public static abstract interface SourceCodec
  {
    public abstract void upgradeFrom(ChannelHandlerContext paramChannelHandlerContext);
  }
}
