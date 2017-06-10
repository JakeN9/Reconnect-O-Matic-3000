package io.netty.handler.codec.spdy;

import io.netty.handler.codec.AsciiString;

public final class SpdyHttpHeaders
{
  public static final class Names
  {
    public static final AsciiString STREAM_ID = new AsciiString("X-SPDY-Stream-ID");
    public static final AsciiString ASSOCIATED_TO_STREAM_ID = new AsciiString("X-SPDY-Associated-To-Stream-ID");
    public static final AsciiString PRIORITY = new AsciiString("X-SPDY-Priority");
    public static final AsciiString SCHEME = new AsciiString("X-SPDY-Scheme");
  }
}
