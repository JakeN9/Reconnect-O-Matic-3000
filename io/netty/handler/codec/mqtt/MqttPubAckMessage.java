package io.netty.handler.codec.mqtt;

public class MqttPubAckMessage
  extends MqttMessage
{
  public MqttPubAckMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader variableHeader)
  {
    super(mqttFixedHeader, variableHeader);
  }
  
  public MqttMessageIdVariableHeader variableHeader()
  {
    return (MqttMessageIdVariableHeader)super.variableHeader();
  }
}
