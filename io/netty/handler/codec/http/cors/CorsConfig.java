package io.netty.handler.codec.http.cors;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.internal.StringUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

public final class CorsConfig
{
  private final Set<String> origins;
  private final boolean anyOrigin;
  private final boolean enabled;
  private final Set<String> exposeHeaders;
  private final boolean allowCredentials;
  private final long maxAge;
  private final Set<HttpMethod> allowedRequestMethods;
  private final Set<String> allowedRequestHeaders;
  private final boolean allowNullOrigin;
  private final Map<CharSequence, Callable<?>> preflightHeaders;
  private final boolean shortCurcuit;
  
  private CorsConfig(Builder builder)
  {
    this.origins = new LinkedHashSet(builder.origins);
    this.anyOrigin = builder.anyOrigin;
    this.enabled = builder.enabled;
    this.exposeHeaders = builder.exposeHeaders;
    this.allowCredentials = builder.allowCredentials;
    this.maxAge = builder.maxAge;
    this.allowedRequestMethods = builder.requestMethods;
    this.allowedRequestHeaders = builder.requestHeaders;
    this.allowNullOrigin = builder.allowNullOrigin;
    this.preflightHeaders = builder.preflightHeaders;
    this.shortCurcuit = builder.shortCurcuit;
  }
  
  public boolean isCorsSupportEnabled()
  {
    return this.enabled;
  }
  
  public boolean isAnyOriginSupported()
  {
    return this.anyOrigin;
  }
  
  public String origin()
  {
    return this.origins.isEmpty() ? "*" : (String)this.origins.iterator().next();
  }
  
  public Set<String> origins()
  {
    return this.origins;
  }
  
  public boolean isNullOriginAllowed()
  {
    return this.allowNullOrigin;
  }
  
  public Set<String> exposedHeaders()
  {
    return Collections.unmodifiableSet(this.exposeHeaders);
  }
  
  public boolean isCredentialsAllowed()
  {
    return this.allowCredentials;
  }
  
  public long maxAge()
  {
    return this.maxAge;
  }
  
  public Set<HttpMethod> allowedRequestMethods()
  {
    return Collections.unmodifiableSet(this.allowedRequestMethods);
  }
  
  public Set<String> allowedRequestHeaders()
  {
    return Collections.unmodifiableSet(this.allowedRequestHeaders);
  }
  
  public HttpHeaders preflightResponseHeaders()
  {
    if (this.preflightHeaders.isEmpty()) {
      return EmptyHttpHeaders.INSTANCE;
    }
    HttpHeaders preflightHeaders = new DefaultHttpHeaders();
    for (Map.Entry<CharSequence, Callable<?>> entry : this.preflightHeaders.entrySet())
    {
      Object value = getValue((Callable)entry.getValue());
      if ((value instanceof Iterable)) {
        preflightHeaders.addObject((CharSequence)entry.getKey(), (Iterable)value);
      } else {
        preflightHeaders.addObject((CharSequence)entry.getKey(), value);
      }
    }
    return preflightHeaders;
  }
  
  public boolean isShortCurcuit()
  {
    return this.shortCurcuit;
  }
  
  private static <T> T getValue(Callable<T> callable)
  {
    try
    {
      return (T)callable.call();
    }
    catch (Exception e)
    {
      throw new IllegalStateException("Could not generate value for callable [" + callable + ']', e);
    }
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + "[enabled=" + this.enabled + ", origins=" + this.origins + ", anyOrigin=" + this.anyOrigin + ", exposedHeaders=" + this.exposeHeaders + ", isCredentialsAllowed=" + this.allowCredentials + ", maxAge=" + this.maxAge + ", allowedRequestMethods=" + this.allowedRequestMethods + ", allowedRequestHeaders=" + this.allowedRequestHeaders + ", preflightHeaders=" + this.preflightHeaders + ']';
  }
  
  public static Builder withAnyOrigin()
  {
    return new Builder();
  }
  
  public static Builder withOrigin(String origin)
  {
    if ("*".equals(origin)) {
      return new Builder();
    }
    return new Builder(new String[] { origin });
  }
  
  public static Builder withOrigins(String... origins)
  {
    return new Builder(origins);
  }
  
  public static class Builder
  {
    private final Set<String> origins;
    private final boolean anyOrigin;
    private boolean allowNullOrigin;
    private boolean enabled = true;
    private boolean allowCredentials;
    private final Set<String> exposeHeaders = new HashSet();
    private long maxAge;
    private final Set<HttpMethod> requestMethods = new HashSet();
    private final Set<String> requestHeaders = new HashSet();
    private final Map<CharSequence, Callable<?>> preflightHeaders = new HashMap();
    private boolean noPreflightHeaders;
    private boolean shortCurcuit;
    
    public Builder(String... origins)
    {
      this.origins = new LinkedHashSet(Arrays.asList(origins));
      this.anyOrigin = false;
    }
    
    public Builder()
    {
      this.anyOrigin = true;
      this.origins = Collections.emptySet();
    }
    
    public Builder allowNullOrigin()
    {
      this.allowNullOrigin = true;
      return this;
    }
    
    public Builder disable()
    {
      this.enabled = false;
      return this;
    }
    
    public Builder exposeHeaders(String... headers)
    {
      this.exposeHeaders.addAll(Arrays.asList(headers));
      return this;
    }
    
    public Builder allowCredentials()
    {
      this.allowCredentials = true;
      return this;
    }
    
    public Builder maxAge(long max)
    {
      this.maxAge = max;
      return this;
    }
    
    public Builder allowedRequestMethods(HttpMethod... methods)
    {
      this.requestMethods.addAll(Arrays.asList(methods));
      return this;
    }
    
    public Builder allowedRequestHeaders(String... headers)
    {
      this.requestHeaders.addAll(Arrays.asList(headers));
      return this;
    }
    
    public Builder preflightResponseHeader(CharSequence name, Object... values)
    {
      if (values.length == 1) {
        this.preflightHeaders.put(name, new CorsConfig.ConstantValueGenerator(values[0], null));
      } else {
        preflightResponseHeader(name, Arrays.asList(values));
      }
      return this;
    }
    
    public <T> Builder preflightResponseHeader(CharSequence name, Iterable<T> value)
    {
      this.preflightHeaders.put(name, new CorsConfig.ConstantValueGenerator(value, null));
      return this;
    }
    
    public <T> Builder preflightResponseHeader(String name, Callable<T> valueGenerator)
    {
      this.preflightHeaders.put(name, valueGenerator);
      return this;
    }
    
    public Builder noPreflightResponseHeaders()
    {
      this.noPreflightHeaders = true;
      return this;
    }
    
    public CorsConfig build()
    {
      if ((this.preflightHeaders.isEmpty()) && (!this.noPreflightHeaders))
      {
        this.preflightHeaders.put(HttpHeaderNames.DATE, new CorsConfig.DateValueGenerator());
        this.preflightHeaders.put(HttpHeaderNames.CONTENT_LENGTH, new CorsConfig.ConstantValueGenerator("0", null));
      }
      return new CorsConfig(this, null);
    }
    
    public Builder shortCurcuit()
    {
      this.shortCurcuit = true;
      return this;
    }
  }
  
  private static final class ConstantValueGenerator
    implements Callable<Object>
  {
    private final Object value;
    
    private ConstantValueGenerator(Object value)
    {
      if (value == null) {
        throw new IllegalArgumentException("value must not be null");
      }
      this.value = value;
    }
    
    public Object call()
    {
      return this.value;
    }
  }
  
  public static final class DateValueGenerator
    implements Callable<Date>
  {
    public Date call()
      throws Exception
    {
      return new Date();
    }
  }
}
