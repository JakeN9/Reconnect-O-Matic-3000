package io.netty.handler.codec.dns;

public final class DnsResponseCode
  implements Comparable<DnsResponseCode>
{
  public static final DnsResponseCode NOERROR = new DnsResponseCode(0, "no error");
  public static final DnsResponseCode FORMERROR = new DnsResponseCode(1, "format error");
  public static final DnsResponseCode SERVFAIL = new DnsResponseCode(2, "server failure");
  public static final DnsResponseCode NXDOMAIN = new DnsResponseCode(3, "name error");
  public static final DnsResponseCode NOTIMPL = new DnsResponseCode(4, "not implemented");
  public static final DnsResponseCode REFUSED = new DnsResponseCode(5, "operation refused");
  public static final DnsResponseCode YXDOMAIN = new DnsResponseCode(6, "domain name should not exist");
  public static final DnsResponseCode YXRRSET = new DnsResponseCode(7, "resource record set should not exist");
  public static final DnsResponseCode NXRRSET = new DnsResponseCode(8, "rrset does not exist");
  public static final DnsResponseCode NOTAUTH = new DnsResponseCode(9, "not authoritative for zone");
  public static final DnsResponseCode NOTZONE = new DnsResponseCode(10, "name not in zone");
  public static final DnsResponseCode BADVERS = new DnsResponseCode(11, "bad extension mechanism for version");
  public static final DnsResponseCode BADSIG = new DnsResponseCode(12, "bad signature");
  public static final DnsResponseCode BADKEY = new DnsResponseCode(13, "bad key");
  public static final DnsResponseCode BADTIME = new DnsResponseCode(14, "bad timestamp");
  private final int errorCode;
  private final String message;
  
  public static DnsResponseCode valueOf(int responseCode)
  {
    switch (responseCode)
    {
    case 0: 
      return NOERROR;
    case 1: 
      return FORMERROR;
    case 2: 
      return SERVFAIL;
    case 3: 
      return NXDOMAIN;
    case 4: 
      return NOTIMPL;
    case 5: 
      return REFUSED;
    case 6: 
      return YXDOMAIN;
    case 7: 
      return YXRRSET;
    case 8: 
      return NXRRSET;
    case 9: 
      return NOTAUTH;
    case 10: 
      return NOTZONE;
    case 11: 
      return BADVERS;
    case 12: 
      return BADSIG;
    case 13: 
      return BADKEY;
    case 14: 
      return BADTIME;
    }
    return new DnsResponseCode(responseCode, null);
  }
  
  public DnsResponseCode(int errorCode, String message)
  {
    this.errorCode = errorCode;
    this.message = message;
  }
  
  public int code()
  {
    return this.errorCode;
  }
  
  public int compareTo(DnsResponseCode o)
  {
    return code() - o.code();
  }
  
  public int hashCode()
  {
    return code();
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof DnsResponseCode)) {
      return false;
    }
    return code() == ((DnsResponseCode)o).code();
  }
  
  public String toString()
  {
    if (this.message == null) {
      return "DnsResponseCode(" + this.errorCode + ')';
    }
    return "DnsResponseCode(" + this.errorCode + ", " + this.message + ')';
  }
}
