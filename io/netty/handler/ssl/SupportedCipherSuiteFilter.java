package io.netty.handler.ssl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SupportedCipherSuiteFilter
  implements CipherSuiteFilter
{
  public static final SupportedCipherSuiteFilter INSTANCE = new SupportedCipherSuiteFilter();
  
  public String[] filterCipherSuites(Iterable<String> ciphers, List<String> defaultCiphers, Set<String> supportedCiphers)
  {
    if (defaultCiphers == null) {
      throw new NullPointerException("defaultCiphers");
    }
    if (supportedCiphers == null) {
      throw new NullPointerException("supportedCiphers");
    }
    List<String> newCiphers;
    if (ciphers == null)
    {
      List<String> newCiphers = new ArrayList(defaultCiphers.size());
      ciphers = defaultCiphers;
    }
    else
    {
      newCiphers = new ArrayList(supportedCiphers.size());
    }
    for (String c : ciphers)
    {
      if (c == null) {
        break;
      }
      if (supportedCiphers.contains(c)) {
        newCiphers.add(c);
      }
    }
    return (String[])newCiphers.toArray(new String[newCiphers.size()]);
  }
}
