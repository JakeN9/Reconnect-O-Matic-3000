package io.netty.handler.codec.http;

import io.netty.handler.codec.AsciiString;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpVersion
  implements Comparable<HttpVersion>
{
  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\S+)/(\\d+)\\.(\\d+)");
  private static final String HTTP_1_0_STRING = "HTTP/1.0";
  private static final String HTTP_1_1_STRING = "HTTP/1.1";
  public static final HttpVersion HTTP_1_0 = new HttpVersion("HTTP", 1, 0, false);
  public static final HttpVersion HTTP_1_1 = new HttpVersion("HTTP", 1, 1, true);
  private final AsciiString protocolName;
  private final int majorVersion;
  private final int minorVersion;
  private final AsciiString text;
  private final String textAsString;
  private final boolean keepAliveDefault;
  
  public static HttpVersion valueOf(String text)
  {
    if (text == null) {
      throw new NullPointerException("text");
    }
    text = text.trim();
    if (text.isEmpty()) {
      throw new IllegalArgumentException("text is empty");
    }
    HttpVersion version = version0(text);
    if (version == null) {
      version = new HttpVersion(text, true);
    }
    return version;
  }
  
  private static HttpVersion version0(String text)
  {
    if ("HTTP/1.1".equals(text)) {
      return HTTP_1_1;
    }
    if ("HTTP/1.0".equals(text)) {
      return HTTP_1_0;
    }
    return null;
  }
  
  public HttpVersion(String text, boolean keepAliveDefault)
  {
    if (text == null) {
      throw new NullPointerException("text");
    }
    text = text.trim().toUpperCase();
    if (text.isEmpty()) {
      throw new IllegalArgumentException("empty text");
    }
    Matcher m = VERSION_PATTERN.matcher(text);
    if (!m.matches()) {
      throw new IllegalArgumentException("invalid version format: " + text);
    }
    this.protocolName = new AsciiString(m.group(1));
    this.majorVersion = Integer.parseInt(m.group(2));
    this.minorVersion = Integer.parseInt(m.group(3));
    this.textAsString = (this.protocolName + "/" + this.majorVersion + '.' + this.minorVersion);
    this.text = new AsciiString(this.textAsString);
    this.keepAliveDefault = keepAliveDefault;
  }
  
  public HttpVersion(String protocolName, int majorVersion, int minorVersion, boolean keepAliveDefault)
  {
    if (protocolName == null) {
      throw new NullPointerException("protocolName");
    }
    protocolName = protocolName.trim().toUpperCase();
    if (protocolName.isEmpty()) {
      throw new IllegalArgumentException("empty protocolName");
    }
    for (int i = 0; i < protocolName.length(); i++) {
      if ((Character.isISOControl(protocolName.charAt(i))) || (Character.isWhitespace(protocolName.charAt(i)))) {
        throw new IllegalArgumentException("invalid character in protocolName");
      }
    }
    if (majorVersion < 0) {
      throw new IllegalArgumentException("negative majorVersion");
    }
    if (minorVersion < 0) {
      throw new IllegalArgumentException("negative minorVersion");
    }
    this.protocolName = new AsciiString(protocolName);
    this.majorVersion = majorVersion;
    this.minorVersion = minorVersion;
    this.textAsString = (protocolName + '/' + majorVersion + '.' + minorVersion);
    this.text = new AsciiString(this.textAsString);
    this.keepAliveDefault = keepAliveDefault;
  }
  
  public AsciiString protocolName()
  {
    return this.protocolName;
  }
  
  public int majorVersion()
  {
    return this.majorVersion;
  }
  
  public int minorVersion()
  {
    return this.minorVersion;
  }
  
  public AsciiString text()
  {
    return this.text;
  }
  
  public boolean isKeepAliveDefault()
  {
    return this.keepAliveDefault;
  }
  
  public String toString()
  {
    return this.textAsString;
  }
  
  public int hashCode()
  {
    return (protocolName().hashCode() * 31 + majorVersion()) * 31 + minorVersion();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof HttpVersion)) {
      return false;
    }
    HttpVersion that = (HttpVersion)o;
    return (minorVersion() == that.minorVersion()) && (majorVersion() == that.majorVersion()) && (protocolName().equals(that.protocolName()));
  }
  
  public int compareTo(HttpVersion o)
  {
    int v = protocolName().compareTo(o.protocolName());
    if (v != 0) {
      return v;
    }
    v = majorVersion() - o.majorVersion();
    if (v != 0) {
      return v;
    }
    return minorVersion() - o.minorVersion();
  }
}
