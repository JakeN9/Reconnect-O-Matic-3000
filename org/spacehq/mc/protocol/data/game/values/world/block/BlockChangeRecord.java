package org.spacehq.mc.protocol.data.game.values.world.block;

import org.spacehq.mc.protocol.data.game.Position;

public class BlockChangeRecord
{
  private Position position;
  private int id;
  private int data;
  
  public BlockChangeRecord(Position position, int id, int data)
  {
    this.position = position;
    this.id = id;
    this.data = data;
  }
  
  public Position getPosition()
  {
    return this.position;
  }
  
  public int getId()
  {
    return this.id;
  }
  
  public int getData()
  {
    return this.data;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    BlockChangeRecord record = (BlockChangeRecord)o;
    if (this.id != record.id) {
      return false;
    }
    if (this.data != record.data) {
      return false;
    }
    if (!this.position.equals(record.position)) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = this.position.hashCode();
    result = 31 * result + this.id;
    result = 31 * result + this.data;
    return result;
  }
}
