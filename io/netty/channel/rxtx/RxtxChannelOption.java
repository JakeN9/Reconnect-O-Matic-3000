package io.netty.channel.rxtx;

import io.netty.channel.ChannelOption;

public final class RxtxChannelOption
{
  private static final Class<RxtxChannelOption> T = RxtxChannelOption.class;
  public static final ChannelOption<Integer> BAUD_RATE = ChannelOption.valueOf(T, "BAUD_RATE");
  public static final ChannelOption<Boolean> DTR = ChannelOption.valueOf(T, "DTR");
  public static final ChannelOption<Boolean> RTS = ChannelOption.valueOf(T, "RTS");
  public static final ChannelOption<RxtxChannelConfig.Stopbits> STOP_BITS = ChannelOption.valueOf(T, "STOP_BITS");
  public static final ChannelOption<RxtxChannelConfig.Databits> DATA_BITS = ChannelOption.valueOf(T, "DATA_BITS");
  public static final ChannelOption<RxtxChannelConfig.Paritybit> PARITY_BIT = ChannelOption.valueOf(T, "PARITY_BIT");
  public static final ChannelOption<Integer> WAIT_TIME = ChannelOption.valueOf(T, "WAIT_TIME");
  public static final ChannelOption<Integer> READ_TIMEOUT = ChannelOption.valueOf(T, "READ_TIMEOUT");
}
