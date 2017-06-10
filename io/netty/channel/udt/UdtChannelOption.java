package io.netty.channel.udt;

import io.netty.channel.ChannelOption;

public final class UdtChannelOption
{
  private static final Class<UdtChannelOption> T = UdtChannelOption.class;
  public static final ChannelOption<Integer> PROTOCOL_RECEIVE_BUFFER_SIZE = ChannelOption.valueOf(T, "PROTOCOL_RECEIVE_BUFFER_SIZE");
  public static final ChannelOption<Integer> PROTOCOL_SEND_BUFFER_SIZE = ChannelOption.valueOf(T, "PROTOCOL_SEND_BUFFER_SIZE");
  public static final ChannelOption<Integer> SYSTEM_RECEIVE_BUFFER_SIZE = ChannelOption.valueOf(T, "SYSTEM_RECEIVE_BUFFER_SIZE");
  public static final ChannelOption<Integer> SYSTEM_SEND_BUFFER_SIZE = ChannelOption.valueOf(T, "SYSTEM_SEND_BUFFER_SIZE");
}
