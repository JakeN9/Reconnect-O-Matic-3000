package io.netty.handler.codec.mqtt;

import io.netty.util.internal.StringUtil;

public class MqttPublishVariableHeader
{
  private final String topicName;
  private final int messageId;
  
  public MqttPublishVariableHeader(String topicName, int messageId)
  {
    this.topicName = topicName;
    this.messageId = messageId;
  }
  
  public String topicName()
  {
    return this.topicName;
  }
  
  public int messageId()
  {
    return this.messageId;
  }
  
  public String toString()
  {
    return StringUtil.simpleClassName(this) + '[' + "topicName=" + this.topicName + ", messageId=" + this.messageId + ']';
  }
}
