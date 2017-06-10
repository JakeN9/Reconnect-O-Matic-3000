package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import java.net.InetSocketAddress;
import java.util.List;

@ChannelHandler.Sharable
public class DnsResponseDecoder
  extends MessageToMessageDecoder<DatagramPacket>
{
  protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out)
    throws Exception
  {
    ByteBuf buf = (ByteBuf)packet.content();
    
    int id = buf.readUnsignedShort();
    
    DnsResponse response = new DnsResponse(id, (InetSocketAddress)packet.sender());
    DnsResponseHeader header = response.header();
    int flags = buf.readUnsignedShort();
    header.setType(flags >> 15);
    header.setOpcode(flags >> 11 & 0xF);
    header.setRecursionDesired((flags >> 8 & 0x1) == 1);
    header.setAuthoritativeAnswer((flags >> 10 & 0x1) == 1);
    header.setTruncated((flags >> 9 & 0x1) == 1);
    header.setRecursionAvailable((flags >> 7 & 0x1) == 1);
    header.setZ(flags >> 4 & 0x7);
    header.setResponseCode(DnsResponseCode.valueOf(flags & 0xF));
    
    int questions = buf.readUnsignedShort();
    int answers = buf.readUnsignedShort();
    int authorities = buf.readUnsignedShort();
    int additionals = buf.readUnsignedShort();
    for (int i = 0; i < questions; i++) {
      response.addQuestion(decodeQuestion(buf));
    }
    if (header.responseCode() != DnsResponseCode.NOERROR)
    {
      out.add(response);
      return;
    }
    boolean release = true;
    try
    {
      for (int i = 0; i < answers; i++) {
        response.addAnswer(decodeResource(buf));
      }
      for (int i = 0; i < authorities; i++) {
        response.addAuthorityResource(decodeResource(buf));
      }
      for (int i = 0; i < additionals; i++) {
        response.addAdditionalResource(decodeResource(buf));
      }
      out.add(response);
      release = false;
    }
    finally
    {
      if (release)
      {
        releaseDnsResources(response.answers());
        releaseDnsResources(response.authorityResources());
        releaseDnsResources(response.additionalResources());
      }
    }
  }
  
  private static void releaseDnsResources(List<DnsResource> resources)
  {
    int size = resources.size();
    for (int i = 0; i < size; i++)
    {
      DnsResource resource = (DnsResource)resources.get(i);
      resource.release();
    }
  }
  
  private static String readName(ByteBuf buf)
  {
    int position = -1;
    int checked = 0;
    int length = buf.writerIndex();
    StringBuilder name = new StringBuilder();
    for (int len = buf.readUnsignedByte(); (buf.isReadable()) && (len != 0); len = buf.readUnsignedByte())
    {
      boolean pointer = (len & 0xC0) == 192;
      if (pointer)
      {
        if (position == -1) {
          position = buf.readerIndex() + 1;
        }
        buf.readerIndex((len & 0x3F) << 8 | buf.readUnsignedByte());
        
        checked += 2;
        if (checked >= length) {
          throw new CorruptedFrameException("name contains a loop.");
        }
      }
      else
      {
        name.append(buf.toString(buf.readerIndex(), len, CharsetUtil.UTF_8)).append('.');
        buf.skipBytes(len);
      }
    }
    if (position != -1) {
      buf.readerIndex(position);
    }
    if (name.length() == 0) {
      return "";
    }
    return name.substring(0, name.length() - 1);
  }
  
  private static DnsQuestion decodeQuestion(ByteBuf buf)
  {
    String name = readName(buf);
    DnsType type = DnsType.valueOf(buf.readUnsignedShort());
    DnsClass qClass = DnsClass.valueOf(buf.readUnsignedShort());
    return new DnsQuestion(name, type, qClass);
  }
  
  private static DnsResource decodeResource(ByteBuf buf)
  {
    String name = readName(buf);
    DnsType type = DnsType.valueOf(buf.readUnsignedShort());
    DnsClass aClass = DnsClass.valueOf(buf.readUnsignedShort());
    long ttl = buf.readUnsignedInt();
    int len = buf.readUnsignedShort();
    
    int readerIndex = buf.readerIndex();
    ByteBuf payload = buf.duplicate().setIndex(readerIndex, readerIndex + len).retain();
    buf.readerIndex(readerIndex + len);
    return new DnsResource(name, type, aClass, ttl, payload);
  }
}
