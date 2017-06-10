package io.netty.channel;

import io.netty.buffer.ByteBufUtil;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.ThreadLocalRandom;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DefaultChannelId
  implements ChannelId
{
  private static final long serialVersionUID = 3884076183504074063L;
  private static final InternalLogger logger;
  private static final Pattern MACHINE_ID_PATTERN;
  private static final int MACHINE_ID_LEN = 8;
  private static final byte[] MACHINE_ID;
  private static final int PROCESS_ID_LEN = 4;
  private static final int MAX_PROCESS_ID = 4194304;
  private static final int PROCESS_ID;
  private static final int SEQUENCE_LEN = 4;
  private static final int TIMESTAMP_LEN = 8;
  private static final int RANDOM_LEN = 4;
  private static final AtomicInteger nextSequence;
  
  static ChannelId newInstance()
  {
    DefaultChannelId id = new DefaultChannelId();
    id.init();
    return id;
  }
  
  static
  {
    logger = InternalLoggerFactory.getInstance(DefaultChannelId.class);
    
    MACHINE_ID_PATTERN = Pattern.compile("^(?:[0-9a-fA-F][:-]?){6,8}$");
    
    nextSequence = new AtomicInteger();
    
    int processId = -1;
    String customProcessId = SystemPropertyUtil.get("io.netty.processId");
    if (customProcessId != null)
    {
      try
      {
        processId = Integer.parseInt(customProcessId);
      }
      catch (NumberFormatException e) {}
      if ((processId < 0) || (processId > 4194304))
      {
        processId = -1;
        logger.warn("-Dio.netty.processId: {} (malformed)", customProcessId);
      }
      else if (logger.isDebugEnabled())
      {
        logger.debug("-Dio.netty.processId: {} (user-set)", Integer.valueOf(processId));
      }
    }
    if (processId < 0)
    {
      processId = defaultProcessId();
      if (logger.isDebugEnabled()) {
        logger.debug("-Dio.netty.processId: {} (auto-detected)", Integer.valueOf(processId));
      }
    }
    PROCESS_ID = processId;
    
    byte[] machineId = null;
    String customMachineId = SystemPropertyUtil.get("io.netty.machineId");
    if (customMachineId != null) {
      if (MACHINE_ID_PATTERN.matcher(customMachineId).matches())
      {
        machineId = parseMachineId(customMachineId);
        logger.debug("-Dio.netty.machineId: {} (user-set)", customMachineId);
      }
      else
      {
        logger.warn("-Dio.netty.machineId: {} (malformed)", customMachineId);
      }
    }
    if (machineId == null)
    {
      machineId = defaultMachineId();
      if (logger.isDebugEnabled()) {
        logger.debug("-Dio.netty.machineId: {} (auto-detected)", formatAddress(machineId));
      }
    }
    MACHINE_ID = machineId;
  }
  
  private static byte[] parseMachineId(String value)
  {
    value = value.replaceAll("[:-]", "");
    
    byte[] machineId = new byte[8];
    for (int i = 0; i < value.length(); i += 2) {
      machineId[i] = ((byte)Integer.parseInt(value.substring(i, i + 2), 16));
    }
    return machineId;
  }
  
  private static byte[] defaultMachineId()
  {
    byte[] NOT_FOUND = { -1 };
    byte[] bestMacAddr = NOT_FOUND;
    InetAddress bestInetAddr = null;
    try
    {
      bestInetAddr = InetAddress.getByAddress(new byte[] { Byte.MAX_VALUE, 0, 0, 1 });
    }
    catch (UnknownHostException e)
    {
      PlatformDependent.throwException(e);
    }
    Map<NetworkInterface, InetAddress> ifaces = new LinkedHashMap();
    try
    {
      for (i = NetworkInterface.getNetworkInterfaces(); i.hasMoreElements();)
      {
        NetworkInterface iface = (NetworkInterface)i.nextElement();
        
        Enumeration<InetAddress> addrs = iface.getInetAddresses();
        if (addrs.hasMoreElements())
        {
          InetAddress a = (InetAddress)addrs.nextElement();
          if (!a.isLoopbackAddress()) {
            ifaces.put(iface, a);
          }
        }
      }
    }
    catch (SocketException e)
    {
      Enumeration<NetworkInterface> i;
      logger.warn("Failed to retrieve the list of available network interfaces", e);
    }
    for (Map.Entry<NetworkInterface, InetAddress> entry : ifaces.entrySet())
    {
      NetworkInterface iface = (NetworkInterface)entry.getKey();
      InetAddress inetAddr = (InetAddress)entry.getValue();
      if (!iface.isVirtual())
      {
        byte[] macAddr;
        try
        {
          macAddr = iface.getHardwareAddress();
        }
        catch (SocketException e)
        {
          logger.debug("Failed to get the hardware address of a network interface: {}", iface, e);
        }
        continue;
        
        boolean replace = false;
        int res = compareAddresses(bestMacAddr, macAddr);
        if (res < 0)
        {
          replace = true;
        }
        else if (res == 0)
        {
          res = compareAddresses(bestInetAddr, inetAddr);
          if (res < 0) {
            replace = true;
          } else if (res == 0) {
            if (bestMacAddr.length < macAddr.length) {
              replace = true;
            }
          }
        }
        if (replace)
        {
          bestMacAddr = macAddr;
          bestInetAddr = inetAddr;
        }
      }
    }
    if (bestMacAddr == NOT_FOUND)
    {
      bestMacAddr = new byte[8];
      ThreadLocalRandom.current().nextBytes(bestMacAddr);
      logger.warn("Failed to find a usable hardware address from the network interfaces; using random bytes: {}", formatAddress(bestMacAddr));
    }
    switch (bestMacAddr.length)
    {
    case 6: 
      byte[] newAddr = new byte[8];
      System.arraycopy(bestMacAddr, 0, newAddr, 0, 3);
      newAddr[3] = -1;
      newAddr[4] = -2;
      System.arraycopy(bestMacAddr, 3, newAddr, 5, 3);
      bestMacAddr = newAddr;
      break;
    default: 
      bestMacAddr = Arrays.copyOf(bestMacAddr, 8);
    }
    return bestMacAddr;
  }
  
  private static int compareAddresses(byte[] current, byte[] candidate)
  {
    if (candidate == null) {
      return 1;
    }
    if (candidate.length < 6) {
      return 1;
    }
    boolean onlyZeroAndOne = true;
    for (byte b : candidate) {
      if ((b != 0) && (b != 1))
      {
        onlyZeroAndOne = false;
        break;
      }
    }
    if (onlyZeroAndOne) {
      return 1;
    }
    if ((candidate[0] & 0x1) != 0) {
      return 1;
    }
    if ((current[0] & 0x2) == 0)
    {
      if ((candidate[0] & 0x2) == 0) {
        return 0;
      }
      return 1;
    }
    if ((candidate[0] & 0x2) == 0) {
      return -1;
    }
    return 0;
  }
  
  private static int compareAddresses(InetAddress current, InetAddress candidate)
  {
    return scoreAddress(current) - scoreAddress(candidate);
  }
  
  private static int scoreAddress(InetAddress addr)
  {
    if (addr.isAnyLocalAddress()) {
      return 0;
    }
    if (addr.isMulticastAddress()) {
      return 1;
    }
    if (addr.isLinkLocalAddress()) {
      return 2;
    }
    if (addr.isSiteLocalAddress()) {
      return 3;
    }
    return 4;
  }
  
  private static String formatAddress(byte[] addr)
  {
    StringBuilder buf = new StringBuilder(24);
    for (byte b : addr) {
      buf.append(String.format("%02x:", new Object[] { Integer.valueOf(b & 0xFF) }));
    }
    return buf.substring(0, buf.length() - 1);
  }
  
  private static int defaultProcessId()
  {
    ClassLoader loader = PlatformDependent.getSystemClassLoader();
    String value;
    try
    {
      Class<?> mgmtFactoryType = Class.forName("java.lang.management.ManagementFactory", true, loader);
      Class<?> runtimeMxBeanType = Class.forName("java.lang.management.RuntimeMXBean", true, loader);
      
      Method getRuntimeMXBean = mgmtFactoryType.getMethod("getRuntimeMXBean", EmptyArrays.EMPTY_CLASSES);
      Object bean = getRuntimeMXBean.invoke(null, EmptyArrays.EMPTY_OBJECTS);
      Method getName = runtimeMxBeanType.getDeclaredMethod("getName", EmptyArrays.EMPTY_CLASSES);
      value = (String)getName.invoke(bean, EmptyArrays.EMPTY_OBJECTS);
    }
    catch (Exception e)
    {
      logger.debug("Could not invoke ManagementFactory.getRuntimeMXBean().getName(); Android?", e);
      try
      {
        Class<?> processType = Class.forName("android.os.Process", true, loader);
        Method myPid = processType.getMethod("myPid", EmptyArrays.EMPTY_CLASSES);
        value = myPid.invoke(null, EmptyArrays.EMPTY_OBJECTS).toString();
      }
      catch (Exception e2)
      {
        logger.debug("Could not invoke Process.myPid(); not Android?", e2);
        value = "";
      }
    }
    int atIndex = value.indexOf('@');
    if (atIndex >= 0) {
      value = value.substring(0, atIndex);
    }
    int pid;
    try
    {
      pid = Integer.parseInt(value);
    }
    catch (NumberFormatException e)
    {
      pid = -1;
    }
    if ((pid < 0) || (pid > 4194304))
    {
      pid = ThreadLocalRandom.current().nextInt(4194305);
      logger.warn("Failed to find the current process ID from '{}'; using a random value: {}", value, Integer.valueOf(pid));
    }
    return pid;
  }
  
  private final byte[] data = new byte[28];
  private int hashCode;
  private transient String shortValue;
  private transient String longValue;
  
  private void init()
  {
    int i = 0;
    
    System.arraycopy(MACHINE_ID, 0, this.data, i, 8);
    i += 8;
    
    i = writeInt(i, PROCESS_ID);
    
    i = writeInt(i, nextSequence.getAndIncrement());
    
    i = writeLong(i, Long.reverse(System.nanoTime()) ^ System.currentTimeMillis());
    
    int random = ThreadLocalRandom.current().nextInt();
    this.hashCode = random;
    i = writeInt(i, random);
    
    assert (i == this.data.length);
  }
  
  private int writeInt(int i, int value)
  {
    this.data[(i++)] = ((byte)(value >>> 24));
    this.data[(i++)] = ((byte)(value >>> 16));
    this.data[(i++)] = ((byte)(value >>> 8));
    this.data[(i++)] = ((byte)value);
    return i;
  }
  
  private int writeLong(int i, long value)
  {
    this.data[(i++)] = ((byte)(int)(value >>> 56));
    this.data[(i++)] = ((byte)(int)(value >>> 48));
    this.data[(i++)] = ((byte)(int)(value >>> 40));
    this.data[(i++)] = ((byte)(int)(value >>> 32));
    this.data[(i++)] = ((byte)(int)(value >>> 24));
    this.data[(i++)] = ((byte)(int)(value >>> 16));
    this.data[(i++)] = ((byte)(int)(value >>> 8));
    this.data[(i++)] = ((byte)(int)value);
    return i;
  }
  
  public String asShortText()
  {
    String shortValue = this.shortValue;
    if (shortValue == null) {
      this.shortValue = (shortValue = ByteBufUtil.hexDump(this.data, 24, 4));
    }
    return shortValue;
  }
  
  public String asLongText()
  {
    String longValue = this.longValue;
    if (longValue == null) {
      this.longValue = (longValue = newLongValue());
    }
    return longValue;
  }
  
  private String newLongValue()
  {
    StringBuilder buf = new StringBuilder(2 * this.data.length + 5);
    int i = 0;
    i = appendHexDumpField(buf, i, 8);
    i = appendHexDumpField(buf, i, 4);
    i = appendHexDumpField(buf, i, 4);
    i = appendHexDumpField(buf, i, 8);
    i = appendHexDumpField(buf, i, 4);
    assert (i == this.data.length);
    return buf.substring(0, buf.length() - 1);
  }
  
  private int appendHexDumpField(StringBuilder buf, int i, int length)
  {
    buf.append(ByteBufUtil.hexDump(this.data, i, length));
    buf.append('-');
    i += length;
    return i;
  }
  
  public int hashCode()
  {
    return this.hashCode;
  }
  
  public int compareTo(ChannelId o)
  {
    return 0;
  }
  
  public boolean equals(Object obj)
  {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof DefaultChannelId)) {
      return false;
    }
    return Arrays.equals(this.data, ((DefaultChannelId)obj).data);
  }
  
  public String toString()
  {
    return asShortText();
  }
}
