package org.spacehq.mc.protocol.data.game.values.world.effect;

public class RecordEffectData
  implements WorldEffectData
{
  private int recordId;
  
  public RecordEffectData(int recordId)
  {
    this.recordId = recordId;
  }
  
  public int getRecordId()
  {
    return this.recordId;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    RecordEffectData that = (RecordEffectData)o;
    if (this.recordId != that.recordId) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    return this.recordId;
  }
}
