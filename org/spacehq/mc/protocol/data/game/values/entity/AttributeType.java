package org.spacehq.mc.protocol.data.game.values.entity;

public enum AttributeType
{
  MAX_HEALTH(20.0D, 0.0D, Double.MAX_VALUE),  FOLLOW_RANGE(32.0D, 0.0D, 2048.0D),  KNOCKBACK_RESISTANCE(0.0D, 0.0D, 1.0D),  MOVEMENT_SPEED(0.699999988079071D, 0.0D, Double.MAX_VALUE),  ATTACK_DAMAGE(2.0D, 0.0D, Double.MAX_VALUE),  HORSE_JUMP_STRENGTH(0.7D, 0.0D, 2.0D),  ZOMBIE_SPAWN_REINFORCEMENTS_CHANCE(0.0D, 0.0D, 1.0D);
  
  private double def;
  private double min;
  private double max;
  
  private AttributeType(double def, double min, double max)
  {
    this.def = def;
    this.min = min;
    this.max = max;
  }
  
  public double getDefault()
  {
    return this.def;
  }
  
  public double getMin()
  {
    return this.min;
  }
  
  public double getMax()
  {
    return this.max;
  }
}
