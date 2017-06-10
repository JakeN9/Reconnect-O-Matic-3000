package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public final class NetUtil
{
  public static final Inet4Address LOCALHOST4;
  public static final Inet6Address LOCALHOST6;
  public static final InetAddress LOCALHOST;
  public static final NetworkInterface LOOPBACK_IF;
  public static final int SOMAXCONN;
  private static final int IPV6_WORD_COUNT = 8;
  private static final int IPV6_MAX_CHAR_COUNT = 39;
  private static final int IPV6_BYTE_COUNT = 16;
  private static final int IPV6_MAX_CHAR_BETWEEN_SEPARATOR = 4;
  private static final int IPV6_MIN_SEPARATORS = 2;
  private static final int IPV6_MAX_SEPARATORS = 8;
  private static final int IPV4_BYTE_COUNT = 4;
  private static final int IPV4_MAX_CHAR_BETWEEN_SEPARATOR = 3;
  private static final int IPV4_SEPARATORS = 3;
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(NetUtil.class);
  
  static
  {
    byte[] LOCALHOST4_BYTES = { Byte.MAX_VALUE, 0, 0, 1 };
    byte[] LOCALHOST6_BYTES = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
    
    Inet4Address localhost4 = null;
    try
    {
      localhost4 = (Inet4Address)InetAddress.getByAddress(LOCALHOST4_BYTES);
    }
    catch (Exception e)
    {
      PlatformDependent.throwException(e);
    }
    LOCALHOST4 = localhost4;
    
    Inet6Address localhost6 = null;
    try
    {
      localhost6 = (Inet6Address)InetAddress.getByAddress(LOCALHOST6_BYTES);
    }
    catch (Exception e)
    {
      PlatformDependent.throwException(e);
    }
    LOCALHOST6 = localhost6;
    
    List<NetworkInterface> ifaces = new ArrayList();
    try
    {
      for (i = NetworkInterface.getNetworkInterfaces(); i.hasMoreElements();)
      {
        NetworkInterface iface = (NetworkInterface)i.nextElement();
        if (iface.getInetAddresses().hasMoreElements()) {
          ifaces.add(iface);
        }
      }
    }
    catch (SocketException e)
    {
      Enumeration<NetworkInterface> i;
      logger.warn("Failed to retrieve the list of available network interfaces", e);
    }
    NetworkInterface loopbackIface = null;
    InetAddress loopbackAddr = null;
    for (Iterator i$ = ifaces.iterator(); i$.hasNext();)
    {
      iface = (NetworkInterface)i$.next();
      for (i = iface.getInetAddresses(); i.hasMoreElements();)
      {
        InetAddress addr = (InetAddress)i.nextElement();
        if (addr.isLoopbackAddress())
        {
          loopbackIface = iface;
          loopbackAddr = addr;
          break label325;
        }
      }
    }
    NetworkInterface iface;
    Enumeration<InetAddress> i;
    label325:
    if (loopbackIface == null) {
      try
      {
        for (NetworkInterface iface : ifaces) {
          if (iface.isLoopback())
          {
            Enumeration<InetAddress> i = iface.getInetAddresses();
            if (i.hasMoreElements())
            {
              loopbackIface = iface;
              loopbackAddr = (InetAddress)i.nextElement();
              break;
            }
          }
        }
        if (loopbackIface == null) {
          logger.warn("Failed to find the loopback interface");
        }
      }
      catch (SocketException e)
      {
        logger.warn("Failed to find the loopback interface", e);
      }
    }
    if (loopbackIface != null) {
      logger.debug("Loopback interface: {} ({}, {})", new Object[] { loopbackIface.getName(), loopbackIface.getDisplayName(), loopbackAddr.getHostAddress() });
    } else if (loopbackAddr == null) {
      try
      {
        if (NetworkInterface.getByInetAddress(LOCALHOST6) != null)
        {
          logger.debug("Using hard-coded IPv6 localhost address: {}", localhost6);
          loopbackAddr = localhost6;
        }
      }
      catch (Exception e) {}finally
      {
        if (loopbackAddr == null)
        {
          logger.debug("Using hard-coded IPv4 localhost address: {}", localhost4);
          loopbackAddr = localhost4;
        }
      }
    }
    LOOPBACK_IF = loopbackIface;
    LOCALHOST = loopbackAddr;
    
    int somaxconn = PlatformDependent.isWindows() ? 200 : 128;
    File file = new File("/proc/sys/net/core/somaxconn");
    if (file.exists())
    {
      BufferedReader in = null;
      try
      {
        in = new BufferedReader(new FileReader(file));
        somaxconn = Integer.parseInt(in.readLine());
        if (logger.isDebugEnabled()) {
          logger.debug("{}: {}", file, Integer.valueOf(somaxconn));
        }
      }
      catch (Exception e)
      {
        logger.debug("Failed to get SOMAXCONN from: {}", file, e);
      }
      finally
      {
        if (in != null) {
          try
          {
            in.close();
          }
          catch (Exception e) {}
        }
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("{}: {} (non-existent)", file, Integer.valueOf(somaxconn));
    }
    SOMAXCONN = somaxconn;
  }
  
  public static byte[] createByteArrayFromIpAddressString(String ipAddressString)
  {
    if (isValidIpV4Address(ipAddressString))
    {
      StringTokenizer tokenizer = new StringTokenizer(ipAddressString, ".");
      
      byte[] byteAddress = new byte[4];
      for (int i = 0; i < 4; i++)
      {
        String token = tokenizer.nextToken();
        int tempInt = Integer.parseInt(token);
        byteAddress[i] = ((byte)tempInt);
      }
      return byteAddress;
    }
    if (isValidIpV6Address(ipAddressString))
    {
      if (ipAddressString.charAt(0) == '[') {
        ipAddressString = ipAddressString.substring(1, ipAddressString.length() - 1);
      }
      int percentPos = ipAddressString.indexOf('%');
      if (percentPos >= 0) {
        ipAddressString = ipAddressString.substring(0, percentPos);
      }
      StringTokenizer tokenizer = new StringTokenizer(ipAddressString, ":.", true);
      ArrayList<String> hexStrings = new ArrayList();
      ArrayList<String> decStrings = new ArrayList();
      String token = "";
      String prevToken = "";
      int doubleColonIndex = -1;
      while (tokenizer.hasMoreTokens())
      {
        prevToken = token;
        token = tokenizer.nextToken();
        if (":".equals(token))
        {
          if (":".equals(prevToken)) {
            doubleColonIndex = hexStrings.size();
          } else if (!prevToken.isEmpty()) {
            hexStrings.add(prevToken);
          }
        }
        else if (".".equals(token)) {
          decStrings.add(prevToken);
        }
      }
      if (":".equals(prevToken))
      {
        if (":".equals(token)) {
          doubleColonIndex = hexStrings.size();
        } else {
          hexStrings.add(token);
        }
      }
      else if (".".equals(prevToken)) {
        decStrings.add(token);
      }
      int hexStringsLength = 8;
      if (!decStrings.isEmpty()) {
        hexStringsLength -= 2;
      }
      if (doubleColonIndex != -1)
      {
        int numberToInsert = hexStringsLength - hexStrings.size();
        for (int i = 0; i < numberToInsert; i++) {
          hexStrings.add(doubleColonIndex, "0");
        }
      }
      byte[] ipByteArray = new byte[16];
      for (int i = 0; i < hexStrings.size(); i++) {
        convertToBytes((String)hexStrings.get(i), ipByteArray, i << 1);
      }
      for (int i = 0; i < decStrings.size(); i++) {
        ipByteArray[(i + 12)] = ((byte)(Integer.parseInt((String)decStrings.get(i)) & 0xFF));
      }
      return ipByteArray;
    }
    return null;
  }
  
  private static void convertToBytes(String hexWord, byte[] ipByteArray, int byteIndex)
  {
    int hexWordLength = hexWord.length();
    int hexWordIndex = 0;
    ipByteArray[byteIndex] = 0;
    ipByteArray[(byteIndex + 1)] = 0;
    if (hexWordLength > 3)
    {
      int charValue = getIntValue(hexWord.charAt(hexWordIndex++)); int 
        tmp39_38 = byteIndex; byte[] tmp39_37 = ipByteArray;tmp39_37[tmp39_38] = ((byte)(tmp39_37[tmp39_38] | charValue << 4));
    }
    if (hexWordLength > 2)
    {
      int charValue = getIntValue(hexWord.charAt(hexWordIndex++)); int 
        tmp69_68 = byteIndex; byte[] tmp69_67 = ipByteArray;tmp69_67[tmp69_68] = ((byte)(tmp69_67[tmp69_68] | charValue));
    }
    if (hexWordLength > 1)
    {
      int charValue = getIntValue(hexWord.charAt(hexWordIndex++)); int 
        tmp99_98 = (byteIndex + 1); byte[] tmp99_95 = ipByteArray;tmp99_95[tmp99_98] = ((byte)(tmp99_95[tmp99_98] | charValue << 4));
    }
    int charValue = getIntValue(hexWord.charAt(hexWordIndex)); int 
      tmp123_122 = (byteIndex + 1); byte[] tmp123_119 = ipByteArray;tmp123_119[tmp123_122] = ((byte)(tmp123_119[tmp123_122] | charValue & 0xF));
  }
  
  private static int getIntValue(char c)
  {
    switch (c)
    {
    case '0': 
      return 0;
    case '1': 
      return 1;
    case '2': 
      return 2;
    case '3': 
      return 3;
    case '4': 
      return 4;
    case '5': 
      return 5;
    case '6': 
      return 6;
    case '7': 
      return 7;
    case '8': 
      return 8;
    case '9': 
      return 9;
    }
    c = Character.toLowerCase(c);
    switch (c)
    {
    case 'a': 
      return 10;
    case 'b': 
      return 11;
    case 'c': 
      return 12;
    case 'd': 
      return 13;
    case 'e': 
      return 14;
    case 'f': 
      return 15;
    }
    return 0;
  }
  
  public static String intToIpAddress(int i)
  {
    StringBuilder buf = new StringBuilder(15);
    buf.append(i >> 24 & 0xFF);
    buf.append('.');
    buf.append(i >> 16 & 0xFF);
    buf.append('.');
    buf.append(i >> 8 & 0xFF);
    buf.append('.');
    buf.append(i & 0xFF);
    return buf.toString();
  }
  
  public static String bytesToIpAddress(byte[] bytes, int offset, int length)
  {
    if (length == 4)
    {
      StringBuilder buf = new StringBuilder(15);
      
      buf.append(bytes[(offset++)] >> 24 & 0xFF);
      buf.append('.');
      buf.append(bytes[(offset++)] >> 16 & 0xFF);
      buf.append('.');
      buf.append(bytes[(offset++)] >> 8 & 0xFF);
      buf.append('.');
      buf.append(bytes[offset] & 0xFF);
      
      return buf.toString();
    }
    if (length == 16)
    {
      StringBuilder sb = new StringBuilder(39);
      int endOffset = offset + 14;
      for (; offset < endOffset; offset += 2)
      {
        StringUtil.toHexString(sb, bytes, offset, 2);
        sb.append(':');
      }
      StringUtil.toHexString(sb, bytes, offset, 2);
      
      return sb.toString();
    }
    throw new IllegalArgumentException("length: " + length + " (expected: 4 or 16)");
  }
  
  public static boolean isValidIpV6Address(String ipAddress)
  {
    int length = ipAddress.length();
    boolean doubleColon = false;
    int numberOfColons = 0;
    int numberOfPeriods = 0;
    StringBuilder word = new StringBuilder();
    char c = '\000';
    
    int startOffset = 0;
    int endOffset = ipAddress.length();
    if (endOffset < 2) {
      return false;
    }
    if (ipAddress.charAt(0) == '[')
    {
      if (ipAddress.charAt(endOffset - 1) != ']') {
        return false;
      }
      startOffset = 1;
      endOffset--;
    }
    int percentIdx = ipAddress.indexOf('%', startOffset);
    if (percentIdx >= 0) {
      endOffset = percentIdx;
    }
    for (int i = startOffset; i < endOffset; i++)
    {
      char prevChar = c;
      c = ipAddress.charAt(i);
      switch (c)
      {
      case '.': 
        numberOfPeriods++;
        if (numberOfPeriods > 3) {
          return false;
        }
        if (!isValidIp4Word(word.toString())) {
          return false;
        }
        if ((numberOfColons != 6) && (!doubleColon)) {
          return false;
        }
        if ((numberOfColons == 7) && (ipAddress.charAt(startOffset) != ':') && (ipAddress.charAt(1 + startOffset) != ':')) {
          return false;
        }
        word.delete(0, word.length());
        break;
      case ':': 
        if ((i == startOffset) && ((ipAddress.length() <= i) || (ipAddress.charAt(i + 1) != ':'))) {
          return false;
        }
        numberOfColons++;
        if (numberOfColons > 7) {
          return false;
        }
        if (numberOfPeriods > 0) {
          return false;
        }
        if (prevChar == ':')
        {
          if (doubleColon) {
            return false;
          }
          doubleColon = true;
        }
        word.delete(0, word.length());
        break;
      default: 
        if ((word != null) && (word.length() > 3)) {
          return false;
        }
        if (!isValidHexChar(c)) {
          return false;
        }
        word.append(c);
      }
    }
    if (numberOfPeriods > 0)
    {
      if ((numberOfPeriods != 3) || (!isValidIp4Word(word.toString())) || (numberOfColons >= 7)) {
        return false;
      }
    }
    else
    {
      if ((numberOfColons != 7) && (!doubleColon)) {
        return false;
      }
      if ((word.length() == 0) && (ipAddress.charAt(length - 1 - startOffset) == ':') && (ipAddress.charAt(length - 2 - startOffset) != ':')) {
        return false;
      }
    }
    return true;
  }
  
  private static boolean isValidIp4Word(String word)
  {
    if ((word.length() < 1) || (word.length() > 3)) {
      return false;
    }
    for (int i = 0; i < word.length(); i++)
    {
      char c = word.charAt(i);
      if ((c < '0') || (c > '9')) {
        return false;
      }
    }
    return Integer.parseInt(word) <= 255;
  }
  
  private static boolean isValidHexChar(char c)
  {
    return ((c >= '0') && (c <= '9')) || ((c >= 'A') && (c <= 'F')) || ((c >= 'a') && (c <= 'f'));
  }
  
  private static boolean isValidNumericChar(char c)
  {
    return (c >= '0') && (c <= '9');
  }
  
  public static boolean isValidIpV4Address(String value)
  {
    int periods = 0;
    
    int length = value.length();
    if (length > 15) {
      return false;
    }
    StringBuilder word = new StringBuilder();
    for (int i = 0; i < length; i++)
    {
      char c = value.charAt(i);
      if (c == '.')
      {
        periods++;
        if (periods > 3) {
          return false;
        }
        if (word.length() == 0) {
          return false;
        }
        if (Integer.parseInt(word.toString()) > 255) {
          return false;
        }
        word.delete(0, word.length());
      }
      else
      {
        if (!Character.isDigit(c)) {
          return false;
        }
        if (word.length() > 2) {
          return false;
        }
        word.append(c);
      }
    }
    if ((word.length() == 0) || (Integer.parseInt(word.toString()) > 255)) {
      return false;
    }
    return periods == 3;
  }
  
  public static Inet6Address getByName(CharSequence ip)
  {
    return getByName(ip, true);
  }
  
  public static Inet6Address getByName(CharSequence ip, boolean ipv4Mapped)
  {
    byte[] bytes = new byte[16];
    int ipLength = ip.length();
    int compressBegin = 0;
    int compressLength = 0;
    int currentIndex = 0;
    int value = 0;
    int begin = -1;
    int i = 0;
    int ipv6Seperators = 0;
    int ipv4Seperators = 0;
    
    boolean needsShift = false;
    int tmp;
    for (; i < ipLength; i++)
    {
      char c = ip.charAt(i);
      switch (c)
      {
      case ':': 
        ipv6Seperators++;
        if ((i - begin > 4) || (ipv4Seperators > 0) || (ipv6Seperators > 8) || (currentIndex + 1 >= bytes.length)) {
          return null;
        }
        value <<= 4 - (i - begin) << 2;
        if (compressLength > 0) {
          compressLength -= 2;
        }
        bytes[(currentIndex++)] = ((byte)((value & 0xF) << 4 | value >> 4 & 0xF));
        bytes[(currentIndex++)] = ((byte)((value >> 8 & 0xF) << 4 | value >> 12 & 0xF));
        tmp = i + 1;
        if ((tmp < ipLength) && (ip.charAt(tmp) == ':'))
        {
          tmp++;
          if ((compressBegin != 0) || ((tmp < ipLength) && (ip.charAt(tmp) == ':'))) {
            return null;
          }
          ipv6Seperators++;
          needsShift = (ipv6Seperators == 2) && (value == 0);
          compressBegin = currentIndex;
          compressLength = bytes.length - compressBegin - 2;
          i++;
        }
        value = 0;
        begin = -1;
        break;
      case '.': 
        ipv4Seperators++;
        if ((i - begin > 3) || (ipv4Seperators > 3) || ((ipv6Seperators > 0) && (currentIndex + compressLength < 12)) || (i + 1 >= ipLength) || (currentIndex >= bytes.length) || (begin < 0) || ((begin == 0) && (((i == 3) && ((!isValidNumericChar(ip.charAt(2))) || (!isValidNumericChar(ip.charAt(1))) || (!isValidNumericChar(ip.charAt(0))))) || ((i == 2) && ((!isValidNumericChar(ip.charAt(1))) || (!isValidNumericChar(ip.charAt(0))))) || ((i == 1) && (!isValidNumericChar(ip.charAt(0))))))) {
          return null;
        }
        value <<= 3 - (i - begin) << 2;
        
        begin = (value & 0xF) * 100 + (value >> 4 & 0xF) * 10 + (value >> 8 & 0xF);
        if ((begin < 0) || (begin > 255)) {
          return null;
        }
        bytes[(currentIndex++)] = ((byte)begin);
        value = 0;
        begin = -1;
        break;
      default: 
        if ((!isValidHexChar(c)) || ((ipv4Seperators > 0) && (!isValidNumericChar(c)))) {
          return null;
        }
        if (begin < 0) {
          begin = i;
        } else if (i - begin > 4) {
          return null;
        }
        value += (getIntValue(c) << (i - begin << 2));
      }
    }
    boolean isCompressed = compressBegin > 0;
    if (ipv4Seperators > 0)
    {
      if (((begin > 0) && (i - begin > 3)) || (ipv4Seperators != 3) || (currentIndex >= bytes.length)) {
        return null;
      }
      if (ipv6Seperators == 0) {
        compressLength = 12;
      } else if ((ipv6Seperators >= 2) && (ip.charAt(ipLength - 1) != ':') && (((!isCompressed) && (ipv6Seperators == 6) && (ip.charAt(0) != ':')) || ((isCompressed) && (ipv6Seperators + 1 < 8) && ((ip.charAt(0) != ':') || (compressBegin <= 2))))) {
        compressLength -= 2;
      } else {
        return null;
      }
      value <<= 3 - (i - begin) << 2;
      
      begin = (value & 0xF) * 100 + (value >> 4 & 0xF) * 10 + (value >> 8 & 0xF);
      if ((begin < 0) || (begin > 255)) {
        return null;
      }
      bytes[(currentIndex++)] = ((byte)begin);
    }
    else
    {
      tmp = ipLength - 1;
      if (((begin > 0) && (i - begin > 4)) || (ipv6Seperators < 2) || ((!isCompressed) && ((ipv6Seperators + 1 != 8) || (ip.charAt(0) == ':') || (ip.charAt(tmp) == ':'))) || ((isCompressed) && ((ipv6Seperators > 8) || ((ipv6Seperators == 8) && (((compressBegin <= 2) && (ip.charAt(0) != ':')) || ((compressBegin >= 14) && (ip.charAt(tmp) != ':')))))) || (currentIndex + 1 >= bytes.length)) {
        return null;
      }
      if ((begin >= 0) && (i - begin <= 4)) {
        value <<= 4 - (i - begin) << 2;
      }
      bytes[(currentIndex++)] = ((byte)((value & 0xF) << 4 | value >> 4 & 0xF));
      bytes[(currentIndex++)] = ((byte)((value >> 8 & 0xF) << 4 | value >> 12 & 0xF));
    }
    i = currentIndex + compressLength;
    if ((needsShift) || (i >= bytes.length))
    {
      if (i >= bytes.length) {
        compressBegin++;
      }
      for (i = currentIndex; i < bytes.length;)
      {
        for (begin = bytes.length - 1; begin >= compressBegin; begin--) {
          bytes[begin] = bytes[(begin - 1)];
        }
        bytes[begin] = 0;
        compressBegin++;i++; continue;
        for (i = 0; i < compressLength; i++)
        {
          begin = i + compressBegin;
          currentIndex = begin + compressLength;
          if (currentIndex >= bytes.length) {
            break;
          }
          bytes[currentIndex] = bytes[begin];
          bytes[begin] = 0;
        }
      }
    }
    if ((ipv4Mapped) && (ipv4Seperators > 0) && (bytes[0] == 0) && (bytes[1] == 0) && (bytes[2] == 0) && (bytes[3] == 0) && (bytes[4] == 0) && (bytes[5] == 0) && (bytes[6] == 0) && (bytes[7] == 0) && (bytes[8] == 0) && (bytes[9] == 0)) {
      bytes[10] = (bytes[11] = -1);
    }
    try
    {
      return Inet6Address.getByAddress(null, bytes, -1);
    }
    catch (UnknownHostException e)
    {
      throw new RuntimeException(e);
    }
  }
  
  public static String toAddressString(InetAddress ip)
  {
    return toAddressString(ip, false);
  }
  
  public static String toAddressString(InetAddress ip, boolean ipv4Mapped)
  {
    if ((ip instanceof Inet4Address)) {
      return ip.getHostAddress();
    }
    if (!(ip instanceof Inet6Address)) {
      throw new IllegalArgumentException("Unhandled type: " + ip.getClass());
    }
    byte[] bytes = ip.getAddress();
    int[] words = new int[8];
    for (int i = 0; i < words.length; i++) {
      words[i] = ((bytes[(i << 1)] & 0xFF) << 8 | bytes[((i << 1) + 1)] & 0xFF);
    }
    int currentStart = -1;
    int currentLength = 0;
    int shortestStart = -1;
    int shortestLength = 0;
    for (i = 0; i < words.length; i++) {
      if (words[i] == 0)
      {
        if (currentStart < 0) {
          currentStart = i;
        }
      }
      else if (currentStart >= 0)
      {
        currentLength = i - currentStart;
        if (currentLength > shortestLength)
        {
          shortestStart = currentStart;
          shortestLength = currentLength;
        }
        currentStart = -1;
      }
    }
    if (currentStart >= 0)
    {
      currentLength = i - currentStart;
      if (currentLength > shortestLength)
      {
        shortestStart = currentStart;
        shortestLength = currentLength;
      }
    }
    if (shortestLength == 1)
    {
      shortestLength = 0;
      shortestStart = -1;
    }
    int shortestEnd = shortestStart + shortestLength;
    StringBuilder b = new StringBuilder(39);
    if (shortestEnd < 0)
    {
      b.append(Integer.toHexString(words[0]));
      for (i = 1; i < words.length; i++)
      {
        b.append(':');
        b.append(Integer.toHexString(words[i]));
      }
    }
    boolean isIpv4Mapped;
    boolean isIpv4Mapped;
    if (inRangeEndExclusive(0, shortestStart, shortestEnd))
    {
      b.append("::");
      isIpv4Mapped = (ipv4Mapped) && (shortestEnd == 5) && (words[5] == 65535);
    }
    else
    {
      b.append(Integer.toHexString(words[0]));
      isIpv4Mapped = false;
    }
    for (i = 1; i < words.length; i++) {
      if (!inRangeEndExclusive(i, shortestStart, shortestEnd))
      {
        if (!inRangeEndExclusive(i - 1, shortestStart, shortestEnd)) {
          if ((!isIpv4Mapped) || (i == 6)) {
            b.append(':');
          } else {
            b.append('.');
          }
        }
        if ((isIpv4Mapped) && (i > 5))
        {
          b.append(words[i] >> 8);
          b.append('.');
          b.append(words[i] & 0xFF);
        }
        else
        {
          b.append(Integer.toHexString(words[i]));
        }
      }
      else if (!inRangeEndExclusive(i - 1, shortestStart, shortestEnd))
      {
        b.append("::");
      }
    }
    return b.toString();
  }
  
  private static boolean inRangeEndExclusive(int value, int start, int end)
  {
    return (value >= start) && (value < end);
  }
}
