package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;

public class Utf8FrameValidator
  extends ChannelHandlerAdapter
{
  private int fragmentedFramesCount;
  private Utf8Validator utf8Validator;
  
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    if ((msg instanceof WebSocketFrame))
    {
      WebSocketFrame frame = (WebSocketFrame)msg;
      if (((WebSocketFrame)msg).isFinalFragment())
      {
        if (!(frame instanceof PingWebSocketFrame))
        {
          this.fragmentedFramesCount = 0;
          if (((frame instanceof TextWebSocketFrame)) || ((this.utf8Validator != null) && (this.utf8Validator.isChecking())))
          {
            checkUTF8String(ctx, frame.content());
            
            this.utf8Validator.finish();
          }
        }
      }
      else
      {
        if (this.fragmentedFramesCount == 0)
        {
          if ((frame instanceof TextWebSocketFrame)) {
            checkUTF8String(ctx, frame.content());
          }
        }
        else if ((this.utf8Validator != null) && (this.utf8Validator.isChecking())) {
          checkUTF8String(ctx, frame.content());
        }
        this.fragmentedFramesCount += 1;
      }
    }
    super.channelRead(ctx, msg);
  }
  
  private void checkUTF8String(ChannelHandlerContext ctx, ByteBuf buffer)
  {
    try
    {
      if (this.utf8Validator == null) {
        this.utf8Validator = new Utf8Validator();
      }
      this.utf8Validator.check(buffer);
    }
    catch (CorruptedFrameException ex)
    {
      if (ctx.channel().isActive()) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
      }
    }
  }
}
