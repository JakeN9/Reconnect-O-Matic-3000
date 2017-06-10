package org.spacehq.mc.protocol.data.game.values.window.property;

public enum EnchantmentTableProperty
  implements WindowProperty
{
  LEVEL_SLOT_1,  LEVEL_SLOT_2,  LEVEL_SLOT_3,  XP_SEED,  ENCHANTMENT_SLOT_1,  ENCHANTMENT_SLOT_2,  ENCHANTMENT_SLOT_3;
  
  private EnchantmentTableProperty() {}
  
  public static int getEnchantment(int type, int level)
  {
    return type | level << 8;
  }
  
  public static int getEnchantmentType(int enchantmentInfo)
  {
    return enchantmentInfo & 0xFF;
  }
  
  public static int getEnchantmentLevel(int enchantmentInfo)
  {
    return enchantmentInfo >> 8;
  }
}
