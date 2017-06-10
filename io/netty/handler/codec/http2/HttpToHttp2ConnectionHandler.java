package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpHeaders;

public class HttpToHttp2ConnectionHandler
  extends Http2ConnectionHandler
{
  public HttpToHttp2ConnectionHandler(boolean server, Http2FrameListener listener)
  {
    super(server, listener);
  }
  
  public HttpToHttp2ConnectionHandler(Http2Connection connection, Http2FrameListener listener)
  {
    super(connection, listener);
  }
  
  public HttpToHttp2ConnectionHandler(Http2Connection connection, Http2FrameReader frameReader, Http2FrameWriter frameWriter, Http2FrameListener listener)
  {
    super(connection, frameReader, frameWriter, listener);
  }
  
  public HttpToHttp2ConnectionHandler(Http2ConnectionDecoder.Builder decoderBuilder, Http2ConnectionEncoder.Builder encoderBuilder)
  {
    super(decoderBuilder, encoderBuilder);
  }
  
  private int getStreamId(HttpHeaders httpHeaders)
    throws Exception
  {
    return httpHeaders.getInt(HttpUtil.ExtensionHeaderNames.STREAM_ID.text(), connection().local().nextStreamId());
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
  {
    if ((msg instanceof FullHttpMessage))
    {
      FullHttpMessage httpMsg = (FullHttpMessage)msg;
      boolean hasData = httpMsg.content().isReadable();
      boolean httpMsgNeedRelease = true;
      Http2CodecUtil.SimpleChannelPromiseAggregator promiseAggregator = null;
      try
      {
        int streamId = getStreamId(httpMsg.headers());
        
        Http2Headers http2Headers = HttpUtil.toHttp2Headers(httpMsg);
        Http2ConnectionEncoder encoder = encoder();
        if (hasData)
        {
          promiseAggregator = new Http2CodecUtil.SimpleChannelPromiseAggregator(promise, ctx.channel(), ctx.executor());
          encoder.writeHeaders(ctx, streamId, http2Headers, 0, false, promiseAggregator.newPromise());
          httpMsgNeedRelease = false;
          encoder.writeData(ctx, streamId, httpMsg.content(), 0, true, promiseAggregator.newPromise());
          promiseAggregator.doneAllocatingPromises();
        }
        else
        {
          encoder.writeHeaders(ctx, streamId, http2Headers, 0, true, promise);
        }
      }
      catch (Throwable t)
      {
        if (promiseAggregator == null) {
          promise.tryFailure(t);
        } else {
          promiseAggregator.setFailure(t);
        }
      }
      finally
      {
        if (httpMsgNeedRelease) {
          httpMsg.release();
        }
      }
    }
    else
    {
      ctx.write(msg, promise);
    }
  }
}
