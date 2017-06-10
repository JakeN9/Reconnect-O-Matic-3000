package io.netty.handler.codec.dns;

import io.netty.util.collection.IntObjectHashMap;
import java.util.HashMap;
import java.util.Map;

public final class DnsType
  implements Comparable<DnsType>
{
  public static final DnsType A = new DnsType(1, "A");
  public static final DnsType NS = new DnsType(2, "NS");
  public static final DnsType CNAME = new DnsType(5, "CNAME");
  public static final DnsType SOA = new DnsType(6, "SOA");
  public static final DnsType PTR = new DnsType(12, "PTR");
  public static final DnsType MX = new DnsType(15, "MX");
  public static final DnsType TXT = new DnsType(16, "TXT");
  public static final DnsType RP = new DnsType(17, "RP");
  public static final DnsType AFSDB = new DnsType(18, "AFSDB");
  public static final DnsType SIG = new DnsType(24, "SIG");
  public static final DnsType KEY = new DnsType(25, "KEY");
  public static final DnsType AAAA = new DnsType(28, "AAAA");
  public static final DnsType LOC = new DnsType(29, "LOC");
  public static final DnsType SRV = new DnsType(33, "SRV");
  public static final DnsType NAPTR = new DnsType(35, "NAPTR");
  public static final DnsType KX = new DnsType(36, "KX");
  public static final DnsType CERT = new DnsType(37, "CERT");
  public static final DnsType DNAME = new DnsType(39, "DNAME");
  public static final DnsType OPT = new DnsType(41, "OPT");
  public static final DnsType APL = new DnsType(42, "APL");
  public static final DnsType DS = new DnsType(43, "DS");
  public static final DnsType SSHFP = new DnsType(44, "SSHFP");
  public static final DnsType IPSECKEY = new DnsType(45, "IPSECKEY");
  public static final DnsType RRSIG = new DnsType(46, "RRSIG");
  public static final DnsType NSEC = new DnsType(47, "NSEC");
  public static final DnsType DNSKEY = new DnsType(48, "DNSKEY");
  public static final DnsType DHCID = new DnsType(49, "DHCID");
  public static final DnsType NSEC3 = new DnsType(50, "NSEC3");
  public static final DnsType NSEC3PARAM = new DnsType(51, "NSEC3PARAM");
  public static final DnsType TLSA = new DnsType(52, "TLSA");
  public static final DnsType HIP = new DnsType(55, "HIP");
  public static final DnsType SPF = new DnsType(99, "SPF");
  public static final DnsType TKEY = new DnsType(249, "TKEY");
  public static final DnsType TSIG = new DnsType(250, "TSIG");
  public static final DnsType IXFR = new DnsType(251, "IXFR");
  public static final DnsType AXFR = new DnsType(252, "AXFR");
  public static final DnsType ANY = new DnsType(255, "ANY");
  public static final DnsType CAA = new DnsType(257, "CAA");
  public static final DnsType TA = new DnsType(32768, "TA");
  public static final DnsType DLV = new DnsType(32769, "DLV");
  private static final Map<String, DnsType> BY_NAME = new HashMap();
  private static final IntObjectHashMap<DnsType> BY_TYPE = new IntObjectHashMap();
  private static final String EXPECTED;
  private final int intValue;
  private final String name;
  
  static
  {
    DnsType[] all = { A, NS, CNAME, SOA, PTR, MX, TXT, RP, AFSDB, SIG, KEY, AAAA, LOC, SRV, NAPTR, KX, CERT, DNAME, OPT, APL, DS, SSHFP, IPSECKEY, RRSIG, NSEC, DNSKEY, DHCID, NSEC3, NSEC3PARAM, TLSA, HIP, SPF, TKEY, TSIG, IXFR, AXFR, ANY, CAA, TA, DLV };
    
    StringBuilder expected = new StringBuilder(512);
    expected.append(" (expected: ");
    for (DnsType type : all)
    {
      BY_NAME.put(type.name(), type);
      BY_TYPE.put(type.intValue(), type);
      expected.append(type.name());
      expected.append('(');
      expected.append(type.intValue());
      expected.append("), ");
    }
    expected.setLength(expected.length() - 2);
    expected.append(')');
    EXPECTED = expected.toString();
  }
  
  public static DnsType valueOf(int intValue)
  {
    DnsType result = (DnsType)BY_TYPE.get(intValue);
    if (result == null) {
      return new DnsType(intValue, "UNKNOWN");
    }
    return result;
  }
  
  public static DnsType valueOf(String name)
  {
    DnsType result = (DnsType)BY_NAME.get(name);
    if (result == null) {
      throw new IllegalArgumentException("name: " + name + EXPECTED);
    }
    return result;
  }
  
  public static DnsType valueOf(int intValue, String name)
  {
    return new DnsType(intValue, name);
  }
  
  private DnsType(int intValue, String name)
  {
    if ((intValue & 0xFFFF) != intValue) {
      throw new IllegalArgumentException("intValue: " + intValue + " (expected: 0 ~ 65535)");
    }
    this.intValue = intValue;
    this.name = name;
  }
  
  public String name()
  {
    return this.name;
  }
  
  public int intValue()
  {
    return this.intValue;
  }
  
  public int hashCode()
  {
    return this.intValue;
  }
  
  public boolean equals(Object o)
  {
    return ((o instanceof DnsType)) && (((DnsType)o).intValue == this.intValue);
  }
  
  public int compareTo(DnsType o)
  {
    return intValue() - o.intValue();
  }
  
  public String toString()
  {
    return this.name;
  }
}
