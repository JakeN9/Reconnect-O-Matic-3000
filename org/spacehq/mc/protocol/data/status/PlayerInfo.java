package org.spacehq.mc.protocol.data.status;

import java.util.Arrays;
import org.spacehq.mc.auth.data.GameProfile;

public class PlayerInfo
{
  private int max;
  private int online;
  private GameProfile[] players;
  
  public PlayerInfo(int max, int online, GameProfile[] players)
  {
    this.max = max;
    this.online = online;
    this.players = players;
  }
  
  public int getMaxPlayers()
  {
    return this.max;
  }
  
  public int getOnlinePlayers()
  {
    return this.online;
  }
  
  public GameProfile[] getPlayers()
  {
    return this.players;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    PlayerInfo that = (PlayerInfo)o;
    if (this.max != that.max) {
      return false;
    }
    if (this.online != that.online) {
      return false;
    }
    if (!Arrays.equals(this.players, that.players)) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = this.max;
    result = 31 * result + this.online;
    result = 31 * result + Arrays.hashCode(this.players);
    return result;
  }
}
