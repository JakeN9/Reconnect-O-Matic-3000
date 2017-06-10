package io.netty.handler.codec.http2;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.BinaryHeaders;
import io.netty.handler.codec.EmptyBinaryHeaders;

public final class EmptyHttp2Headers
  extends EmptyBinaryHeaders
  implements Http2Headers
{
  public static final EmptyHttp2Headers INSTANCE = new EmptyHttp2Headers();
  
  public Http2Headers add(AsciiString name, AsciiString value)
  {
    super.add(name, value);
    return this;
  }
  
  public Http2Headers add(AsciiString name, Iterable<? extends AsciiString> values)
  {
    super.add(name, values);
    return this;
  }
  
  public Http2Headers add(AsciiString name, AsciiString... values)
  {
    super.add(name, values);
    return this;
  }
  
  public Http2Headers addObject(AsciiString name, Object value)
  {
    super.addObject(name, value);
    return this;
  }
  
  public Http2Headers addObject(AsciiString name, Iterable<?> values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public Http2Headers addObject(AsciiString name, Object... values)
  {
    super.addObject(name, values);
    return this;
  }
  
  public Http2Headers addBoolean(AsciiString name, boolean value)
  {
    super.addBoolean(name, value);
    return this;
  }
  
  public Http2Headers addChar(AsciiString name, char value)
  {
    super.addChar(name, value);
    return this;
  }
  
  public Http2Headers addByte(AsciiString name, byte value)
  {
    super.addByte(name, value);
    return this;
  }
  
  public Http2Headers addShort(AsciiString name, short value)
  {
    super.addShort(name, value);
    return this;
  }
  
  public Http2Headers addInt(AsciiString name, int value)
  {
    super.addInt(name, value);
    return this;
  }
  
  public Http2Headers addLong(AsciiString name, long value)
  {
    super.addLong(name, value);
    return this;
  }
  
  public Http2Headers addFloat(AsciiString name, float value)
  {
    super.addFloat(name, value);
    return this;
  }
  
  public Http2Headers addDouble(AsciiString name, double value)
  {
    super.addDouble(name, value);
    return this;
  }
  
  public Http2Headers addTimeMillis(AsciiString name, long value)
  {
    super.addTimeMillis(name, value);
    return this;
  }
  
  public Http2Headers add(BinaryHeaders headers)
  {
    super.add(headers);
    return this;
  }
  
  public Http2Headers set(AsciiString name, AsciiString value)
  {
    super.set(name, value);
    return this;
  }
  
  public Http2Headers set(AsciiString name, Iterable<? extends AsciiString> values)
  {
    super.set(name, values);
    return this;
  }
  
  public Http2Headers set(AsciiString name, AsciiString... values)
  {
    super.set(name, values);
    return this;
  }
  
  public Http2Headers setObject(AsciiString name, Object value)
  {
    super.setObject(name, value);
    return this;
  }
  
  public Http2Headers setObject(AsciiString name, Iterable<?> values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public Http2Headers setObject(AsciiString name, Object... values)
  {
    super.setObject(name, values);
    return this;
  }
  
  public Http2Headers setBoolean(AsciiString name, boolean value)
  {
    super.setBoolean(name, value);
    return this;
  }
  
  public Http2Headers setChar(AsciiString name, char value)
  {
    super.setChar(name, value);
    return this;
  }
  
  public Http2Headers setByte(AsciiString name, byte value)
  {
    super.setByte(name, value);
    return this;
  }
  
  public Http2Headers setShort(AsciiString name, short value)
  {
    super.setShort(name, value);
    return this;
  }
  
  public Http2Headers setInt(AsciiString name, int value)
  {
    super.setInt(name, value);
    return this;
  }
  
  public Http2Headers setLong(AsciiString name, long value)
  {
    super.setLong(name, value);
    return this;
  }
  
  public Http2Headers setFloat(AsciiString name, float value)
  {
    super.setFloat(name, value);
    return this;
  }
  
  public Http2Headers setDouble(AsciiString name, double value)
  {
    super.setDouble(name, value);
    return this;
  }
  
  public Http2Headers setTimeMillis(AsciiString name, long value)
  {
    super.setTimeMillis(name, value);
    return this;
  }
  
  public Http2Headers set(BinaryHeaders headers)
  {
    super.set(headers);
    return this;
  }
  
  public Http2Headers setAll(BinaryHeaders headers)
  {
    super.setAll(headers);
    return this;
  }
  
  public Http2Headers clear()
  {
    super.clear();
    return this;
  }
  
  public EmptyHttp2Headers method(AsciiString method)
  {
    throw new UnsupportedOperationException();
  }
  
  public EmptyHttp2Headers scheme(AsciiString status)
  {
    throw new UnsupportedOperationException();
  }
  
  public EmptyHttp2Headers authority(AsciiString authority)
  {
    throw new UnsupportedOperationException();
  }
  
  public EmptyHttp2Headers path(AsciiString path)
  {
    throw new UnsupportedOperationException();
  }
  
  public EmptyHttp2Headers status(AsciiString status)
  {
    throw new UnsupportedOperationException();
  }
  
  public AsciiString method()
  {
    return (AsciiString)get(Http2Headers.PseudoHeaderName.METHOD.value());
  }
  
  public AsciiString scheme()
  {
    return (AsciiString)get(Http2Headers.PseudoHeaderName.SCHEME.value());
  }
  
  public AsciiString authority()
  {
    return (AsciiString)get(Http2Headers.PseudoHeaderName.AUTHORITY.value());
  }
  
  public AsciiString path()
  {
    return (AsciiString)get(Http2Headers.PseudoHeaderName.PATH.value());
  }
  
  public AsciiString status()
  {
    return (AsciiString)get(Http2Headers.PseudoHeaderName.STATUS.value());
  }
}
