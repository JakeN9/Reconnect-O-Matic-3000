package io.netty.handler.codec.http.cors;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.Set;

public class CorsHandler
  extends ChannelHandlerAdapter
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(CorsHandler.class);
  private static final String ANY_ORIGIN = "*";
  private final CorsConfig config;
  private HttpRequest request;
  
  public CorsHandler(CorsConfig config)
  {
    this.config = config;
  }
  
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    if ((this.config.isCorsSupportEnabled()) && ((msg instanceof HttpRequest)))
    {
      this.request = ((HttpRequest)msg);
      if (isPreflightRequest(this.request))
      {
        handlePreflight(ctx, this.request);
        return;
      }
      if ((this.config.isShortCurcuit()) && (!validateOrigin()))
      {
        forbidden(ctx, this.request);
        return;
      }
    }
    ctx.fireChannelRead(msg);
  }
  
  private void handlePreflight(ChannelHandlerContext ctx, HttpRequest request)
  {
    HttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, true, true);
    if (setOrigin(response))
    {
      setAllowMethods(response);
      setAllowHeaders(response);
      setAllowCredentials(response);
      setMaxAge(response);
      setPreflightHeaders(response);
    }
    ReferenceCountUtil.release(request);
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }
  
  private void setPreflightHeaders(HttpResponse response)
  {
    response.headers().add(this.config.preflightResponseHeaders());
  }
  
  private boolean setOrigin(HttpResponse response)
  {
    CharSequence origin = (CharSequence)this.request.headers().get(HttpHeaderNames.ORIGIN);
    if (origin != null)
    {
      if (("null".equals(origin)) && (this.config.isNullOriginAllowed()))
      {
        setAnyOrigin(response);
        return true;
      }
      if (this.config.isAnyOriginSupported())
      {
        if (this.config.isCredentialsAllowed())
        {
          echoRequestOrigin(response);
          setVaryHeader(response);
        }
        else
        {
          setAnyOrigin(response);
        }
        return true;
      }
      if (this.config.origins().contains(origin))
      {
        setOrigin(response, origin);
        setVaryHeader(response);
        return true;
      }
      logger.debug("Request origin [" + origin + "] was not among the configured origins " + this.config.origins());
    }
    return false;
  }
  
  private boolean validateOrigin()
  {
    if (this.config.isAnyOriginSupported()) {
      return true;
    }
    CharSequence origin = (CharSequence)this.request.headers().get(HttpHeaderNames.ORIGIN);
    if (origin == null) {
      return true;
    }
    if (("null".equals(origin)) && (this.config.isNullOriginAllowed())) {
      return true;
    }
    return this.config.origins().contains(origin);
  }
  
  private void echoRequestOrigin(HttpResponse response)
  {
    setOrigin(response, (CharSequence)this.request.headers().get(HttpHeaderNames.ORIGIN));
  }
  
  private static void setVaryHeader(HttpResponse response)
  {
    response.headers().set(HttpHeaderNames.VARY, HttpHeaderNames.ORIGIN);
  }
  
  private static void setAnyOrigin(HttpResponse response)
  {
    setOrigin(response, "*");
  }
  
  private static void setOrigin(HttpResponse response, CharSequence origin)
  {
    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
  }
  
  private void setAllowCredentials(HttpResponse response)
  {
    if ((this.config.isCredentialsAllowed()) && (!((CharSequence)response.headers().get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).equals("*"))) {
      response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }
  }
  
  private static boolean isPreflightRequest(HttpRequest request)
  {
    HttpHeaders headers = request.headers();
    return (request.method().equals(HttpMethod.OPTIONS)) && (headers.contains(HttpHeaderNames.ORIGIN)) && (headers.contains(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD));
  }
  
  private void setExposeHeaders(HttpResponse response)
  {
    if (!this.config.exposedHeaders().isEmpty()) {
      response.headers().set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, this.config.exposedHeaders());
    }
  }
  
  private void setAllowMethods(HttpResponse response)
  {
    response.headers().setObject(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, this.config.allowedRequestMethods());
  }
  
  private void setAllowHeaders(HttpResponse response)
  {
    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, this.config.allowedRequestHeaders());
  }
  
  private void setMaxAge(HttpResponse response)
  {
    response.headers().setLong(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, this.config.maxAge());
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    throws Exception
  {
    if ((this.config.isCorsSupportEnabled()) && ((msg instanceof HttpResponse)))
    {
      HttpResponse response = (HttpResponse)msg;
      if (setOrigin(response))
      {
        setAllowCredentials(response);
        setAllowHeaders(response);
        setExposeHeaders(response);
      }
    }
    ctx.writeAndFlush(msg, promise);
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    logger.error("Caught error in CorsHandler", cause);
    ctx.fireExceptionCaught(cause);
  }
  
  private static void forbidden(ChannelHandlerContext ctx, HttpRequest request)
  {
    ctx.writeAndFlush(new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.FORBIDDEN)).addListener(ChannelFutureListener.CLOSE);
    
    ReferenceCountUtil.release(request);
  }
}
