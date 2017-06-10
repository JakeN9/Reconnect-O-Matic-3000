package io.netty.handler.codec.mqtt;

import io.netty.util.CharsetUtil;

public enum MqttVersion
{
  MQTT_3_1("MQIsdp", (byte)3),  MQTT_3_1_1("MQTT", (byte)4);
  
  private String name;
  private byte level;
  
  private MqttVersion(String protocolName, byte protocolLevel)
  {
    this.name = protocolName;
    this.level = protocolLevel;
  }
  
  public String protocolName()
  {
    return this.name;
  }
  
  public byte[] protocolNameBytes()
  {
    return this.name.getBytes(CharsetUtil.UTF_8);
  }
  
  public byte protocolLevel()
  {
    return this.level;
  }
  
  public static MqttVersion fromProtocolNameAndLevel(String protocolName, byte protocolLevel)
  {
    for (MqttVersion mv : ) {
      if (mv.name.equals(protocolName))
      {
        if (mv.level == protocolLevel) {
          return mv;
        }
        throw new MqttUnacceptableProtocolVersionException(protocolName + " and " + protocolLevel + " are not match");
      }
    }
    throw new MqttUnacceptableProtocolVersionException(protocolName + "is unknown protocol name");
  }
}
