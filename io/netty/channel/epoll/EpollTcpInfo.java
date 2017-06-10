package io.netty.channel.epoll;

public final class EpollTcpInfo
{
  final int[] info = new int[32];
  
  public int state()
  {
    return this.info[0] & 0xFF;
  }
  
  public int caState()
  {
    return this.info[1] & 0xFF;
  }
  
  public int retransmits()
  {
    return this.info[2] & 0xFF;
  }
  
  public int probes()
  {
    return this.info[3] & 0xFF;
  }
  
  public int backoff()
  {
    return this.info[4] & 0xFF;
  }
  
  public int options()
  {
    return this.info[5] & 0xFF;
  }
  
  public int sndWscale()
  {
    return this.info[6] & 0xFF;
  }
  
  public int rcvWscale()
  {
    return this.info[7] & 0xFF;
  }
  
  public long rto()
  {
    return this.info[8] & 0xFFFFFFFF;
  }
  
  public long ato()
  {
    return this.info[9] & 0xFFFFFFFF;
  }
  
  public long sndMss()
  {
    return this.info[10] & 0xFFFFFFFF;
  }
  
  public long rcvMss()
  {
    return this.info[11] & 0xFFFFFFFF;
  }
  
  public long unacked()
  {
    return this.info[12] & 0xFFFFFFFF;
  }
  
  public long sacked()
  {
    return this.info[13] & 0xFFFFFFFF;
  }
  
  public long lost()
  {
    return this.info[14] & 0xFFFFFFFF;
  }
  
  public long retrans()
  {
    return this.info[15] & 0xFFFFFFFF;
  }
  
  public long fackets()
  {
    return this.info[16] & 0xFFFFFFFF;
  }
  
  public long lastDataSent()
  {
    return this.info[17] & 0xFFFFFFFF;
  }
  
  public long lastAckSent()
  {
    return this.info[18] & 0xFFFFFFFF;
  }
  
  public long lastDataRecv()
  {
    return this.info[19] & 0xFFFFFFFF;
  }
  
  public long lastAckRecv()
  {
    return this.info[20] & 0xFFFFFFFF;
  }
  
  public long pmtu()
  {
    return this.info[21] & 0xFFFFFFFF;
  }
  
  public long rcvSsthresh()
  {
    return this.info[22] & 0xFFFFFFFF;
  }
  
  public long rtt()
  {
    return this.info[23] & 0xFFFFFFFF;
  }
  
  public long rttvar()
  {
    return this.info[24] & 0xFFFFFFFF;
  }
  
  public long sndSsthresh()
  {
    return this.info[25] & 0xFFFFFFFF;
  }
  
  public long sndCwnd()
  {
    return this.info[26] & 0xFFFFFFFF;
  }
  
  public long advmss()
  {
    return this.info[27] & 0xFFFFFFFF;
  }
  
  public long reordering()
  {
    return this.info[28] & 0xFFFFFFFF;
  }
  
  public long rcvRtt()
  {
    return this.info[29] & 0xFFFFFFFF;
  }
  
  public long rcvSpace()
  {
    return this.info[30] & 0xFFFFFFFF;
  }
  
  public long totalRetrans()
  {
    return this.info[31] & 0xFFFFFFFF;
  }
}
