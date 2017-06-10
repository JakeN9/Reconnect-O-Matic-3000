package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.DomainNameMapping;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.IDN;
import java.util.List;
import java.util.Locale;

public class SniHandler
  extends ByteToMessageDecoder
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(SniHandler.class);
  private final DomainNameMapping<SslContext> mapping;
  private boolean handshaken;
  private volatile String hostname;
  private volatile SslContext selectedContext;
  
  public SniHandler(DomainNameMapping<? extends SslContext> mapping)
  {
    if (mapping == null) {
      throw new NullPointerException("mapping");
    }
    this.mapping = mapping;
    this.handshaken = false;
  }
  
  public String hostname()
  {
    return this.hostname;
  }
  
  public SslContext sslContext()
  {
    return this.selectedContext;
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    if ((!this.handshaken) && (in.readableBytes() >= 5))
    {
      String hostname = sniHostNameFromHandshakeInfo(in);
      if (hostname != null) {
        hostname = IDN.toASCII(hostname, 1).toLowerCase(Locale.US);
      }
      this.hostname = hostname;
      
      this.selectedContext = ((SslContext)this.mapping.map(hostname));
    }
    if (this.handshaken)
    {
      SslHandler sslHandler = this.selectedContext.newHandler(ctx.alloc());
      ctx.pipeline().replace(this, SslHandler.class.getName(), sslHandler);
    }
  }
  
  private String sniHostNameFromHandshakeInfo(ByteBuf in)
  {
    int readerIndex = in.readerIndex();
    try
    {
      int command = in.getUnsignedByte(readerIndex);
      switch (command)
      {
      case 20: 
      case 21: 
      case 23: 
        return null;
      case 22: 
        break;
      default: 
        this.handshaken = true;
        return null;
      }
      int majorVersion = in.getUnsignedByte(readerIndex + 1);
      if (majorVersion == 3)
      {
        int packetLength = in.getUnsignedShort(readerIndex + 3) + 5;
        if (in.readableBytes() >= packetLength)
        {
          int offset = readerIndex + 43;
          
          int sessionIdLength = in.getUnsignedByte(offset);
          offset += sessionIdLength + 1;
          
          int cipherSuitesLength = in.getUnsignedShort(offset);
          offset += cipherSuitesLength + 2;
          
          int compressionMethodLength = in.getUnsignedByte(offset);
          offset += compressionMethodLength + 1;
          
          int extensionsLength = in.getUnsignedShort(offset);
          offset += 2;
          int extensionsLimit = offset + extensionsLength;
          while (offset < extensionsLimit)
          {
            int extensionType = in.getUnsignedShort(offset);
            offset += 2;
            
            int extensionLength = in.getUnsignedShort(offset);
            offset += 2;
            if (extensionType == 0)
            {
              this.handshaken = true;
              int serverNameType = in.getUnsignedByte(offset + 2);
              if (serverNameType == 0)
              {
                int serverNameLength = in.getUnsignedShort(offset + 3);
                return in.toString(offset + 5, serverNameLength, CharsetUtil.UTF_8);
              }
              return null;
            }
            offset += extensionLength;
          }
          this.handshaken = true;
          return null;
        }
        return null;
      }
      this.handshaken = true;
      return null;
    }
    catch (Throwable e)
    {
      if (logger.isDebugEnabled()) {
        logger.debug("Unexpected client hello packet: " + ByteBufUtil.hexDump(in), e);
      }
      this.handshaken = true;
    }
    return null;
  }
}
