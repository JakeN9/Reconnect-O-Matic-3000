package io.netty.handler.codec.http2;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.BinaryHeaders.EntryVisitor;
import io.netty.handler.codec.TextHeaders.EntryVisitor;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.ObjectUtil;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpUtil
{
  private static final Set<CharSequence> HTTP_TO_HTTP2_HEADER_BLACKLIST = new HashSet()
  {
    private static final long serialVersionUID = -5678614530214167043L;
  };
  public static final HttpMethod OUT_OF_MESSAGE_SEQUENCE_METHOD = HttpMethod.OPTIONS;
  public static final String OUT_OF_MESSAGE_SEQUENCE_PATH = "";
  public static final HttpResponseStatus OUT_OF_MESSAGE_SEQUENCE_RETURN_CODE = HttpResponseStatus.OK;
  private static final Pattern AUTHORITY_REPLACEMENT_PATTERN = Pattern.compile("^.*@");
  
  public static enum ExtensionHeaderNames
  {
    STREAM_ID("x-http2-stream-id"),  AUTHORITY("x-http2-authority"),  SCHEME("x-http2-scheme"),  PATH("x-http2-path"),  STREAM_PROMISE_ID("x-http2-stream-promise-id"),  STREAM_DEPENDENCY_ID("x-http2-stream-dependency-id"),  STREAM_WEIGHT("x-http2-stream-weight");
    
    private final AsciiString text;
    
    private ExtensionHeaderNames(String text)
    {
      this.text = new AsciiString(text);
    }
    
    public AsciiString text()
    {
      return this.text;
    }
  }
  
  public static HttpResponseStatus parseStatus(AsciiString status)
    throws Http2Exception
  {
    HttpResponseStatus result;
    try
    {
      result = HttpResponseStatus.parseLine(status);
      if (result == HttpResponseStatus.SWITCHING_PROTOCOLS) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Invalid HTTP/2 status code '%d'", new Object[] { Integer.valueOf(result.code()) });
      }
    }
    catch (Http2Exception e)
    {
      throw e;
    }
    catch (Throwable t)
    {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, t, "Unrecognized HTTP status code '%s' encountered in translation to HTTP/1.x", new Object[] { status });
    }
    return result;
  }
  
  public static FullHttpResponse toHttpResponse(int streamId, Http2Headers http2Headers, boolean validateHttpHeaders)
    throws Http2Exception
  {
    HttpResponseStatus status = parseStatus(http2Headers.status());
    
    FullHttpResponse msg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, validateHttpHeaders);
    addHttp2ToHttpHeaders(streamId, http2Headers, msg, false);
    return msg;
  }
  
  public static FullHttpRequest toHttpRequest(int streamId, Http2Headers http2Headers, boolean validateHttpHeaders)
    throws Http2Exception
  {
    AsciiString method = (AsciiString)ObjectUtil.checkNotNull(http2Headers.method(), "method header cannot be null in conversion to HTTP/1.x");
    
    AsciiString path = (AsciiString)ObjectUtil.checkNotNull(http2Headers.path(), "path header cannot be null in conversion to HTTP/1.x");
    
    FullHttpRequest msg = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(method.toString()), path.toString(), validateHttpHeaders);
    
    addHttp2ToHttpHeaders(streamId, http2Headers, msg, false);
    return msg;
  }
  
  public static void addHttp2ToHttpHeaders(int streamId, Http2Headers sourceHeaders, FullHttpMessage destinationMessage, boolean addToTrailer)
    throws Http2Exception
  {
    HttpHeaders headers = addToTrailer ? destinationMessage.trailingHeaders() : destinationMessage.headers();
    boolean request = destinationMessage instanceof HttpRequest;
    Http2ToHttpHeaderTranslator visitor = new Http2ToHttpHeaderTranslator(streamId, headers, request);
    try
    {
      sourceHeaders.forEachEntry(visitor);
    }
    catch (Http2Exception ex)
    {
      throw ex;
    }
    catch (Throwable t)
    {
      throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, t, "HTTP/2 to HTTP/1.x headers conversion error", new Object[0]);
    }
    headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
    headers.remove(HttpHeaderNames.TRAILER);
    if (!addToTrailer)
    {
      headers.setInt(ExtensionHeaderNames.STREAM_ID.text(), streamId);
      HttpHeaderUtil.setKeepAlive(destinationMessage, true);
    }
  }
  
  public static Http2Headers toHttp2Headers(FullHttpMessage in)
    throws Exception
  {
    Http2Headers out = new DefaultHttp2Headers();
    HttpHeaders inHeaders = in.headers();
    if ((in instanceof HttpRequest))
    {
      HttpRequest request = (HttpRequest)in;
      out.path(new AsciiString(request.uri()));
      out.method(new AsciiString(request.method().toString()));
      
      String value = (String)inHeaders.getAndConvert(HttpHeaderNames.HOST);
      if (value != null)
      {
        URI hostUri = URI.create(value);
        
        value = hostUri.getAuthority();
        if (value != null) {
          out.authority(new AsciiString(AUTHORITY_REPLACEMENT_PATTERN.matcher(value).replaceFirst("")));
        }
        value = hostUri.getScheme();
        if (value != null) {
          out.scheme(new AsciiString(value));
        }
      }
      CharSequence cValue = (CharSequence)inHeaders.get(ExtensionHeaderNames.AUTHORITY.text());
      if (cValue != null) {
        out.authority(AsciiString.of(cValue));
      }
      cValue = (CharSequence)inHeaders.get(ExtensionHeaderNames.SCHEME.text());
      if (cValue != null) {
        out.scheme(AsciiString.of(cValue));
      }
    }
    else if ((in instanceof HttpResponse))
    {
      HttpResponse response = (HttpResponse)in;
      out.status(new AsciiString(Integer.toString(response.status().code())));
    }
    inHeaders.forEachEntry(new TextHeaders.EntryVisitor()
    {
      public boolean visit(Map.Entry<CharSequence, CharSequence> entry)
        throws Exception
      {
        AsciiString aName = AsciiString.of((CharSequence)entry.getKey()).toLowerCase();
        if (!HttpUtil.HTTP_TO_HTTP2_HEADER_BLACKLIST.contains(aName))
        {
          AsciiString aValue = AsciiString.of((CharSequence)entry.getValue());
          if ((!aName.equalsIgnoreCase(HttpHeaderNames.TE)) || (aValue.equalsIgnoreCase(HttpHeaderValues.TRAILERS))) {
            this.val$out.add(aName, aValue);
          }
        }
        return true;
      }
    });
    return out;
  }
  
  private static final class Http2ToHttpHeaderTranslator
    implements BinaryHeaders.EntryVisitor
  {
    private static final Map<AsciiString, AsciiString> REQUEST_HEADER_TRANSLATIONS = new HashMap();
    private static final Map<AsciiString, AsciiString> RESPONSE_HEADER_TRANSLATIONS = new HashMap();
    private final int streamId;
    private final HttpHeaders output;
    private final Map<AsciiString, AsciiString> translations;
    
    static
    {
      RESPONSE_HEADER_TRANSLATIONS.put(Http2Headers.PseudoHeaderName.AUTHORITY.value(), HttpUtil.ExtensionHeaderNames.AUTHORITY.text());
      
      RESPONSE_HEADER_TRANSLATIONS.put(Http2Headers.PseudoHeaderName.SCHEME.value(), HttpUtil.ExtensionHeaderNames.SCHEME.text());
      
      REQUEST_HEADER_TRANSLATIONS.putAll(RESPONSE_HEADER_TRANSLATIONS);
      RESPONSE_HEADER_TRANSLATIONS.put(Http2Headers.PseudoHeaderName.PATH.value(), HttpUtil.ExtensionHeaderNames.PATH.text());
    }
    
    Http2ToHttpHeaderTranslator(int streamId, HttpHeaders output, boolean request)
    {
      this.streamId = streamId;
      this.output = output;
      this.translations = (request ? REQUEST_HEADER_TRANSLATIONS : RESPONSE_HEADER_TRANSLATIONS);
    }
    
    public boolean visit(Map.Entry<AsciiString, AsciiString> entry)
      throws Http2Exception
    {
      AsciiString name = (AsciiString)entry.getKey();
      AsciiString value = (AsciiString)entry.getValue();
      AsciiString translatedName = (AsciiString)this.translations.get(name);
      if ((translatedName != null) || (!Http2Headers.PseudoHeaderName.isPseudoHeader(name)))
      {
        if (translatedName == null) {
          translatedName = name;
        }
        if ((translatedName.isEmpty()) || (translatedName.charAt(0) == ':')) {
          throw Http2Exception.streamError(this.streamId, Http2Error.PROTOCOL_ERROR, "Invalid HTTP/2 header '%s' encountered in translation to HTTP/1.x", new Object[] { translatedName });
        }
        this.output.add(translatedName, value);
      }
      return true;
    }
  }
}
