package io.netty.channel.epoll;

import io.netty.channel.ChannelException;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;

final class Native
{
  public static final int EPOLLIN;
  public static final int EPOLLOUT;
  public static final int EPOLLRDHUP;
  public static final int EPOLLET;
  public static final int IOV_MAX;
  public static final int UIO_MAX_IOV;
  public static final boolean IS_SUPPORTING_SENDMMSG;
  private static final byte[] IPV4_MAPPED_IPV6_PREFIX;
  private static final int ERRNO_EBADF_NEGATIVE;
  private static final int ERRNO_EPIPE_NEGATIVE;
  private static final int ERRNO_ECONNRESET_NEGATIVE;
  private static final int ERRNO_EAGAIN_NEGATIVE;
  private static final int ERRNO_EWOULDBLOCK_NEGATIVE;
  private static final int ERRNO_EINPROGRESS_NEGATIVE;
  private static final String[] ERRORS;
  private static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION;
  private static final IOException CONNECTION_RESET_EXCEPTION_WRITE;
  private static final IOException CONNECTION_RESET_EXCEPTION_WRITEV;
  private static final IOException CONNECTION_RESET_EXCEPTION_READ;
  private static final IOException CONNECTION_RESET_EXCEPTION_SENDFILE;
  private static final IOException CONNECTION_RESET_EXCEPTION_SENDTO;
  private static final IOException CONNECTION_RESET_EXCEPTION_SENDMSG;
  private static final IOException CONNECTION_RESET_EXCEPTION_SENDMMSG;
  
  static
  {
    String name = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
    if (!name.startsWith("linux")) {
      throw new IllegalStateException("Only supported on Linux");
    }
    NativeLibraryLoader.load("netty-transport-native-epoll", PlatformDependent.getClassLoader(Native.class));
    
    EPOLLIN = epollin();
    EPOLLOUT = epollout();
    EPOLLRDHUP = epollrdhup();
    EPOLLET = epollet();
    
    IOV_MAX = iovMax();
    UIO_MAX_IOV = uioMaxIov();
    IS_SUPPORTING_SENDMMSG = isSupportingSendmmsg();
    
    IPV4_MAPPED_IPV6_PREFIX = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1 };
    
    ERRNO_EBADF_NEGATIVE = -errnoEBADF();
    ERRNO_EPIPE_NEGATIVE = -errnoEPIPE();
    ERRNO_ECONNRESET_NEGATIVE = -errnoECONNRESET();
    ERRNO_EAGAIN_NEGATIVE = -errnoEAGAIN();
    ERRNO_EWOULDBLOCK_NEGATIVE = -errnoEWOULDBLOCK();
    ERRNO_EINPROGRESS_NEGATIVE = -errnoEINPROGRESS();
    
    ERRORS = new String['Ѐ'];
    for (int i = 0; i < ERRORS.length; i++) {
      ERRORS[i] = strError(i);
    }
    CONNECTION_RESET_EXCEPTION_READ = newConnectionResetException("syscall:read(...)", ERRNO_ECONNRESET_NEGATIVE);
    
    CONNECTION_RESET_EXCEPTION_WRITE = newConnectionResetException("syscall:write(...)", ERRNO_EPIPE_NEGATIVE);
    
    CONNECTION_RESET_EXCEPTION_WRITEV = newConnectionResetException("syscall:writev(...)", ERRNO_EPIPE_NEGATIVE);
    
    CONNECTION_RESET_EXCEPTION_SENDFILE = newConnectionResetException("syscall:sendfile(...)", ERRNO_EPIPE_NEGATIVE);
    
    CONNECTION_RESET_EXCEPTION_SENDTO = newConnectionResetException("syscall:sendto(...)", ERRNO_EPIPE_NEGATIVE);
    
    CONNECTION_RESET_EXCEPTION_SENDMSG = newConnectionResetException("syscall:sendmsg(...)", ERRNO_EPIPE_NEGATIVE);
    
    CONNECTION_RESET_EXCEPTION_SENDMMSG = newConnectionResetException("syscall:sendmmsg(...)", ERRNO_EPIPE_NEGATIVE);
    
    CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();
    CLOSED_CHANNEL_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
  }
  
  private static IOException newConnectionResetException(String method, int errnoNegative)
  {
    IOException exception = newIOException(method, errnoNegative);
    exception.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    return exception;
  }
  
  private static IOException newIOException(String method, int err)
  {
    return new IOException(method + "() failed: " + ERRORS[(-err)]);
  }
  
  private static int ioResult(String method, int err, IOException resetCause)
    throws IOException
  {
    if ((err == ERRNO_EAGAIN_NEGATIVE) || (err == ERRNO_EWOULDBLOCK_NEGATIVE)) {
      return 0;
    }
    if ((err == ERRNO_EPIPE_NEGATIVE) || (err == ERRNO_ECONNRESET_NEGATIVE)) {
      throw resetCause;
    }
    if (err == ERRNO_EBADF_NEGATIVE) {
      throw CLOSED_CHANNEL_EXCEPTION;
    }
    throw newIOException(method, err);
  }
  
  public static int epollWait(int efd, EpollEventArray events, int timeout)
    throws IOException
  {
    int ready = epollWait0(efd, events.memoryAddress(), events.length(), timeout);
    if (ready < 0) {
      throw newIOException("epoll_wait", ready);
    }
    return ready;
  }
  
  public static void close(int fd)
    throws IOException
  {
    int res = close0(fd);
    if (res < 0) {
      throw newIOException("close", res);
    }
  }
  
  public static int write(int fd, ByteBuffer buf, int pos, int limit)
    throws IOException
  {
    int res = write0(fd, buf, pos, limit);
    if (res >= 0) {
      return res;
    }
    return ioResult("write", res, CONNECTION_RESET_EXCEPTION_WRITE);
  }
  
  public static int writeAddress(int fd, long address, int pos, int limit)
    throws IOException
  {
    int res = writeAddress0(fd, address, pos, limit);
    if (res >= 0) {
      return res;
    }
    return ioResult("writeAddress", res, CONNECTION_RESET_EXCEPTION_WRITE);
  }
  
  public static long writev(int fd, ByteBuffer[] buffers, int offset, int length)
    throws IOException
  {
    long res = writev0(fd, buffers, offset, length);
    if (res >= 0L) {
      return res;
    }
    return ioResult("writev", (int)res, CONNECTION_RESET_EXCEPTION_WRITEV);
  }
  
  public static long writevAddresses(int fd, long memoryAddress, int length)
    throws IOException
  {
    long res = writevAddresses0(fd, memoryAddress, length);
    if (res >= 0L) {
      return res;
    }
    return ioResult("writevAddresses", (int)res, CONNECTION_RESET_EXCEPTION_WRITEV);
  }
  
  public static int read(int fd, ByteBuffer buf, int pos, int limit)
    throws IOException
  {
    int res = read0(fd, buf, pos, limit);
    if (res > 0) {
      return res;
    }
    if (res == 0) {
      return -1;
    }
    return ioResult("read", res, CONNECTION_RESET_EXCEPTION_READ);
  }
  
  public static int readAddress(int fd, long address, int pos, int limit)
    throws IOException
  {
    int res = readAddress0(fd, address, pos, limit);
    if (res > 0) {
      return res;
    }
    if (res == 0) {
      return -1;
    }
    return ioResult("readAddress", res, CONNECTION_RESET_EXCEPTION_READ);
  }
  
  public static long sendfile(int dest, DefaultFileRegion src, long baseOffset, long offset, long length)
    throws IOException
  {
    src.open();
    
    long res = sendfile0(dest, src, baseOffset, offset, length);
    if (res >= 0L) {
      return res;
    }
    return ioResult("sendfile", (int)res, CONNECTION_RESET_EXCEPTION_SENDFILE);
  }
  
  public static int sendTo(int fd, ByteBuffer buf, int pos, int limit, InetAddress addr, int port)
    throws IOException
  {
    int scopeId;
    int scopeId;
    byte[] address;
    if ((addr instanceof Inet6Address))
    {
      byte[] address = addr.getAddress();
      scopeId = ((Inet6Address)addr).getScopeId();
    }
    else
    {
      scopeId = 0;
      address = ipv4MappedIpv6Address(addr.getAddress());
    }
    int res = sendTo0(fd, buf, pos, limit, address, scopeId, port);
    if (res >= 0) {
      return res;
    }
    return ioResult("sendTo", res, CONNECTION_RESET_EXCEPTION_SENDTO);
  }
  
  public static int sendToAddress(int fd, long memoryAddress, int pos, int limit, InetAddress addr, int port)
    throws IOException
  {
    int scopeId;
    int scopeId;
    byte[] address;
    if ((addr instanceof Inet6Address))
    {
      byte[] address = addr.getAddress();
      scopeId = ((Inet6Address)addr).getScopeId();
    }
    else
    {
      scopeId = 0;
      address = ipv4MappedIpv6Address(addr.getAddress());
    }
    int res = sendToAddress0(fd, memoryAddress, pos, limit, address, scopeId, port);
    if (res >= 0) {
      return res;
    }
    return ioResult("sendToAddress", res, CONNECTION_RESET_EXCEPTION_SENDTO);
  }
  
  public static int sendToAddresses(int fd, long memoryAddress, int length, InetAddress addr, int port)
    throws IOException
  {
    int scopeId;
    int scopeId;
    byte[] address;
    if ((addr instanceof Inet6Address))
    {
      byte[] address = addr.getAddress();
      scopeId = ((Inet6Address)addr).getScopeId();
    }
    else
    {
      scopeId = 0;
      address = ipv4MappedIpv6Address(addr.getAddress());
    }
    int res = sendToAddresses(fd, memoryAddress, length, address, scopeId, port);
    if (res >= 0) {
      return res;
    }
    return ioResult("sendToAddresses", res, CONNECTION_RESET_EXCEPTION_SENDMSG);
  }
  
  public static int sendmmsg(int fd, NativeDatagramPacketArray.NativeDatagramPacket[] msgs, int offset, int len)
    throws IOException
  {
    int res = sendmmsg0(fd, msgs, offset, len);
    if (res >= 0) {
      return res;
    }
    return ioResult("sendmmsg", res, CONNECTION_RESET_EXCEPTION_SENDMMSG);
  }
  
  public static int socketStreamFd()
  {
    int res = socketStream();
    if (res < 0) {
      throw new ChannelException(newIOException("socketStreamFd", res));
    }
    return res;
  }
  
  public static int socketDgramFd()
  {
    int res = socketDgram();
    if (res < 0) {
      throw new ChannelException(newIOException("socketDgramFd", res));
    }
    return res;
  }
  
  public static int socketDomainFd()
  {
    int res = socketDomain();
    if (res < 0) {
      throw new ChannelException(newIOException("socketDomain", res));
    }
    return res;
  }
  
  public static void bind(int fd, SocketAddress socketAddress)
    throws IOException
  {
    if ((socketAddress instanceof InetSocketAddress))
    {
      InetSocketAddress addr = (InetSocketAddress)socketAddress;
      NativeInetAddress address = toNativeInetAddress(addr.getAddress());
      int res = bind(fd, address.address, address.scopeId, addr.getPort());
      if (res < 0) {
        throw newIOException("bind", res);
      }
    }
    else if ((socketAddress instanceof DomainSocketAddress))
    {
      DomainSocketAddress addr = (DomainSocketAddress)socketAddress;
      int res = bindDomainSocket(fd, addr.path());
      if (res < 0) {
        throw newIOException("bind", res);
      }
    }
    else
    {
      throw new Error("Unexpected SocketAddress implementation " + socketAddress);
    }
  }
  
  public static void listen(int fd, int backlog)
    throws IOException
  {
    int res = listen0(fd, backlog);
    if (res < 0) {
      throw newIOException("listen", res);
    }
  }
  
  public static boolean connect(int fd, SocketAddress socketAddress)
    throws IOException
  {
    int res;
    if ((socketAddress instanceof InetSocketAddress))
    {
      InetSocketAddress inetSocketAddress = (InetSocketAddress)socketAddress;
      NativeInetAddress address = toNativeInetAddress(inetSocketAddress.getAddress());
      res = connect(fd, address.address, address.scopeId, inetSocketAddress.getPort());
    }
    else
    {
      int res;
      if ((socketAddress instanceof DomainSocketAddress))
      {
        DomainSocketAddress unixDomainSocketAddress = (DomainSocketAddress)socketAddress;
        res = connectDomainSocket(fd, unixDomainSocketAddress.path());
      }
      else
      {
        throw new Error("Unexpected SocketAddress implementation " + socketAddress);
      }
    }
    int res;
    if (res < 0)
    {
      if (res == ERRNO_EINPROGRESS_NEGATIVE) {
        return false;
      }
      throw newIOException("connect", res);
    }
    return true;
  }
  
  public static boolean finishConnect(int fd)
    throws IOException
  {
    int res = finishConnect0(fd);
    if (res < 0)
    {
      if (res == ERRNO_EINPROGRESS_NEGATIVE) {
        return false;
      }
      throw newIOException("finishConnect", res);
    }
    return true;
  }
  
  public static InetSocketAddress remoteAddress(int fd)
  {
    byte[] addr = remoteAddress0(fd);
    if (addr == null) {
      return null;
    }
    return address(addr, 0, addr.length);
  }
  
  public static InetSocketAddress localAddress(int fd)
  {
    byte[] addr = localAddress0(fd);
    if (addr == null) {
      return null;
    }
    return address(addr, 0, addr.length);
  }
  
  static InetSocketAddress address(byte[] addr, int offset, int len)
  {
    int port = decodeInt(addr, offset + len - 4);
    try
    {
      InetAddress address;
      switch (len)
      {
      case 8: 
        byte[] ipv4 = new byte[4];
        System.arraycopy(addr, offset, ipv4, 0, 4);
        address = InetAddress.getByAddress(ipv4);
        break;
      case 24: 
        byte[] ipv6 = new byte[16];
        System.arraycopy(addr, offset, ipv6, 0, 16);
        int scopeId = decodeInt(addr, offset + len - 8);
        address = Inet6Address.getByAddress(null, ipv6, scopeId);
        break;
      default: 
        throw new Error();
      }
      return new InetSocketAddress(address, port);
    }
    catch (UnknownHostException e)
    {
      throw new Error("Should never happen", e);
    }
  }
  
  static int decodeInt(byte[] addr, int index)
  {
    return (addr[index] & 0xFF) << 24 | (addr[(index + 1)] & 0xFF) << 16 | (addr[(index + 2)] & 0xFF) << 8 | addr[(index + 3)] & 0xFF;
  }
  
  public static int accept(int fd, byte[] addr)
    throws IOException
  {
    int res = accept0(fd, addr);
    if (res >= 0) {
      return res;
    }
    if ((res == ERRNO_EAGAIN_NEGATIVE) || (res == ERRNO_EWOULDBLOCK_NEGATIVE)) {
      return -1;
    }
    throw newIOException("accept", res);
  }
  
  public static int recvFd(int fd)
    throws IOException
  {
    int res = recvFd0(fd);
    if (res > 0) {
      return res;
    }
    if (res == 0) {
      return -1;
    }
    if ((res == ERRNO_EAGAIN_NEGATIVE) || (res == ERRNO_EWOULDBLOCK_NEGATIVE)) {
      return 0;
    }
    throw newIOException("recvFd", res);
  }
  
  public static int sendFd(int socketFd, int fd)
    throws IOException
  {
    int res = sendFd0(socketFd, fd);
    if (res >= 0) {
      return res;
    }
    if ((res == ERRNO_EAGAIN_NEGATIVE) || (res == ERRNO_EWOULDBLOCK_NEGATIVE)) {
      return -1;
    }
    throw newIOException("sendFd", res);
  }
  
  public static void shutdown(int fd, boolean read, boolean write)
    throws IOException
  {
    int res = shutdown0(fd, read, write);
    if (res < 0) {
      throw newIOException("shutdown", res);
    }
  }
  
  public static void tcpInfo(int fd, EpollTcpInfo info)
  {
    tcpInfo0(fd, info.info);
  }
  
  private static NativeInetAddress toNativeInetAddress(InetAddress addr)
  {
    byte[] bytes = addr.getAddress();
    if ((addr instanceof Inet6Address)) {
      return new NativeInetAddress(bytes, ((Inet6Address)addr).getScopeId());
    }
    return new NativeInetAddress(ipv4MappedIpv6Address(bytes));
  }
  
  static byte[] ipv4MappedIpv6Address(byte[] ipv4)
  {
    byte[] address = new byte[16];
    System.arraycopy(IPV4_MAPPED_IPV6_PREFIX, 0, address, 0, IPV4_MAPPED_IPV6_PREFIX.length);
    System.arraycopy(ipv4, 0, address, 12, ipv4.length);
    return address;
  }
  
  public static native int eventFd();
  
  public static native void eventFdWrite(int paramInt, long paramLong);
  
  public static native void eventFdRead(int paramInt);
  
  public static native int epollCreate();
  
  private static native int epollWait0(int paramInt1, long paramLong, int paramInt2, int paramInt3);
  
  public static native void epollCtlAdd(int paramInt1, int paramInt2, int paramInt3);
  
  public static native void epollCtlMod(int paramInt1, int paramInt2, int paramInt3);
  
  public static native void epollCtlDel(int paramInt1, int paramInt2);
  
  private static native int errnoEBADF();
  
  private static native int errnoEPIPE();
  
  private static native int errnoECONNRESET();
  
  private static native int errnoEAGAIN();
  
  private static native int errnoEWOULDBLOCK();
  
  private static native int errnoEINPROGRESS();
  
  private static native String strError(int paramInt);
  
  private static native int close0(int paramInt);
  
  private static native int write0(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3);
  
  private static native int writeAddress0(int paramInt1, long paramLong, int paramInt2, int paramInt3);
  
  private static native long writev0(int paramInt1, ByteBuffer[] paramArrayOfByteBuffer, int paramInt2, int paramInt3);
  
  private static native long writevAddresses0(int paramInt1, long paramLong, int paramInt2);
  
  private static native int read0(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3);
  
  private static native int readAddress0(int paramInt1, long paramLong, int paramInt2, int paramInt3);
  
  private static native long sendfile0(int paramInt, DefaultFileRegion paramDefaultFileRegion, long paramLong1, long paramLong2, long paramLong3)
    throws IOException;
  
  private static native int sendTo0(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3, byte[] paramArrayOfByte, int paramInt4, int paramInt5);
  
  private static native int sendToAddress0(int paramInt1, long paramLong, int paramInt2, int paramInt3, byte[] paramArrayOfByte, int paramInt4, int paramInt5);
  
  private static native int sendToAddresses(int paramInt1, long paramLong, int paramInt2, byte[] paramArrayOfByte, int paramInt3, int paramInt4);
  
  public static native EpollDatagramChannel.DatagramSocketAddress recvFrom(int paramInt1, ByteBuffer paramByteBuffer, int paramInt2, int paramInt3)
    throws IOException;
  
  public static native EpollDatagramChannel.DatagramSocketAddress recvFromAddress(int paramInt1, long paramLong, int paramInt2, int paramInt3)
    throws IOException;
  
  private static native int sendmmsg0(int paramInt1, NativeDatagramPacketArray.NativeDatagramPacket[] paramArrayOfNativeDatagramPacket, int paramInt2, int paramInt3);
  
  private static native boolean isSupportingSendmmsg();
  
  private static native int socketStream();
  
  private static native int socketDgram();
  
  private static native int socketDomain();
  
  private static native int bind(int paramInt1, byte[] paramArrayOfByte, int paramInt2, int paramInt3);
  
  private static native int bindDomainSocket(int paramInt, String paramString);
  
  private static native int listen0(int paramInt1, int paramInt2);
  
  private static native int connect(int paramInt1, byte[] paramArrayOfByte, int paramInt2, int paramInt3);
  
  private static native int connectDomainSocket(int paramInt, String paramString);
  
  private static native int finishConnect0(int paramInt);
  
  private static native byte[] remoteAddress0(int paramInt);
  
  private static native byte[] localAddress0(int paramInt);
  
  private static native int accept0(int paramInt, byte[] paramArrayOfByte);
  
  private static native int recvFd0(int paramInt);
  
  private static native int sendFd0(int paramInt1, int paramInt2);
  
  private static native int shutdown0(int paramInt, boolean paramBoolean1, boolean paramBoolean2);
  
  public static native int getReceiveBufferSize(int paramInt);
  
  public static native int getSendBufferSize(int paramInt);
  
  public static native int isKeepAlive(int paramInt);
  
  public static native int isReuseAddress(int paramInt);
  
  public static native int isReusePort(int paramInt);
  
  public static native int isTcpNoDelay(int paramInt);
  
  public static native int isTcpCork(int paramInt);
  
  public static native int getSoLinger(int paramInt);
  
  public static native int getTrafficClass(int paramInt);
  
  public static native int isBroadcast(int paramInt);
  
  public static native int getTcpKeepIdle(int paramInt);
  
  public static native int getTcpKeepIntvl(int paramInt);
  
  public static native int getTcpKeepCnt(int paramInt);
  
  public static native int getSoError(int paramInt);
  
  public static native void setKeepAlive(int paramInt1, int paramInt2);
  
  public static native void setReceiveBufferSize(int paramInt1, int paramInt2);
  
  public static native void setReuseAddress(int paramInt1, int paramInt2);
  
  public static native void setReusePort(int paramInt1, int paramInt2);
  
  public static native void setSendBufferSize(int paramInt1, int paramInt2);
  
  public static native void setTcpNoDelay(int paramInt1, int paramInt2);
  
  public static native void setTcpCork(int paramInt1, int paramInt2);
  
  public static native void setSoLinger(int paramInt1, int paramInt2);
  
  public static native void setTrafficClass(int paramInt1, int paramInt2);
  
  public static native void setBroadcast(int paramInt1, int paramInt2);
  
  public static native void setTcpKeepIdle(int paramInt1, int paramInt2);
  
  public static native void setTcpKeepIntvl(int paramInt1, int paramInt2);
  
  public static native void setTcpKeepCnt(int paramInt1, int paramInt2);
  
  private static native void tcpInfo0(int paramInt, int[] paramArrayOfInt);
  
  public static native String kernelVersion();
  
  private static native int iovMax();
  
  private static native int uioMaxIov();
  
  public static native int sizeofEpollEvent();
  
  public static native int offsetofEpollData();
  
  private static native int epollin();
  
  private static native int epollout();
  
  private static native int epollrdhup();
  
  private static native int epollet();
  
  private static class NativeInetAddress
  {
    final byte[] address;
    final int scopeId;
    
    NativeInetAddress(byte[] address, int scopeId)
    {
      this.address = address;
      this.scopeId = scopeId;
    }
    
    NativeInetAddress(byte[] address)
    {
      this(address, 0);
    }
  }
}
