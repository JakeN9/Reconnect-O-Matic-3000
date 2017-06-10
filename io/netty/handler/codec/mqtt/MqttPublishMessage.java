package io.netty.handler.codec.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.util.IllegalReferenceCountException;

public class MqttPublishMessage
  extends MqttMessage
  implements ByteBufHolder
{
  public MqttPublishMessage(MqttFixedHeader mqttFixedHeader, MqttPublishVariableHeader variableHeader, ByteBuf payload)
  {
    super(mqttFixedHeader, variableHeader, payload);
  }
  
  public MqttPublishVariableHeader variableHeader()
  {
    return (MqttPublishVariableHeader)super.variableHeader();
  }
  
  public ByteBuf payload()
  {
    return content();
  }
  
  public ByteBuf content()
  {
    ByteBuf data = (ByteBuf)super.payload();
    if (data.refCnt() <= 0) {
      throw new IllegalReferenceCountException(data.refCnt());
    }
    return data;
  }
  
  public MqttPublishMessage copy()
  {
    return new MqttPublishMessage(fixedHeader(), variableHeader(), content().copy());
  }
  
  public MqttPublishMessage duplicate()
  {
    return new MqttPublishMessage(fixedHeader(), variableHeader(), content().duplicate());
  }
  
  public int refCnt()
  {
    return content().refCnt();
  }
  
  public MqttPublishMessage retain()
  {
    content().retain();
    return this;
  }
  
  public MqttPublishMessage retain(int increment)
  {
    content().retain(increment);
    return this;
  }
  
  public MqttPublishMessage touch()
  {
    content().touch();
    return this;
  }
  
  public MqttPublishMessage touch(Object hint)
  {
    content().touch(hint);
    return this;
  }
  
  public boolean release()
  {
    return content().release();
  }
  
  public boolean release(int decrement)
  {
    return content().release(decrement);
  }
}
