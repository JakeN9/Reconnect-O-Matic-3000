package io.netty.handler.codec.http.multipart;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.internal.StringUtil;
import java.nio.charset.Charset;
import java.util.List;

public class HttpPostRequestDecoder
  implements InterfaceHttpPostRequestDecoder
{
  static final int DEFAULT_DISCARD_THRESHOLD = 10485760;
  private final InterfaceHttpPostRequestDecoder decoder;
  
  public HttpPostRequestDecoder(HttpRequest request)
  {
    this(new DefaultHttpDataFactory(16384L), request, HttpConstants.DEFAULT_CHARSET);
  }
  
  public HttpPostRequestDecoder(HttpDataFactory factory, HttpRequest request)
  {
    this(factory, request, HttpConstants.DEFAULT_CHARSET);
  }
  
  public HttpPostRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset)
  {
    if (factory == null) {
      throw new NullPointerException("factory");
    }
    if (request == null) {
      throw new NullPointerException("request");
    }
    if (charset == null) {
      throw new NullPointerException("charset");
    }
    if (isMultipart(request)) {
      this.decoder = new HttpPostMultipartRequestDecoder(factory, request, charset);
    } else {
      this.decoder = new HttpPostStandardRequestDecoder(factory, request, charset);
    }
  }
  
  protected static enum MultiPartStatus
  {
    NOTSTARTED,  PREAMBLE,  HEADERDELIMITER,  DISPOSITION,  FIELD,  FILEUPLOAD,  MIXEDPREAMBLE,  MIXEDDELIMITER,  MIXEDDISPOSITION,  MIXEDFILEUPLOAD,  MIXEDCLOSEDELIMITER,  CLOSEDELIMITER,  PREEPILOGUE,  EPILOGUE;
    
    private MultiPartStatus() {}
  }
  
  public static boolean isMultipart(HttpRequest request)
  {
    if (request.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
      return getMultipartDataBoundary((String)request.headers().getAndConvert(HttpHeaderNames.CONTENT_TYPE)) != null;
    }
    return false;
  }
  
  protected static String[] getMultipartDataBoundary(String contentType)
  {
    String[] headerContentType = splitHeaderContentType(contentType);
    if (headerContentType[0].toLowerCase().startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString()))
    {
      int crank;
      if (headerContentType[1].toLowerCase().startsWith(HttpHeaderValues.BOUNDARY.toString()))
      {
        int mrank = 1;
        crank = 2;
      }
      else
      {
        int crank;
        if (headerContentType[2].toLowerCase().startsWith(HttpHeaderValues.BOUNDARY.toString()))
        {
          int mrank = 2;
          crank = 1;
        }
        else
        {
          return null;
        }
      }
      int crank;
      int mrank;
      String boundary = StringUtil.substringAfter(headerContentType[mrank], '=');
      if (boundary == null) {
        throw new ErrorDataDecoderException("Needs a boundary value");
      }
      if (boundary.charAt(0) == '"')
      {
        String bound = boundary.trim();
        int index = bound.length() - 1;
        if (bound.charAt(index) == '"') {
          boundary = bound.substring(1, index);
        }
      }
      if (headerContentType[crank].toLowerCase().startsWith(HttpHeaderValues.CHARSET.toString()))
      {
        String charset = StringUtil.substringAfter(headerContentType[crank], '=');
        if (charset != null) {
          return new String[] { "--" + boundary, charset };
        }
      }
      return new String[] { "--" + boundary };
    }
    return null;
  }
  
  public boolean isMultipart()
  {
    return this.decoder.isMultipart();
  }
  
  public void setDiscardThreshold(int discardThreshold)
  {
    this.decoder.setDiscardThreshold(discardThreshold);
  }
  
  public int getDiscardThreshold()
  {
    return this.decoder.getDiscardThreshold();
  }
  
  public List<InterfaceHttpData> getBodyHttpDatas()
  {
    return this.decoder.getBodyHttpDatas();
  }
  
  public List<InterfaceHttpData> getBodyHttpDatas(String name)
  {
    return this.decoder.getBodyHttpDatas(name);
  }
  
  public InterfaceHttpData getBodyHttpData(String name)
  {
    return this.decoder.getBodyHttpData(name);
  }
  
  public InterfaceHttpPostRequestDecoder offer(HttpContent content)
  {
    return this.decoder.offer(content);
  }
  
  public boolean hasNext()
  {
    return this.decoder.hasNext();
  }
  
  public InterfaceHttpData next()
  {
    return this.decoder.next();
  }
  
  public void destroy()
  {
    this.decoder.destroy();
  }
  
  public void cleanFiles()
  {
    this.decoder.cleanFiles();
  }
  
  public void removeHttpDataFromClean(InterfaceHttpData data)
  {
    this.decoder.removeHttpDataFromClean(data);
  }
  
  private static String[] splitHeaderContentType(String sb)
  {
    int aStart = HttpPostBodyUtil.findNonWhitespace(sb, 0);
    int aEnd = sb.indexOf(';');
    if (aEnd == -1) {
      return new String[] { sb, "", "" };
    }
    int bStart = HttpPostBodyUtil.findNonWhitespace(sb, aEnd + 1);
    if (sb.charAt(aEnd - 1) == ' ') {
      aEnd--;
    }
    int bEnd = sb.indexOf(';', bStart);
    if (bEnd == -1)
    {
      bEnd = HttpPostBodyUtil.findEndOfString(sb);
      return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), "" };
    }
    int cStart = HttpPostBodyUtil.findNonWhitespace(sb, bEnd + 1);
    if (sb.charAt(bEnd - 1) == ' ') {
      bEnd--;
    }
    int cEnd = HttpPostBodyUtil.findEndOfString(sb);
    return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), sb.substring(cStart, cEnd) };
  }
  
  public static class NotEnoughDataDecoderException
    extends DecoderException
  {
    private static final long serialVersionUID = -7846841864603865638L;
    
    public NotEnoughDataDecoderException() {}
    
    public NotEnoughDataDecoderException(String msg)
    {
      super();
    }
    
    public NotEnoughDataDecoderException(Throwable cause)
    {
      super();
    }
    
    public NotEnoughDataDecoderException(String msg, Throwable cause)
    {
      super(cause);
    }
  }
  
  public static class EndOfDataDecoderException
    extends DecoderException
  {
    private static final long serialVersionUID = 1336267941020800769L;
  }
  
  public static class ErrorDataDecoderException
    extends DecoderException
  {
    private static final long serialVersionUID = 5020247425493164465L;
    
    public ErrorDataDecoderException() {}
    
    public ErrorDataDecoderException(String msg)
    {
      super();
    }
    
    public ErrorDataDecoderException(Throwable cause)
    {
      super();
    }
    
    public ErrorDataDecoderException(String msg, Throwable cause)
    {
      super(cause);
    }
  }
}
