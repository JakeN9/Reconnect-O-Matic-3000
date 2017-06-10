package io.netty.handler.codec.http.websocketx.extensions.compression;

import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandshaker;

public class WebSocketClientCompressionHandler
  extends WebSocketClientExtensionHandler
{
  public WebSocketClientCompressionHandler()
  {
    super(new WebSocketClientExtensionHandshaker[] { new PerMessageDeflateClientExtensionHandshaker(), new DeflateFrameClientExtensionHandshaker(false), new DeflateFrameClientExtensionHandshaker(true) });
  }
}
