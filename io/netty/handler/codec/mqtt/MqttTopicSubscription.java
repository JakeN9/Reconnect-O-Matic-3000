package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;

public class MqttTopicSubscription
{
  private final String topicFilter;
  private final MqttQoS qualityOfService;
  
  public MqttTopicSubscription(String topicFilter, MqttQoS qualityOfService)
  {
    this.topicFilter = topicFilter;
    this.qualityOfService = qualityOfService;
  }
  
  public String topicName()
  {
    return this.topicFilter;
  }
  
  public MqttQoS qualityOfService()
  {
    return this.qualityOfService;
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + '[' + "topicFilter=" + this.topicFilter + ", qualityOfService=" + this.qualityOfService + ']';
  }
}
