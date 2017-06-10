package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class BinaryWebSocketFrame
  extends WebSocketFrame
{
  public BinaryWebSocketFrame()
  {
    super(Unpooled.buffer(0));
  }
  
  public BinaryWebSocketFrame(ByteBuf binaryData)
  {
    super(binaryData);
  }
  
  public BinaryWebSocketFrame(boolean finalFragment, int rsv, ByteBuf binaryData)
  {
    super(finalFragment, rsv, binaryData);
  }
  
  public BinaryWebSocketFrame copy()
  {
    return new BinaryWebSocketFrame(isFinalFragment(), rsv(), content().copy());
  }
  
  public BinaryWebSocketFrame duplicate()
  {
    return new BinaryWebSocketFrame(isFinalFragment(), rsv(), content().duplicate());
  }
  
  public BinaryWebSocketFrame retain()
  {
    super.retain();
    return this;
  }
  
  public BinaryWebSocketFrame retain(int increment)
  {
    super.retain(increment);
    return this;
  }
  
  public BinaryWebSocketFrame touch()
  {
    super.touch();
    return this;
  }
  
  public BinaryWebSocketFrame touch(Object hint)
  {
    super.touch(hint);
    return this;
  }
}
