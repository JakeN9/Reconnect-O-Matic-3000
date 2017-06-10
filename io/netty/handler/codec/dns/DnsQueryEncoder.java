package io.netty.handler.codec.dns;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import java.nio.charset.Charset;
import java.util.List;

@ChannelHandler.Sharable
public class DnsQueryEncoder
  extends MessageToMessageEncoder<DnsQuery>
{
  protected void encode(ChannelHandlerContext ctx, DnsQuery query, List<Object> out)
    throws Exception
  {
    ByteBuf buf = ctx.alloc().buffer();
    encodeHeader(query.header(), buf);
    List<DnsQuestion> questions = query.questions();
    for (DnsQuestion question : questions) {
      encodeQuestion(question, CharsetUtil.US_ASCII, buf);
    }
    for (DnsResource resource : query.additionalResources()) {
      encodeResource(resource, CharsetUtil.US_ASCII, buf);
    }
    out.add(new DatagramPacket(buf, query.recipient(), null));
  }
  
  private static void encodeHeader(DnsHeader header, ByteBuf buf)
  {
    buf.writeShort(header.id());
    int flags = 0;
    flags |= header.type() << 15;
    flags |= header.opcode() << 14;
    flags |= (header.isRecursionDesired() ? 256 : 0);
    buf.writeShort(flags);
    buf.writeShort(header.questionCount());
    buf.writeShort(0);
    buf.writeShort(0);
    buf.writeShort(header.additionalResourceCount());
  }
  
  private static void encodeQuestion(DnsQuestion question, Charset charset, ByteBuf buf)
  {
    encodeName(question.name(), charset, buf);
    buf.writeShort(question.type().intValue());
    buf.writeShort(question.dnsClass().intValue());
  }
  
  private static void encodeResource(DnsResource resource, Charset charset, ByteBuf buf)
  {
    encodeName(resource.name(), charset, buf);
    
    buf.writeShort(resource.type().intValue());
    buf.writeShort(resource.dnsClass().intValue());
    buf.writeInt((int)resource.timeToLive());
    
    ByteBuf content = resource.content();
    int contentLen = content.readableBytes();
    
    buf.writeShort(contentLen);
    buf.writeBytes(content, content.readerIndex(), contentLen);
  }
  
  private static void encodeName(String name, Charset charset, ByteBuf buf)
  {
    String[] parts = StringUtil.split(name, '.');
    for (String part : parts)
    {
      int partLen = part.length();
      if (partLen != 0)
      {
        buf.writeByte(partLen);
        buf.writeBytes(part.getBytes(charset));
      }
    }
    buf.writeByte(0);
  }
}
