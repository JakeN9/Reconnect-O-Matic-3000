package org.spacehq.packetlib.packet;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;

public abstract interface Packet
{
  public abstract void read(NetInput paramNetInput)
    throws IOException;
  
  public abstract void write(NetOutput paramNetOutput)
    throws IOException;
  
  public abstract boolean isPriority();
}
