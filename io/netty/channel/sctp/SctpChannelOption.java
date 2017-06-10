package io.netty.channel.sctp;

import com.sun.nio.sctp.SctpStandardSocketOptions.InitMaxStreams;
import io.netty.channel.ChannelOption;
import java.net.SocketAddress;

public final class SctpChannelOption
{
  private static final Class<SctpChannelOption> T = SctpChannelOption.class;
  public static final ChannelOption<Boolean> SCTP_DISABLE_FRAGMENTS = ChannelOption.valueOf(T, "SCTP_DISABLE_FRAGMENTS");
  public static final ChannelOption<Boolean> SCTP_EXPLICIT_COMPLETE = ChannelOption.valueOf(T, "SCTP_EXPLICIT_COMPLETE");
  public static final ChannelOption<Integer> SCTP_FRAGMENT_INTERLEAVE = ChannelOption.valueOf(T, "SCTP_FRAGMENT_INTERLEAVE");
  public static final ChannelOption<SctpStandardSocketOptions.InitMaxStreams> SCTP_INIT_MAXSTREAMS = ChannelOption.valueOf(T, "SCTP_INIT_MAXSTREAMS");
  public static final ChannelOption<Boolean> SCTP_NODELAY = ChannelOption.valueOf(T, "SCTP_NODELAY");
  public static final ChannelOption<SocketAddress> SCTP_PRIMARY_ADDR = ChannelOption.valueOf(T, "SCTP_PRIMARY_ADDR");
  public static final ChannelOption<SocketAddress> SCTP_SET_PEER_PRIMARY_ADDR = ChannelOption.valueOf(T, "SCTP_SET_PEER_PRIMARY_ADDR");
}
