package io.netty.handler.codec.mqtt;

public class MqttUnsubAckMessage
  extends MqttMessage
{
  public MqttUnsubAckMessage(MqttFixedHeader mqttFixedHeader, MqttMessageIdVariableHeader variableHeader)
  {
    super(mqttFixedHeader, variableHeader, null);
  }
  
  public MqttMessageIdVariableHeader variableHeader()
  {
    return (MqttMessageIdVariableHeader)super.variableHeader();
  }
}
