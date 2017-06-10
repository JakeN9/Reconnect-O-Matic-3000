package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.net.ssl.SSLSessionContext;
import javax.security.cert.CertificateException;
import org.apache.tomcat.jni.Buffer;
import org.apache.tomcat.jni.SSL;

public final class OpenSslEngine
  extends SSLEngine
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(OpenSslEngine.class);
  private static final Certificate[] EMPTY_CERTIFICATES = new Certificate[0];
  private static final SSLException ENGINE_CLOSED = new SSLException("engine closed");
  private static final SSLException RENEGOTIATION_UNSUPPORTED = new SSLException("renegotiation unsupported");
  private static final SSLException ENCRYPTED_PACKET_OVERSIZED = new SSLException("encrypted packet oversized");
  private static final int MAX_PLAINTEXT_LENGTH = 16384;
  private static final int MAX_COMPRESSED_LENGTH = 17408;
  private static final int MAX_CIPHERTEXT_LENGTH = 18432;
  private static final String PROTOCOL_SSL_V2_HELLO = "SSLv2Hello";
  private static final String PROTOCOL_SSL_V2 = "SSLv2";
  private static final String PROTOCOL_SSL_V3 = "SSLv3";
  private static final String PROTOCOL_TLS_V1 = "TLSv1";
  private static final String PROTOCOL_TLS_V1_1 = "TLSv1.1";
  private static final String PROTOCOL_TLS_V1_2 = "TLSv1.2";
  
  static
  {
    ENGINE_CLOSED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    RENEGOTIATION_UNSUPPORTED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    ENCRYPTED_PACKET_OVERSIZED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    
    AtomicIntegerFieldUpdater<OpenSslEngine> destroyedUpdater = PlatformDependent.newAtomicIntegerFieldUpdater(OpenSslEngine.class, "destroyed");
    if (destroyedUpdater == null) {
      destroyedUpdater = AtomicIntegerFieldUpdater.newUpdater(OpenSslEngine.class, "destroyed");
    }
    DESTROYED_UPDATER = destroyedUpdater;
    AtomicReferenceFieldUpdater<OpenSslEngine, SSLSession> sessionUpdater = PlatformDependent.newAtomicReferenceFieldUpdater(OpenSslEngine.class, "session");
    if (sessionUpdater == null) {
      sessionUpdater = AtomicReferenceFieldUpdater.newUpdater(OpenSslEngine.class, SSLSession.class, "session");
    }
    SESSION_UPDATER = sessionUpdater;
  }
  
  private static final String[] SUPPORTED_PROTOCOLS = { "SSLv2Hello", "SSLv2", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2" };
  private static final Set<String> SUPPORTED_PROTOCOLS_SET = new HashSet(Arrays.asList(SUPPORTED_PROTOCOLS));
  static final int MAX_ENCRYPTED_PACKET_LENGTH = 18713;
  static final int MAX_ENCRYPTION_OVERHEAD_LENGTH = 2329;
  private static final AtomicIntegerFieldUpdater<OpenSslEngine> DESTROYED_UPDATER;
  private static final AtomicReferenceFieldUpdater<OpenSslEngine, SSLSession> SESSION_UPDATER;
  private static final String INVALID_CIPHER = "SSL_NULL_WITH_NULL_NULL";
  
  static enum ClientAuthMode
  {
    NONE,  OPTIONAL,  REQUIRE;
    
    private ClientAuthMode() {}
  }
  
  private static final long EMPTY_ADDR = Buffer.address(Unpooled.EMPTY_BUFFER.nioBuffer());
  private long ssl;
  private long networkBIO;
  private int accepted;
  private boolean handshakeFinished;
  private boolean receivedShutdown;
  private volatile int destroyed;
  private volatile String cipher;
  private volatile String applicationProtocol;
  private volatile Certificate[] peerCerts;
  private volatile ClientAuthMode clientAuth = ClientAuthMode.NONE;
  private boolean isInboundDone;
  private boolean isOutboundDone;
  private boolean engineClosed;
  private final boolean clientMode;
  private final ByteBufAllocator alloc;
  private final String fallbackApplicationProtocol;
  private final OpenSslSessionContext sessionContext;
  private volatile SSLSession session;
  
  @Deprecated
  public OpenSslEngine(long sslCtx, ByteBufAllocator alloc, String fallbackApplicationProtocol)
  {
    this(sslCtx, alloc, fallbackApplicationProtocol, false, null);
  }
  
  OpenSslEngine(long sslCtx, ByteBufAllocator alloc, String fallbackApplicationProtocol, boolean clientMode, OpenSslSessionContext sessionContext)
  {
    OpenSsl.ensureAvailability();
    if (sslCtx == 0L) {
      throw new NullPointerException("sslContext");
    }
    if (alloc == null) {
      throw new NullPointerException("alloc");
    }
    this.alloc = alloc;
    this.ssl = SSL.newSSL(sslCtx, !clientMode);
    this.networkBIO = SSL.makeNetworkBIO(this.ssl);
    this.fallbackApplicationProtocol = fallbackApplicationProtocol;
    this.clientMode = clientMode;
    this.sessionContext = sessionContext;
  }
  
  public synchronized void shutdown()
  {
    if (DESTROYED_UPDATER.compareAndSet(this, 0, 1))
    {
      SSL.freeSSL(this.ssl);
      SSL.freeBIO(this.networkBIO);
      this.ssl = (this.networkBIO = 0L);
      
      this.isInboundDone = (this.isOutboundDone = this.engineClosed = 1);
    }
  }
  
  private int writePlaintextData(ByteBuffer src)
  {
    int pos = src.position();
    int limit = src.limit();
    int len = Math.min(limit - pos, 16384);
    int sslWrote;
    if (src.isDirect())
    {
      long addr = Buffer.address(src) + pos;
      int sslWrote = SSL.writeToSSL(this.ssl, addr, len);
      if (sslWrote > 0)
      {
        src.position(pos + sslWrote);
        return sslWrote;
      }
    }
    else
    {
      ByteBuf buf = this.alloc.directBuffer(len);
      try
      {
        long addr = memoryAddress(buf);
        
        src.limit(pos + len);
        
        buf.setBytes(0, src);
        src.limit(limit);
        
        sslWrote = SSL.writeToSSL(this.ssl, addr, len);
        if (sslWrote > 0)
        {
          src.position(pos + sslWrote);
          return sslWrote;
        }
        src.position(pos);
      }
      finally
      {
        buf.release();
      }
    }
    throw new IllegalStateException("SSL.writeToSSL() returned a non-positive value: " + sslWrote);
  }
  
  private int writeEncryptedData(ByteBuffer src)
  {
    int pos = src.position();
    int len = src.remaining();
    if (src.isDirect())
    {
      long addr = Buffer.address(src) + pos;
      int netWrote = SSL.writeToBIO(this.networkBIO, addr, len);
      if (netWrote >= 0)
      {
        src.position(pos + netWrote);
        return netWrote;
      }
    }
    else
    {
      ByteBuf buf = this.alloc.directBuffer(len);
      try
      {
        long addr = memoryAddress(buf);
        
        buf.setBytes(0, src);
        
        int netWrote = SSL.writeToBIO(this.networkBIO, addr, len);
        if (netWrote >= 0)
        {
          src.position(pos + netWrote);
          return netWrote;
        }
        src.position(pos);
      }
      finally
      {
        buf.release();
      }
    }
    return -1;
  }
  
  private int readPlaintextData(ByteBuffer dst)
  {
    if (dst.isDirect())
    {
      int pos = dst.position();
      long addr = Buffer.address(dst) + pos;
      int len = dst.limit() - pos;
      int sslRead = SSL.readFromSSL(this.ssl, addr, len);
      if (sslRead > 0)
      {
        dst.position(pos + sslRead);
        return sslRead;
      }
    }
    else
    {
      int pos = dst.position();
      int limit = dst.limit();
      int len = Math.min(18713, limit - pos);
      ByteBuf buf = this.alloc.directBuffer(len);
      try
      {
        long addr = memoryAddress(buf);
        
        int sslRead = SSL.readFromSSL(this.ssl, addr, len);
        if (sslRead > 0)
        {
          dst.limit(pos + sslRead);
          buf.getBytes(0, dst);
          dst.limit(limit);
          return sslRead;
        }
      }
      finally
      {
        buf.release();
      }
    }
    return 0;
  }
  
  private int readEncryptedData(ByteBuffer dst, int pending)
  {
    if ((dst.isDirect()) && (dst.remaining() >= pending))
    {
      int pos = dst.position();
      long addr = Buffer.address(dst) + pos;
      int bioRead = SSL.readFromBIO(this.networkBIO, addr, pending);
      if (bioRead > 0)
      {
        dst.position(pos + bioRead);
        return bioRead;
      }
    }
    else
    {
      ByteBuf buf = this.alloc.directBuffer(pending);
      try
      {
        long addr = memoryAddress(buf);
        
        int bioRead = SSL.readFromBIO(this.networkBIO, addr, pending);
        if (bioRead > 0)
        {
          int oldLimit = dst.limit();
          dst.limit(dst.position() + bioRead);
          buf.getBytes(0, dst);
          dst.limit(oldLimit);
          return bioRead;
        }
      }
      finally
      {
        buf.release();
      }
    }
    return 0;
  }
  
  public synchronized SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst)
    throws SSLException
  {
    if (this.destroyed != 0) {
      return new SSLEngineResult(SSLEngineResult.Status.CLOSED, SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
    }
    if (srcs == null) {
      throw new IllegalArgumentException("srcs is null");
    }
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    if ((offset >= srcs.length) || (offset + length > srcs.length)) {
      throw new IndexOutOfBoundsException("offset: " + offset + ", length: " + length + " (expected: offset <= offset + length <= srcs.length (" + srcs.length + "))");
    }
    if (dst.isReadOnly()) {
      throw new ReadOnlyBufferException();
    }
    if (this.accepted == 0) {
      beginHandshakeImplicitly();
    }
    SSLEngineResult.HandshakeStatus handshakeStatus = getHandshakeStatus();
    if (((!this.handshakeFinished) || (this.engineClosed)) && (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)) {
      return new SSLEngineResult(getEngineStatus(), SSLEngineResult.HandshakeStatus.NEED_UNWRAP, 0, 0);
    }
    int bytesProduced = 0;
    
    int pendingNet = SSL.pendingWrittenBytesInBIO(this.networkBIO);
    if (pendingNet > 0)
    {
      int capacity = dst.remaining();
      if (capacity < pendingNet) {
        return new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, handshakeStatus, 0, bytesProduced);
      }
      try
      {
        bytesProduced += readEncryptedData(dst, pendingNet);
      }
      catch (Exception e)
      {
        throw new SSLException(e);
      }
      if (this.isOutboundDone) {
        shutdown();
      }
      return new SSLEngineResult(getEngineStatus(), getHandshakeStatus(), 0, bytesProduced);
    }
    int bytesConsumed = 0;
    int endOffset = offset + length;
    for (int i = offset; i < endOffset; i++)
    {
      ByteBuffer src = srcs[i];
      if (src == null) {
        throw new IllegalArgumentException("srcs[" + i + "] is null");
      }
      while (src.hasRemaining())
      {
        try
        {
          bytesConsumed += writePlaintextData(src);
        }
        catch (Exception e)
        {
          throw new SSLException(e);
        }
        pendingNet = SSL.pendingWrittenBytesInBIO(this.networkBIO);
        if (pendingNet > 0)
        {
          int capacity = dst.remaining();
          if (capacity < pendingNet) {
            return new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, getHandshakeStatus(), bytesConsumed, bytesProduced);
          }
          try
          {
            bytesProduced += readEncryptedData(dst, pendingNet);
          }
          catch (Exception e)
          {
            throw new SSLException(e);
          }
          return new SSLEngineResult(getEngineStatus(), getHandshakeStatus(), bytesConsumed, bytesProduced);
        }
      }
    }
    return new SSLEngineResult(getEngineStatus(), getHandshakeStatus(), bytesConsumed, bytesProduced);
  }
  
  public synchronized SSLEngineResult unwrap(ByteBuffer[] srcs, int srcsOffset, int srcsLength, ByteBuffer[] dsts, int dstsOffset, int dstsLength)
    throws SSLException
  {
    if (this.destroyed != 0) {
      return new SSLEngineResult(SSLEngineResult.Status.CLOSED, SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
    }
    if (srcs == null) {
      throw new NullPointerException("srcs");
    }
    if ((srcsOffset >= srcs.length) || (srcsOffset + srcsLength > srcs.length)) {
      throw new IndexOutOfBoundsException("offset: " + srcsOffset + ", length: " + srcsLength + " (expected: offset <= offset + length <= srcs.length (" + srcs.length + "))");
    }
    if (dsts == null) {
      throw new IllegalArgumentException("dsts is null");
    }
    if ((dstsOffset >= dsts.length) || (dstsOffset + dstsLength > dsts.length)) {
      throw new IndexOutOfBoundsException("offset: " + dstsOffset + ", length: " + dstsLength + " (expected: offset <= offset + length <= dsts.length (" + dsts.length + "))");
    }
    int capacity = 0;
    int endOffset = dstsOffset + dstsLength;
    for (int i = dstsOffset; i < endOffset; i++)
    {
      ByteBuffer dst = dsts[i];
      if (dst == null) {
        throw new IllegalArgumentException("dsts[" + i + "] is null");
      }
      if (dst.isReadOnly()) {
        throw new ReadOnlyBufferException();
      }
      capacity += dst.remaining();
    }
    if (this.accepted == 0) {
      beginHandshakeImplicitly();
    }
    SSLEngineResult.HandshakeStatus handshakeStatus = getHandshakeStatus();
    if (((!this.handshakeFinished) || (this.engineClosed)) && (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP)) {
      return new SSLEngineResult(getEngineStatus(), SSLEngineResult.HandshakeStatus.NEED_WRAP, 0, 0);
    }
    int srcsEndOffset = srcsOffset + srcsLength;
    int len = 0;
    for (int i = srcsOffset; i < srcsEndOffset; i++)
    {
      ByteBuffer src = srcs[i];
      if (src == null) {
        throw new IllegalArgumentException("srcs[" + i + "] is null");
      }
      len += src.remaining();
    }
    if (len > 18713)
    {
      this.isInboundDone = true;
      this.isOutboundDone = true;
      this.engineClosed = true;
      shutdown();
      throw ENCRYPTED_PACKET_OVERSIZED;
    }
    int bytesConsumed = -1;
    try
    {
      while (srcsOffset < srcsEndOffset)
      {
        ByteBuffer src = srcs[srcsOffset];
        int remaining = src.remaining();
        int written = writeEncryptedData(src);
        if (written < 0) {
          break;
        }
        if (bytesConsumed == -1) {
          bytesConsumed = written;
        } else {
          bytesConsumed += written;
        }
        if (written == remaining) {
          srcsOffset++;
        } else {
          if (written == 0) {
            break;
          }
        }
      }
    }
    catch (Exception e)
    {
      throw new SSLException(e);
    }
    if (bytesConsumed >= 0)
    {
      int lastPrimingReadResult = SSL.readFromSSL(this.ssl, EMPTY_ADDR, 0);
      if (lastPrimingReadResult <= 0)
      {
        long error = SSL.getLastErrorNumber();
        if (OpenSsl.isError(error))
        {
          String err = SSL.getErrorString(error);
          if (logger.isDebugEnabled()) {
            logger.debug("SSL_read failed: primingReadResult: " + lastPrimingReadResult + "; OpenSSL error: '" + err + '\'');
          }
          shutdown();
          throw new SSLException(err);
        }
      }
    }
    else
    {
      bytesConsumed = 0;
    }
    int pendingApp = (this.handshakeFinished) || (SSL.isInInit(this.ssl) == 0) ? SSL.pendingReadableBytesInSSL(this.ssl) : 0;
    int bytesProduced = 0;
    if (pendingApp > 0)
    {
      if (capacity < pendingApp) {
        return new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, getHandshakeStatus(), bytesConsumed, 0);
      }
      int idx = dstsOffset;
      while (idx < endOffset)
      {
        ByteBuffer dst = dsts[idx];
        if (!dst.hasRemaining())
        {
          idx++;
        }
        else
        {
          if (pendingApp <= 0) {
            break;
          }
          int bytesRead;
          try
          {
            bytesRead = readPlaintextData(dst);
          }
          catch (Exception e)
          {
            throw new SSLException(e);
          }
          if (bytesRead == 0) {
            break;
          }
          bytesProduced += bytesRead;
          pendingApp -= bytesRead;
          if (!dst.hasRemaining()) {
            idx++;
          }
        }
      }
    }
    if ((!this.receivedShutdown) && ((SSL.getShutdown(this.ssl) & 0x2) == 2))
    {
      this.receivedShutdown = true;
      closeOutbound();
      closeInbound();
    }
    return new SSLEngineResult(getEngineStatus(), getHandshakeStatus(), bytesConsumed, bytesProduced);
  }
  
  public SSLEngineResult unwrap(ByteBuffer[] srcs, ByteBuffer[] dsts)
    throws SSLException
  {
    return unwrap(srcs, 0, srcs.length, dsts, 0, dsts.length);
  }
  
  public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length)
    throws SSLException
  {
    return unwrap(new ByteBuffer[] { src }, 0, 1, dsts, offset, length);
  }
  
  public Runnable getDelegatedTask()
  {
    return null;
  }
  
  public synchronized void closeInbound()
    throws SSLException
  {
    if (this.isInboundDone) {
      return;
    }
    this.isInboundDone = true;
    this.engineClosed = true;
    
    shutdown();
    if ((this.accepted != 0) && (!this.receivedShutdown)) {
      throw new SSLException("Inbound closed before receiving peer's close_notify: possible truncation attack?");
    }
  }
  
  public synchronized boolean isInboundDone()
  {
    return (this.isInboundDone) || (this.engineClosed);
  }
  
  public synchronized void closeOutbound()
  {
    if (this.isOutboundDone) {
      return;
    }
    this.isOutboundDone = true;
    this.engineClosed = true;
    if ((this.accepted != 0) && (this.destroyed == 0))
    {
      int mode = SSL.getShutdown(this.ssl);
      if ((mode & 0x1) != 1) {
        SSL.shutdownSSL(this.ssl);
      }
    }
    else
    {
      shutdown();
    }
  }
  
  public synchronized boolean isOutboundDone()
  {
    return this.isOutboundDone;
  }
  
  public String[] getSupportedCipherSuites()
  {
    Set<String> availableCipherSuites = OpenSsl.availableCipherSuites();
    return (String[])availableCipherSuites.toArray(new String[availableCipherSuites.size()]);
  }
  
  public String[] getEnabledCipherSuites()
  {
    String[] enabled = SSL.getCiphers(this.ssl);
    if (enabled == null) {
      return EmptyArrays.EMPTY_STRINGS;
    }
    for (int i = 0; i < enabled.length; i++)
    {
      String mapped = toJavaCipherSuite(enabled[i]);
      if (mapped != null) {
        enabled[i] = mapped;
      }
    }
    return enabled;
  }
  
  public void setEnabledCipherSuites(String[] cipherSuites)
  {
    if (cipherSuites == null) {
      throw new NullPointerException("cipherSuites");
    }
    StringBuilder buf = new StringBuilder();
    for (String c : cipherSuites)
    {
      if (c == null) {
        break;
      }
      String converted = CipherSuiteConverter.toOpenSsl(c);
      if (converted == null) {
        converted = c;
      }
      if (!OpenSsl.isCipherSuiteAvailable(converted)) {
        throw new IllegalArgumentException("unsupported cipher suite: " + c + '(' + converted + ')');
      }
      buf.append(converted);
      buf.append(':');
    }
    if (buf.length() == 0) {
      throw new IllegalArgumentException("empty cipher suites");
    }
    buf.setLength(buf.length() - 1);
    
    String cipherSuiteSpec = buf.toString();
    try
    {
      SSL.setCipherSuites(this.ssl, cipherSuiteSpec);
    }
    catch (Exception e)
    {
      throw new IllegalStateException("failed to enable cipher suites: " + cipherSuiteSpec, e);
    }
  }
  
  public String[] getSupportedProtocols()
  {
    return (String[])SUPPORTED_PROTOCOLS.clone();
  }
  
  public String[] getEnabledProtocols()
  {
    List<String> enabled = new ArrayList();
    
    enabled.add("SSLv2Hello");
    int opts = SSL.getOptions(this.ssl);
    if ((opts & 0x4000000) == 0) {
      enabled.add("TLSv1");
    }
    if ((opts & 0x8000000) == 0) {
      enabled.add("TLSv1.1");
    }
    if ((opts & 0x10000000) == 0) {
      enabled.add("TLSv1.2");
    }
    if ((opts & 0x1000000) == 0) {
      enabled.add("SSLv2");
    }
    if ((opts & 0x2000000) == 0) {
      enabled.add("SSLv3");
    }
    int size = enabled.size();
    if (size == 0) {
      return EmptyArrays.EMPTY_STRINGS;
    }
    return (String[])enabled.toArray(new String[size]);
  }
  
  public void setEnabledProtocols(String[] protocols)
  {
    if (protocols == null) {
      throw new IllegalArgumentException();
    }
    boolean sslv2 = false;
    boolean sslv3 = false;
    boolean tlsv1 = false;
    boolean tlsv1_1 = false;
    boolean tlsv1_2 = false;
    for (String p : protocols)
    {
      if (!SUPPORTED_PROTOCOLS_SET.contains(p)) {
        throw new IllegalArgumentException("Protocol " + p + " is not supported.");
      }
      if (p.equals("SSLv2")) {
        sslv2 = true;
      } else if (p.equals("SSLv3")) {
        sslv3 = true;
      } else if (p.equals("TLSv1")) {
        tlsv1 = true;
      } else if (p.equals("TLSv1.1")) {
        tlsv1_1 = true;
      } else if (p.equals("TLSv1.2")) {
        tlsv1_2 = true;
      }
    }
    SSL.setOptions(this.ssl, 4095);
    if (!sslv2) {
      SSL.setOptions(this.ssl, 16777216);
    }
    if (!sslv3) {
      SSL.setOptions(this.ssl, 33554432);
    }
    if (!tlsv1) {
      SSL.setOptions(this.ssl, 67108864);
    }
    if (!tlsv1_1) {
      SSL.setOptions(this.ssl, 134217728);
    }
    if (!tlsv1_2) {
      SSL.setOptions(this.ssl, 268435456);
    }
  }
  
  private Certificate[] initPeerCertChain()
    throws SSLPeerUnverifiedException
  {
    byte[][] chain = SSL.getPeerCertChain(this.ssl);
    byte[] clientCert;
    byte[] clientCert;
    if (!this.clientMode) {
      clientCert = SSL.getPeerCertificate(this.ssl);
    } else {
      clientCert = null;
    }
    if ((chain == null) && (clientCert == null)) {
      throw new SSLPeerUnverifiedException("peer not verified");
    }
    int len = 0;
    if (chain != null) {
      len += chain.length;
    }
    int i = 0;
    Certificate[] peerCerts;
    if (clientCert != null)
    {
      len++;
      Certificate[] peerCerts = new Certificate[len];
      peerCerts[(i++)] = new OpenSslX509Certificate(clientCert);
    }
    else
    {
      peerCerts = new Certificate[len];
    }
    if (chain != null)
    {
      int a = 0;
      for (; i < peerCerts.length; i++) {
        peerCerts[i] = new OpenSslX509Certificate(chain[(a++)]);
      }
    }
    return peerCerts;
  }
  
  public SSLSession getSession()
  {
    SSLSession session = this.session;
    if (session == null)
    {
      session = new SSLSession()
      {
        private javax.security.cert.X509Certificate[] x509PeerCerts;
        private Map<String, Object> values;
        
        public byte[] getId()
        {
          byte[] id = SSL.getSessionId(OpenSslEngine.this.ssl);
          if (id == null) {
            throw new IllegalStateException("SSL session ID not available");
          }
          return id;
        }
        
        public SSLSessionContext getSessionContext()
        {
          return OpenSslEngine.this.sessionContext;
        }
        
        public long getCreationTime()
        {
          return SSL.getTime(OpenSslEngine.this.ssl) * 1000L;
        }
        
        public long getLastAccessedTime()
        {
          return getCreationTime();
        }
        
        public void invalidate() {}
        
        public boolean isValid()
        {
          return false;
        }
        
        public void putValue(String name, Object value)
        {
          if (name == null) {
            throw new NullPointerException("name");
          }
          if (value == null) {
            throw new NullPointerException("value");
          }
          Map<String, Object> values = this.values;
          if (values == null) {
            values = this.values = new HashMap(2);
          }
          Object old = values.put(name, value);
          if ((value instanceof SSLSessionBindingListener)) {
            ((SSLSessionBindingListener)value).valueBound(new SSLSessionBindingEvent(this, name));
          }
          notifyUnbound(old, name);
        }
        
        public Object getValue(String name)
        {
          if (name == null) {
            throw new NullPointerException("name");
          }
          if (this.values == null) {
            return null;
          }
          return this.values.get(name);
        }
        
        public void removeValue(String name)
        {
          if (name == null) {
            throw new NullPointerException("name");
          }
          Map<String, Object> values = this.values;
          if (values == null) {
            return;
          }
          Object old = values.remove(name);
          notifyUnbound(old, name);
        }
        
        public String[] getValueNames()
        {
          Map<String, Object> values = this.values;
          if ((values == null) || (values.isEmpty())) {
            return EmptyArrays.EMPTY_STRINGS;
          }
          return (String[])values.keySet().toArray(new String[values.size()]);
        }
        
        private void notifyUnbound(Object value, String name)
        {
          if ((value instanceof SSLSessionBindingListener)) {
            ((SSLSessionBindingListener)value).valueUnbound(new SSLSessionBindingEvent(this, name));
          }
        }
        
        public Certificate[] getPeerCertificates()
          throws SSLPeerUnverifiedException
        {
          Certificate[] c = OpenSslEngine.this.peerCerts;
          if (c == null)
          {
            if (SSL.isInInit(OpenSslEngine.this.ssl) != 0) {
              throw new SSLPeerUnverifiedException("peer not verified");
            }
            c = OpenSslEngine.this.peerCerts = OpenSslEngine.this.initPeerCertChain();
          }
          return c;
        }
        
        public Certificate[] getLocalCertificates()
        {
          return OpenSslEngine.EMPTY_CERTIFICATES;
        }
        
        public javax.security.cert.X509Certificate[] getPeerCertificateChain()
          throws SSLPeerUnverifiedException
        {
          javax.security.cert.X509Certificate[] c = this.x509PeerCerts;
          if (c == null)
          {
            if (SSL.isInInit(OpenSslEngine.this.ssl) != 0) {
              throw new SSLPeerUnverifiedException("peer not verified");
            }
            byte[][] chain = SSL.getPeerCertChain(OpenSslEngine.this.ssl);
            if (chain == null) {
              throw new SSLPeerUnverifiedException("peer not verified");
            }
            javax.security.cert.X509Certificate[] peerCerts = new javax.security.cert.X509Certificate[chain.length];
            for (int i = 0; i < peerCerts.length; i++) {
              try
              {
                peerCerts[i] = javax.security.cert.X509Certificate.getInstance(chain[i]);
              }
              catch (CertificateException e)
              {
                throw new IllegalStateException(e);
              }
            }
            c = this.x509PeerCerts = peerCerts;
          }
          return c;
        }
        
        public Principal getPeerPrincipal()
          throws SSLPeerUnverifiedException
        {
          Certificate[] peer = getPeerCertificates();
          if ((peer == null) || (peer.length == 0)) {
            return null;
          }
          return principal(peer);
        }
        
        public Principal getLocalPrincipal()
        {
          Certificate[] local = getLocalCertificates();
          if ((local == null) || (local.length == 0)) {
            return null;
          }
          return principal(local);
        }
        
        private Principal principal(Certificate[] certs)
        {
          return ((java.security.cert.X509Certificate)certs[0]).getIssuerX500Principal();
        }
        
        public String getCipherSuite()
        {
          if (!OpenSslEngine.this.handshakeFinished) {
            return "SSL_NULL_WITH_NULL_NULL";
          }
          if (OpenSslEngine.this.cipher == null)
          {
            String c = OpenSslEngine.this.toJavaCipherSuite(SSL.getCipherForSSL(OpenSslEngine.this.ssl));
            if (c != null) {
              OpenSslEngine.this.cipher = c;
            }
          }
          return OpenSslEngine.this.cipher;
        }
        
        public String getProtocol()
        {
          String applicationProtocol = OpenSslEngine.this.applicationProtocol;
          if (applicationProtocol == null)
          {
            applicationProtocol = SSL.getNextProtoNegotiated(OpenSslEngine.this.ssl);
            if (applicationProtocol == null) {
              applicationProtocol = OpenSslEngine.this.fallbackApplicationProtocol;
            }
            if (applicationProtocol != null) {
              OpenSslEngine.this.applicationProtocol = applicationProtocol.replace(':', '_');
            } else {
              OpenSslEngine.this.applicationProtocol = (applicationProtocol = "");
            }
          }
          String version = SSL.getVersion(OpenSslEngine.this.ssl);
          if (applicationProtocol.isEmpty()) {
            return version;
          }
          return version + ':' + applicationProtocol;
        }
        
        public String getPeerHost()
        {
          return null;
        }
        
        public int getPeerPort()
        {
          return 0;
        }
        
        public int getPacketBufferSize()
        {
          return 18713;
        }
        
        public int getApplicationBufferSize()
        {
          return 16384;
        }
      };
      if (!SESSION_UPDATER.compareAndSet(this, null, session)) {
        session = this.session;
      }
    }
    return session;
  }
  
  public synchronized void beginHandshake()
    throws SSLException
  {
    if ((this.engineClosed) || (this.destroyed != 0)) {
      throw ENGINE_CLOSED;
    }
    switch (this.accepted)
    {
    case 0: 
      handshake();
      this.accepted = 2;
      break;
    case 1: 
      this.accepted = 2;
      break;
    case 2: 
      throw RENEGOTIATION_UNSUPPORTED;
    default: 
      throw new Error();
    }
  }
  
  private void beginHandshakeImplicitly()
    throws SSLException
  {
    if ((this.engineClosed) || (this.destroyed != 0)) {
      throw ENGINE_CLOSED;
    }
    if (this.accepted == 0)
    {
      handshake();
      this.accepted = 1;
    }
  }
  
  private void handshake()
    throws SSLException
  {
    int code = SSL.doHandshake(this.ssl);
    if (code <= 0)
    {
      long error = SSL.getLastErrorNumber();
      if (OpenSsl.isError(error))
      {
        String err = SSL.getErrorString(error);
        if (logger.isDebugEnabled()) {
          logger.debug("SSL_do_handshake failed: OpenSSL error: '" + err + '\'');
        }
        shutdown();
        throw new SSLException(err);
      }
    }
    else
    {
      this.handshakeFinished = true;
    }
  }
  
  private static long memoryAddress(ByteBuf buf)
  {
    if (buf.hasMemoryAddress()) {
      return buf.memoryAddress();
    }
    return Buffer.address(buf.nioBuffer());
  }
  
  private SSLEngineResult.Status getEngineStatus()
  {
    return this.engineClosed ? SSLEngineResult.Status.CLOSED : SSLEngineResult.Status.OK;
  }
  
  public synchronized SSLEngineResult.HandshakeStatus getHandshakeStatus()
  {
    if ((this.accepted == 0) || (this.destroyed != 0)) {
      return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
    }
    if (!this.handshakeFinished)
    {
      if (SSL.pendingWrittenBytesInBIO(this.networkBIO) != 0) {
        return SSLEngineResult.HandshakeStatus.NEED_WRAP;
      }
      if (SSL.isInInit(this.ssl) == 0)
      {
        this.handshakeFinished = true;
        return SSLEngineResult.HandshakeStatus.FINISHED;
      }
      return SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
    }
    if (this.engineClosed)
    {
      if (SSL.pendingWrittenBytesInBIO(this.networkBIO) != 0) {
        return SSLEngineResult.HandshakeStatus.NEED_WRAP;
      }
      return SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
    }
    return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
  }
  
  private String toJavaCipherSuite(String openSslCipherSuite)
  {
    if (openSslCipherSuite == null) {
      return null;
    }
    String prefix = toJavaCipherSuitePrefix(SSL.getVersion(this.ssl));
    return CipherSuiteConverter.toJava(openSslCipherSuite, prefix);
  }
  
  private static String toJavaCipherSuitePrefix(String protocolVersion)
  {
    char c;
    char c;
    if ((protocolVersion == null) || (protocolVersion.length() == 0)) {
      c = '\000';
    } else {
      c = protocolVersion.charAt(0);
    }
    switch (c)
    {
    case 'T': 
      return "TLS";
    case 'S': 
      return "SSL";
    }
    return "UNKNOWN";
  }
  
  public void setUseClientMode(boolean clientMode)
  {
    if (clientMode != this.clientMode) {
      throw new UnsupportedOperationException();
    }
  }
  
  public boolean getUseClientMode()
  {
    return this.clientMode;
  }
  
  public void setNeedClientAuth(boolean b)
  {
    setClientAuth(b ? ClientAuthMode.REQUIRE : ClientAuthMode.NONE);
  }
  
  public boolean getNeedClientAuth()
  {
    return this.clientAuth == ClientAuthMode.REQUIRE;
  }
  
  public void setWantClientAuth(boolean b)
  {
    setClientAuth(b ? ClientAuthMode.OPTIONAL : ClientAuthMode.NONE);
  }
  
  public boolean getWantClientAuth()
  {
    return this.clientAuth == ClientAuthMode.OPTIONAL;
  }
  
  private void setClientAuth(ClientAuthMode mode)
  {
    if (this.clientMode) {
      return;
    }
    synchronized (this)
    {
      if (this.clientAuth == mode) {
        return;
      }
      switch (mode)
      {
      case NONE: 
        SSL.setVerify(this.ssl, 0, 10);
        break;
      case REQUIRE: 
        SSL.setVerify(this.ssl, 2, 10);
        break;
      case OPTIONAL: 
        SSL.setVerify(this.ssl, 1, 10);
      }
      this.clientAuth = mode;
    }
  }
  
  public void setEnableSessionCreation(boolean b)
  {
    if (b) {
      throw new UnsupportedOperationException();
    }
  }
  
  public boolean getEnableSessionCreation()
  {
    return false;
  }
  
  protected void finalize()
    throws Throwable
  {
    super.finalize();
    
    shutdown();
  }
}
