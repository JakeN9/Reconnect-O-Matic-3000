package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Http2ServerUpgradeCodec
  implements HttpServerUpgradeHandler.UpgradeCodec
{
  private static final List<String> REQUIRED_UPGRADE_HEADERS = Collections.singletonList("HTTP2-Settings");
  private final String handlerName;
  private final Http2ConnectionHandler connectionHandler;
  private final Http2FrameReader frameReader;
  
  public Http2ServerUpgradeCodec(Http2ConnectionHandler connectionHandler)
  {
    this("http2ConnectionHandler", connectionHandler);
  }
  
  public Http2ServerUpgradeCodec(String handlerName, Http2ConnectionHandler connectionHandler)
  {
    this.handlerName = ((String)ObjectUtil.checkNotNull(handlerName, "handlerName"));
    this.connectionHandler = ((Http2ConnectionHandler)ObjectUtil.checkNotNull(connectionHandler, "connectionHandler"));
    this.frameReader = new DefaultHttp2FrameReader();
  }
  
  public String protocol()
  {
    return "h2c-16";
  }
  
  public Collection<String> requiredUpgradeHeaders()
  {
    return REQUIRED_UPGRADE_HEADERS;
  }
  
  public void prepareUpgradeResponse(ChannelHandlerContext ctx, FullHttpRequest upgradeRequest, FullHttpResponse upgradeResponse)
  {
    try
    {
      List<CharSequence> upgradeHeaders = upgradeRequest.headers().getAll("HTTP2-Settings");
      if ((upgradeHeaders.isEmpty()) || (upgradeHeaders.size() > 1)) {
        throw new IllegalArgumentException("There must be 1 and only 1 HTTP2-Settings header.");
      }
      Http2Settings settings = decodeSettingsHeader(ctx, (CharSequence)upgradeHeaders.get(0));
      this.connectionHandler.onHttpServerUpgrade(settings);
    }
    catch (Throwable e)
    {
      upgradeResponse.setStatus(HttpResponseStatus.BAD_REQUEST);
      upgradeResponse.headers().clear();
    }
  }
  
  public void upgradeTo(ChannelHandlerContext ctx, FullHttpRequest upgradeRequest, FullHttpResponse upgradeResponse)
  {
    ctx.pipeline().addAfter(ctx.name(), this.handlerName, this.connectionHandler);
  }
  
  private Http2Settings decodeSettingsHeader(ChannelHandlerContext ctx, CharSequence settingsHeader)
    throws Http2Exception
  {
    ByteBuf header = Unpooled.wrappedBuffer(AsciiString.getBytes(settingsHeader, CharsetUtil.UTF_8));
    try
    {
      ByteBuf payload = Base64.decode(header, Base64Dialect.URL_SAFE);
      
      ByteBuf frame = createSettingsFrame(ctx, payload);
      
      return decodeSettings(ctx, frame);
    }
    finally
    {
      header.release();
    }
  }
  
  private Http2Settings decodeSettings(ChannelHandlerContext ctx, ByteBuf frame)
    throws Http2Exception
  {
    try
    {
      final Http2Settings decodedSettings = new Http2Settings();
      this.frameReader.readFrame(ctx, frame, new Http2FrameAdapter()
      {
        public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings)
        {
          decodedSettings.copyFrom(settings);
        }
      });
      return decodedSettings;
    }
    finally
    {
      frame.release();
    }
  }
  
  private static ByteBuf createSettingsFrame(ChannelHandlerContext ctx, ByteBuf payload)
  {
    ByteBuf frame = ctx.alloc().buffer(9 + payload.readableBytes());
    Http2CodecUtil.writeFrameHeader(frame, payload.readableBytes(), (byte)4, new Http2Flags(), 0);
    frame.writeBytes(payload);
    payload.release();
    return frame;
  }
}
