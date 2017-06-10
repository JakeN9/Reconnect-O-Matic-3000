package io.netty.handler.codec.spdy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SpdyHttpDecoder
  extends MessageToMessageDecoder<SpdyFrame>
{
  private final boolean validateHeaders;
  private final int spdyVersion;
  private final int maxContentLength;
  private final Map<Integer, FullHttpMessage> messageMap;
  
  public SpdyHttpDecoder(SpdyVersion version, int maxContentLength)
  {
    this(version, maxContentLength, new HashMap(), true);
  }
  
  public SpdyHttpDecoder(SpdyVersion version, int maxContentLength, boolean validateHeaders)
  {
    this(version, maxContentLength, new HashMap(), validateHeaders);
  }
  
  protected SpdyHttpDecoder(SpdyVersion version, int maxContentLength, Map<Integer, FullHttpMessage> messageMap)
  {
    this(version, maxContentLength, messageMap, true);
  }
  
  protected SpdyHttpDecoder(SpdyVersion version, int maxContentLength, Map<Integer, FullHttpMessage> messageMap, boolean validateHeaders)
  {
    if (version == null) {
      throw new NullPointerException("version");
    }
    if (maxContentLength <= 0) {
      throw new IllegalArgumentException("maxContentLength must be a positive integer: " + maxContentLength);
    }
    this.spdyVersion = version.getVersion();
    this.maxContentLength = maxContentLength;
    this.messageMap = messageMap;
    this.validateHeaders = validateHeaders;
  }
  
  protected FullHttpMessage putMessage(int streamId, FullHttpMessage message)
  {
    return (FullHttpMessage)this.messageMap.put(Integer.valueOf(streamId), message);
  }
  
  protected FullHttpMessage getMessage(int streamId)
  {
    return (FullHttpMessage)this.messageMap.get(Integer.valueOf(streamId));
  }
  
  protected FullHttpMessage removeMessage(int streamId)
  {
    return (FullHttpMessage)this.messageMap.remove(Integer.valueOf(streamId));
  }
  
  protected void decode(ChannelHandlerContext ctx, SpdyFrame msg, List<Object> out)
    throws Exception
  {
    if ((msg instanceof SpdySynStreamFrame))
    {
      SpdySynStreamFrame spdySynStreamFrame = (SpdySynStreamFrame)msg;
      int streamId = spdySynStreamFrame.streamId();
      if (SpdyCodecUtil.isServerId(streamId))
      {
        int associatedToStreamId = spdySynStreamFrame.associatedStreamId();
        if (associatedToStreamId == 0)
        {
          SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, SpdyStreamStatus.INVALID_STREAM);
          
          ctx.writeAndFlush(spdyRstStreamFrame);
          return;
        }
        if (spdySynStreamFrame.isLast())
        {
          SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, SpdyStreamStatus.PROTOCOL_ERROR);
          
          ctx.writeAndFlush(spdyRstStreamFrame);
          return;
        }
        if (spdySynStreamFrame.isTruncated())
        {
          SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, SpdyStreamStatus.INTERNAL_ERROR);
          
          ctx.writeAndFlush(spdyRstStreamFrame);
          return;
        }
        try
        {
          FullHttpRequest httpRequestWithEntity = createHttpRequest(this.spdyVersion, spdySynStreamFrame);
          
          httpRequestWithEntity.headers().setInt(SpdyHttpHeaders.Names.STREAM_ID, streamId);
          httpRequestWithEntity.headers().setInt(SpdyHttpHeaders.Names.ASSOCIATED_TO_STREAM_ID, associatedToStreamId);
          httpRequestWithEntity.headers().setByte(SpdyHttpHeaders.Names.PRIORITY, spdySynStreamFrame.priority());
          
          out.add(httpRequestWithEntity);
        }
        catch (Exception ignored)
        {
          SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, SpdyStreamStatus.PROTOCOL_ERROR);
          
          ctx.writeAndFlush(spdyRstStreamFrame);
        }
      }
      else
      {
        if (spdySynStreamFrame.isTruncated())
        {
          SpdySynReplyFrame spdySynReplyFrame = new DefaultSpdySynReplyFrame(streamId);
          spdySynReplyFrame.setLast(true);
          SpdyHeaders frameHeaders = spdySynReplyFrame.headers();
          frameHeaders.setInt(SpdyHeaders.HttpNames.STATUS, HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.code());
          frameHeaders.setObject(SpdyHeaders.HttpNames.VERSION, HttpVersion.HTTP_1_0);
          ctx.writeAndFlush(spdySynReplyFrame);
          return;
        }
        try
        {
          FullHttpRequest httpRequestWithEntity = createHttpRequest(this.spdyVersion, spdySynStreamFrame);
          
          httpRequestWithEntity.headers().setInt(SpdyHttpHeaders.Names.STREAM_ID, streamId);
          if (spdySynStreamFrame.isLast()) {
            out.add(httpRequestWithEntity);
          } else {
            putMessage(streamId, httpRequestWithEntity);
          }
        }
        catch (Exception e)
        {
          SpdySynReplyFrame spdySynReplyFrame = new DefaultSpdySynReplyFrame(streamId);
          spdySynReplyFrame.setLast(true);
          SpdyHeaders frameHeaders = spdySynReplyFrame.headers();
          frameHeaders.setInt(SpdyHeaders.HttpNames.STATUS, HttpResponseStatus.BAD_REQUEST.code());
          frameHeaders.setObject(SpdyHeaders.HttpNames.VERSION, HttpVersion.HTTP_1_0);
          ctx.writeAndFlush(spdySynReplyFrame);
        }
      }
    }
    else if ((msg instanceof SpdySynReplyFrame))
    {
      SpdySynReplyFrame spdySynReplyFrame = (SpdySynReplyFrame)msg;
      int streamId = spdySynReplyFrame.streamId();
      if (spdySynReplyFrame.isTruncated())
      {
        SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, SpdyStreamStatus.INTERNAL_ERROR);
        
        ctx.writeAndFlush(spdyRstStreamFrame);
        return;
      }
      try
      {
        FullHttpResponse httpResponseWithEntity = createHttpResponse(ctx, spdySynReplyFrame, this.validateHeaders);
        
        httpResponseWithEntity.headers().setInt(SpdyHttpHeaders.Names.STREAM_ID, streamId);
        if (spdySynReplyFrame.isLast())
        {
          HttpHeaderUtil.setContentLength(httpResponseWithEntity, 0L);
          out.add(httpResponseWithEntity);
        }
        else
        {
          putMessage(streamId, httpResponseWithEntity);
        }
      }
      catch (Exception e)
      {
        SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, SpdyStreamStatus.PROTOCOL_ERROR);
        
        ctx.writeAndFlush(spdyRstStreamFrame);
      }
    }
    else if ((msg instanceof SpdyHeadersFrame))
    {
      SpdyHeadersFrame spdyHeadersFrame = (SpdyHeadersFrame)msg;
      int streamId = spdyHeadersFrame.streamId();
      FullHttpMessage fullHttpMessage = getMessage(streamId);
      if (fullHttpMessage == null)
      {
        if (SpdyCodecUtil.isServerId(streamId))
        {
          if (spdyHeadersFrame.isTruncated())
          {
            SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, SpdyStreamStatus.INTERNAL_ERROR);
            
            ctx.writeAndFlush(spdyRstStreamFrame);
            return;
          }
          try
          {
            fullHttpMessage = createHttpResponse(ctx, spdyHeadersFrame, this.validateHeaders);
            
            fullHttpMessage.headers().setInt(SpdyHttpHeaders.Names.STREAM_ID, streamId);
            if (spdyHeadersFrame.isLast())
            {
              HttpHeaderUtil.setContentLength(fullHttpMessage, 0L);
              out.add(fullHttpMessage);
            }
            else
            {
              putMessage(streamId, fullHttpMessage);
            }
          }
          catch (Exception e)
          {
            SpdyRstStreamFrame spdyRstStreamFrame = new DefaultSpdyRstStreamFrame(streamId, SpdyStreamStatus.PROTOCOL_ERROR);
            
            ctx.writeAndFlush(spdyRstStreamFrame);
          }
        }
        return;
      }
      if (!spdyHeadersFrame.isTruncated()) {
        for (Map.Entry<CharSequence, CharSequence> e : spdyHeadersFrame.headers()) {
          fullHttpMessage.headers().add((CharSequence)e.getKey(), (CharSequence)e.getValue());
        }
      }
      if (spdyHeadersFrame.isLast())
      {
        HttpHeaderUtil.setContentLength(fullHttpMessage, fullHttpMessage.content().readableBytes());
        removeMessage(streamId);
        out.add(fullHttpMessage);
      }
    }
    else if ((msg instanceof SpdyDataFrame))
    {
      SpdyDataFrame spdyDataFrame = (SpdyDataFrame)msg;
      int streamId = spdyDataFrame.streamId();
      FullHttpMessage fullHttpMessage = getMessage(streamId);
      if (fullHttpMessage == null) {
        return;
      }
      ByteBuf content = fullHttpMessage.content();
      if (content.readableBytes() > this.maxContentLength - spdyDataFrame.content().readableBytes())
      {
        removeMessage(streamId);
        throw new TooLongFrameException("HTTP content length exceeded " + this.maxContentLength + " bytes.");
      }
      ByteBuf spdyDataFrameData = spdyDataFrame.content();
      int spdyDataFrameDataLen = spdyDataFrameData.readableBytes();
      content.writeBytes(spdyDataFrameData, spdyDataFrameData.readerIndex(), spdyDataFrameDataLen);
      if (spdyDataFrame.isLast())
      {
        HttpHeaderUtil.setContentLength(fullHttpMessage, content.readableBytes());
        removeMessage(streamId);
        out.add(fullHttpMessage);
      }
    }
    else if ((msg instanceof SpdyRstStreamFrame))
    {
      SpdyRstStreamFrame spdyRstStreamFrame = (SpdyRstStreamFrame)msg;
      int streamId = spdyRstStreamFrame.streamId();
      removeMessage(streamId);
    }
  }
  
  private static FullHttpRequest createHttpRequest(int spdyVersion, SpdyHeadersFrame requestFrame)
    throws Exception
  {
    SpdyHeaders headers = requestFrame.headers();
    HttpMethod method = HttpMethod.valueOf((String)headers.getAndConvert(SpdyHeaders.HttpNames.METHOD));
    String url = (String)headers.getAndConvert(SpdyHeaders.HttpNames.PATH);
    HttpVersion httpVersion = HttpVersion.valueOf((String)headers.getAndConvert(SpdyHeaders.HttpNames.VERSION));
    headers.remove(SpdyHeaders.HttpNames.METHOD);
    headers.remove(SpdyHeaders.HttpNames.PATH);
    headers.remove(SpdyHeaders.HttpNames.VERSION);
    
    FullHttpRequest req = new DefaultFullHttpRequest(httpVersion, method, url);
    
    headers.remove(SpdyHeaders.HttpNames.SCHEME);
    
    CharSequence host = (CharSequence)headers.get(SpdyHeaders.HttpNames.HOST);
    headers.remove(SpdyHeaders.HttpNames.HOST);
    req.headers().set(HttpHeaderNames.HOST, host);
    for (Map.Entry<CharSequence, CharSequence> e : requestFrame.headers()) {
      req.headers().add((CharSequence)e.getKey(), (CharSequence)e.getValue());
    }
    HttpHeaderUtil.setKeepAlive(req, true);
    
    req.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
    
    return req;
  }
  
  private static FullHttpResponse createHttpResponse(ChannelHandlerContext ctx, SpdyHeadersFrame responseFrame, boolean validateHeaders)
    throws Exception
  {
    SpdyHeaders headers = responseFrame.headers();
    HttpResponseStatus status = HttpResponseStatus.parseLine((CharSequence)headers.get(SpdyHeaders.HttpNames.STATUS));
    HttpVersion version = HttpVersion.valueOf((String)headers.getAndConvert(SpdyHeaders.HttpNames.VERSION));
    headers.remove(SpdyHeaders.HttpNames.STATUS);
    headers.remove(SpdyHeaders.HttpNames.VERSION);
    
    FullHttpResponse res = new DefaultFullHttpResponse(version, status, ctx.alloc().buffer(), validateHeaders);
    for (Map.Entry<CharSequence, CharSequence> e : responseFrame.headers()) {
      res.headers().add((CharSequence)e.getKey(), (CharSequence)e.getValue());
    }
    HttpHeaderUtil.setKeepAlive(res, true);
    
    res.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
    res.headers().remove(HttpHeaderNames.TRAILER);
    
    return res;
  }
}
