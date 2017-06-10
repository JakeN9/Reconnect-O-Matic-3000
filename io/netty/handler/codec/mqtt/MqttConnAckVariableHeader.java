package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;

public class MqttConnAckVariableHeader
{
  private final MqttConnectReturnCode connectReturnCode;
  
  public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode)
  {
    this.connectReturnCode = connectReturnCode;
  }
  
  public MqttConnectReturnCode connectReturnCode()
  {
    return this.connectReturnCode;
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + '[' + "connectReturnCode=" + this.connectReturnCode + ']';
  }
}
