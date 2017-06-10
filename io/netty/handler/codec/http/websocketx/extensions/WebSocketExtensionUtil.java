package io.netty.handler.codec.http.websocketx.extensions;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.util.internal.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WebSocketExtensionUtil
{
  private static final char EXTENSION_SEPARATOR = ',';
  private static final char PARAMETER_SEPARATOR = ';';
  private static final char PARAMETER_EQUAL = '=';
  private static final Pattern PARAMETER = Pattern.compile("^([^=]+)(=[\\\"]?([^\\\"]+)[\\\"]?)?$");
  
  static boolean isWebsocketUpgrade(HttpMessage httpMessage)
  {
    if (httpMessage == null) {
      throw new NullPointerException("httpMessage");
    }
    return (httpMessage.headers().contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) && (httpMessage.headers().contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true));
  }
  
  public static List<WebSocketExtensionData> extractExtensions(String extensionHeader)
  {
    String[] rawExtensions = StringUtil.split(extensionHeader, ',');
    if (rawExtensions.length > 0)
    {
      List<WebSocketExtensionData> extensions = new ArrayList(rawExtensions.length);
      for (String rawExtension : rawExtensions)
      {
        String[] extensionParameters = StringUtil.split(rawExtension, ';');
        String name = extensionParameters[0].trim();
        Map<String, String> parameters;
        if (extensionParameters.length > 1)
        {
          Map<String, String> parameters = new HashMap(extensionParameters.length - 1);
          for (int i = 1; i < extensionParameters.length; i++)
          {
            String parameter = extensionParameters[i].trim();
            Matcher parameterMatcher = PARAMETER.matcher(parameter);
            if ((parameterMatcher.matches()) && (parameterMatcher.group(1) != null)) {
              parameters.put(parameterMatcher.group(1), parameterMatcher.group(3));
            }
          }
        }
        else
        {
          parameters = Collections.emptyMap();
        }
        extensions.add(new WebSocketExtensionData(name, parameters));
      }
      return extensions;
    }
    return Collections.emptyList();
  }
  
  static String appendExtension(String currentHeaderValue, String extensionName, Map<String, String> extensionParameters)
  {
    StringBuilder newHeaderValue = new StringBuilder(currentHeaderValue != null ? currentHeaderValue.length() : 0 + extensionName.length() + 1);
    if ((currentHeaderValue != null) && (!currentHeaderValue.trim().isEmpty()))
    {
      newHeaderValue.append(currentHeaderValue);
      newHeaderValue.append(',');
    }
    newHeaderValue.append(extensionName);
    boolean isFirst = true;
    for (Map.Entry<String, String> extensionParameter : extensionParameters.entrySet())
    {
      if (isFirst) {
        newHeaderValue.append(';');
      } else {
        isFirst = false;
      }
      newHeaderValue.append((String)extensionParameter.getKey());
      if (extensionParameter.getValue() != null)
      {
        newHeaderValue.append('=');
        newHeaderValue.append((String)extensionParameter.getValue());
      }
    }
    return newHeaderValue.toString();
  }
}
