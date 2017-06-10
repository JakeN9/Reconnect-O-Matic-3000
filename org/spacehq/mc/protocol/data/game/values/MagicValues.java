package org.spacehq.mc.protocol.data.game.values;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.spacehq.mc.protocol.data.game.values.entity.Art;
import org.spacehq.mc.protocol.data.game.values.entity.AttributeType;
import org.spacehq.mc.protocol.data.game.values.entity.Effect;
import org.spacehq.mc.protocol.data.game.values.entity.EntityStatus;
import org.spacehq.mc.protocol.data.game.values.entity.GlobalEntityType;
import org.spacehq.mc.protocol.data.game.values.entity.HangingDirection;
import org.spacehq.mc.protocol.data.game.values.entity.MetadataType;
import org.spacehq.mc.protocol.data.game.values.entity.MinecartType;
import org.spacehq.mc.protocol.data.game.values.entity.MobType;
import org.spacehq.mc.protocol.data.game.values.entity.ModifierOperation;
import org.spacehq.mc.protocol.data.game.values.entity.ModifierType;
import org.spacehq.mc.protocol.data.game.values.entity.ObjectType;
import org.spacehq.mc.protocol.data.game.values.entity.player.Animation;
import org.spacehq.mc.protocol.data.game.values.entity.player.BlockBreakStage;
import org.spacehq.mc.protocol.data.game.values.entity.player.CombatState;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.data.game.values.entity.player.InteractAction;
import org.spacehq.mc.protocol.data.game.values.entity.player.PlayerAction;
import org.spacehq.mc.protocol.data.game.values.entity.player.PlayerState;
import org.spacehq.mc.protocol.data.game.values.entity.player.PositionElement;
import org.spacehq.mc.protocol.data.game.values.scoreboard.NameTagVisibility;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ObjectiveAction;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ScoreType;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ScoreboardAction;
import org.spacehq.mc.protocol.data.game.values.scoreboard.ScoreboardPosition;
import org.spacehq.mc.protocol.data.game.values.scoreboard.TeamAction;
import org.spacehq.mc.protocol.data.game.values.scoreboard.TeamColor;
import org.spacehq.mc.protocol.data.game.values.setting.ChatVisibility;
import org.spacehq.mc.protocol.data.game.values.setting.Difficulty;
import org.spacehq.mc.protocol.data.game.values.statistic.Achievement;
import org.spacehq.mc.protocol.data.game.values.statistic.GenericStatistic;
import org.spacehq.mc.protocol.data.game.values.window.ClickItemParam;
import org.spacehq.mc.protocol.data.game.values.window.CreativeGrabParam;
import org.spacehq.mc.protocol.data.game.values.window.DropItemParam;
import org.spacehq.mc.protocol.data.game.values.window.FillStackParam;
import org.spacehq.mc.protocol.data.game.values.window.MoveToHotbarParam;
import org.spacehq.mc.protocol.data.game.values.window.ShiftClickItemParam;
import org.spacehq.mc.protocol.data.game.values.window.SpreadItemParam;
import org.spacehq.mc.protocol.data.game.values.window.WindowAction;
import org.spacehq.mc.protocol.data.game.values.window.WindowType;
import org.spacehq.mc.protocol.data.game.values.window.property.AnvilProperty;
import org.spacehq.mc.protocol.data.game.values.window.property.BrewingStandProperty;
import org.spacehq.mc.protocol.data.game.values.window.property.EnchantmentTableProperty;
import org.spacehq.mc.protocol.data.game.values.window.property.FurnaceProperty;
import org.spacehq.mc.protocol.data.game.values.world.GenericSound;
import org.spacehq.mc.protocol.data.game.values.world.Particle;
import org.spacehq.mc.protocol.data.game.values.world.WorldBorderAction;
import org.spacehq.mc.protocol.data.game.values.world.WorldType;
import org.spacehq.mc.protocol.data.game.values.world.block.UpdatedTileType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.ChestValueType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.GenericBlockValueType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.MobSpawnerValueType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.NoteBlockValueType;
import org.spacehq.mc.protocol.data.game.values.world.block.value.PistonValue;
import org.spacehq.mc.protocol.data.game.values.world.block.value.PistonValueType;
import org.spacehq.mc.protocol.data.game.values.world.effect.ParticleEffect;
import org.spacehq.mc.protocol.data.game.values.world.effect.SmokeEffectData;
import org.spacehq.mc.protocol.data.game.values.world.effect.SoundEffect;
import org.spacehq.mc.protocol.data.game.values.world.notify.ClientNotification;
import org.spacehq.mc.protocol.data.game.values.world.notify.DemoMessageValue;

public class MagicValues
{
  private static final Map<Enum<?>, Object> values = new HashMap();
  
  static
  {
    register(AttributeType.MAX_HEALTH, "generic.maxHealth");
    register(AttributeType.FOLLOW_RANGE, "generic.followRange");
    register(AttributeType.KNOCKBACK_RESISTANCE, "generic.knockbackResistance");
    register(AttributeType.MOVEMENT_SPEED, "generic.movementSpeed");
    register(AttributeType.ATTACK_DAMAGE, "generic.attackDamage");
    register(AttributeType.HORSE_JUMP_STRENGTH, "horse.jumpStrength");
    register(AttributeType.ZOMBIE_SPAWN_REINFORCEMENTS_CHANCE, "zombie.spawnReinforcements");
    
    register(ModifierType.CREATURE_FLEE_SPEED_BONUS, UUID.fromString("E199AD21-BA8A-4C53-8D13-6182D5C69D3A"));
    register(ModifierType.ENDERMAN_ATTACK_SPEED_BOOST, UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0"));
    register(ModifierType.SPRINT_SPEED_BOOST, UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D"));
    register(ModifierType.PIGZOMBIE_ATTACK_SPEED_BOOST, UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718"));
    register(ModifierType.WITCH_DRINKING_SPEED_PENALTY, UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E"));
    register(ModifierType.ZOMBIE_BABY_SPEED_BOOST, UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836"));
    register(ModifierType.ITEM_MODIFIER, UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF"));
    register(ModifierType.SPEED_POTION_MODIFIER, UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635"));
    register(ModifierType.HEALTH_BOOST_POTION_MODIFIER, UUID.fromString("5D6F0BA2-1186-46AC-B896-C61C5CEE99CC"));
    register(ModifierType.SLOW_POTION_MODIFIER, UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890"));
    register(ModifierType.STRENGTH_POTION_MODIFIER, UUID.fromString("648D7064-6A60-4F59-8ABE-C2C23A6DD7A9"));
    register(ModifierType.WEAKNESS_POTION_MODIFIER, UUID.fromString("22653B89-116E-49DC-9B6B-9971489B5BE5"));
    
    register(ModifierOperation.ADD, Integer.valueOf(0));
    register(ModifierOperation.ADD_MULTIPLIED, Integer.valueOf(1));
    register(ModifierOperation.MULTIPLY, Integer.valueOf(2));
    
    register(MetadataType.BYTE, Integer.valueOf(0));
    register(MetadataType.SHORT, Integer.valueOf(1));
    register(MetadataType.INT, Integer.valueOf(2));
    register(MetadataType.FLOAT, Integer.valueOf(3));
    register(MetadataType.STRING, Integer.valueOf(4));
    register(MetadataType.ITEM, Integer.valueOf(5));
    register(MetadataType.POSITION, Integer.valueOf(6));
    register(MetadataType.ROTATION, Integer.valueOf(7));
    
    register(HandshakeIntent.STATUS, Integer.valueOf(1));
    register(HandshakeIntent.LOGIN, Integer.valueOf(2));
    
    register(ClientRequest.RESPAWN, Integer.valueOf(0));
    register(ClientRequest.STATS, Integer.valueOf(1));
    register(ClientRequest.OPEN_INVENTORY_ACHIEVEMENT, Integer.valueOf(2));
    
    register(ChatVisibility.FULL, Integer.valueOf(0));
    register(ChatVisibility.SYSTEM, Integer.valueOf(1));
    register(ChatVisibility.HIDDEN, Integer.valueOf(2));
    
    register(PlayerState.START_SNEAKING, Integer.valueOf(0));
    register(PlayerState.STOP_SNEAKING, Integer.valueOf(1));
    register(PlayerState.LEAVE_BED, Integer.valueOf(2));
    register(PlayerState.START_SPRINTING, Integer.valueOf(3));
    register(PlayerState.STOP_SPRINTING, Integer.valueOf(4));
    register(PlayerState.RIDING_JUMP, Integer.valueOf(5));
    register(PlayerState.OPEN_INVENTORY, Integer.valueOf(6));
    
    register(InteractAction.INTERACT, Integer.valueOf(0));
    register(InteractAction.ATTACK, Integer.valueOf(1));
    register(InteractAction.INTERACT_AT, Integer.valueOf(2));
    
    register(PlayerAction.START_DIGGING, Integer.valueOf(0));
    register(PlayerAction.CANCEL_DIGGING, Integer.valueOf(1));
    register(PlayerAction.FINISH_DIGGING, Integer.valueOf(2));
    register(PlayerAction.DROP_ITEM_STACK, Integer.valueOf(3));
    register(PlayerAction.DROP_ITEM, Integer.valueOf(4));
    register(PlayerAction.RELEASE_USE_ITEM, Integer.valueOf(5));
    
    register(Face.BOTTOM, Integer.valueOf(0));
    register(Face.TOP, Integer.valueOf(1));
    register(Face.EAST, Integer.valueOf(2));
    register(Face.WEST, Integer.valueOf(3));
    register(Face.NORTH, Integer.valueOf(4));
    register(Face.SOUTH, Integer.valueOf(5));
    register(Face.INVALID, Integer.valueOf(255));
    
    register(WindowAction.CLICK_ITEM, Integer.valueOf(0));
    register(WindowAction.SHIFT_CLICK_ITEM, Integer.valueOf(1));
    register(WindowAction.MOVE_TO_HOTBAR_SLOT, Integer.valueOf(2));
    register(WindowAction.CREATIVE_GRAB_MAX_STACK, Integer.valueOf(3));
    register(WindowAction.DROP_ITEM, Integer.valueOf(4));
    register(WindowAction.SPREAD_ITEM, Integer.valueOf(5));
    register(WindowAction.FILL_STACK, Integer.valueOf(6));
    
    register(ClickItemParam.LEFT_CLICK, Integer.valueOf(0));
    register(ClickItemParam.RIGHT_CLICK, Integer.valueOf(1));
    
    register(ShiftClickItemParam.LEFT_CLICK, Integer.valueOf(0));
    register(ShiftClickItemParam.RIGHT_CLICK, Integer.valueOf(1));
    
    register(MoveToHotbarParam.SLOT_1, Integer.valueOf(0));
    register(MoveToHotbarParam.SLOT_2, Integer.valueOf(1));
    register(MoveToHotbarParam.SLOT_3, Integer.valueOf(2));
    register(MoveToHotbarParam.SLOT_4, Integer.valueOf(3));
    register(MoveToHotbarParam.SLOT_5, Integer.valueOf(4));
    register(MoveToHotbarParam.SLOT_6, Integer.valueOf(5));
    register(MoveToHotbarParam.SLOT_7, Integer.valueOf(6));
    register(MoveToHotbarParam.SLOT_8, Integer.valueOf(7));
    register(MoveToHotbarParam.SLOT_9, Integer.valueOf(8));
    
    register(CreativeGrabParam.GRAB, Integer.valueOf(2));
    
    register(DropItemParam.LEFT_CLICK_OUTSIDE_NOT_HOLDING, Integer.valueOf(0));
    register(DropItemParam.RIGHT_CLICK_OUTSIDE_NOT_HOLDING, Integer.valueOf(1));
    register(DropItemParam.DROP_FROM_SELECTED, Integer.valueOf(2));
    register(DropItemParam.DROP_SELECTED_STACK, Integer.valueOf(3));
    
    register(SpreadItemParam.LEFT_MOUSE_BEGIN_DRAG, Integer.valueOf(0));
    register(SpreadItemParam.LEFT_MOUSE_ADD_SLOT, Integer.valueOf(1));
    register(SpreadItemParam.LEFT_MOUSE_END_DRAG, Integer.valueOf(2));
    register(SpreadItemParam.RIGHT_MOUSE_BEGIN_DRAG, Integer.valueOf(4));
    register(SpreadItemParam.RIGHT_MOUSE_ADD_SLOT, Integer.valueOf(5));
    register(SpreadItemParam.RIGHT_MOUSE_END_DRAG, Integer.valueOf(6));
    
    register(FillStackParam.FILL, Integer.valueOf(0));
    
    register(MessageType.CHAT, Integer.valueOf(0));
    register(MessageType.SYSTEM, Integer.valueOf(1));
    register(MessageType.NOTIFICATION, Integer.valueOf(2));
    
    register(CombatState.ENTER_COMBAT, Integer.valueOf(0));
    register(CombatState.END_COMBAT, Integer.valueOf(1));
    register(CombatState.ENTITY_DEAD, Integer.valueOf(2));
    
    register(GameMode.SURVIVAL, Integer.valueOf(0));
    register(GameMode.CREATIVE, Integer.valueOf(1));
    register(GameMode.ADVENTURE, Integer.valueOf(2));
    register(GameMode.SPECTATOR, Integer.valueOf(3));
    
    register(Difficulty.PEACEFUL, Integer.valueOf(0));
    register(Difficulty.EASY, Integer.valueOf(1));
    register(Difficulty.NORMAL, Integer.valueOf(2));
    register(Difficulty.HARD, Integer.valueOf(3));
    
    register(WorldType.DEFAULT, "default");
    register(WorldType.FLAT, "flat");
    register(WorldType.LARGE_BIOMES, "largebiomes");
    register(WorldType.AMPLIFIED, "amplified");
    register(WorldType.CUSTOMIZED, "customized");
    register(WorldType.DEBUG, "debug_all_block_states");
    register(WorldType.DEFAULT_1_1, "default_1_1");
    
    register(Animation.SWING_ARM, Integer.valueOf(0));
    register(Animation.DAMAGE, Integer.valueOf(1));
    register(Animation.LEAVE_BED, Integer.valueOf(2));
    register(Animation.EAT_FOOD, Integer.valueOf(3));
    register(Animation.CRITICAL_HIT, Integer.valueOf(4));
    register(Animation.ENCHANTMENT_CRITICAL_HIT, Integer.valueOf(5));
    
    register(Effect.SPEED, Integer.valueOf(1));
    register(Effect.SLOWNESS, Integer.valueOf(2));
    register(Effect.DIG_SPEED, Integer.valueOf(3));
    register(Effect.DIG_SLOWNESS, Integer.valueOf(4));
    register(Effect.DAMAGE_BOOST, Integer.valueOf(5));
    register(Effect.HEAL, Integer.valueOf(6));
    register(Effect.DAMAGE, Integer.valueOf(7));
    register(Effect.JUMP_BOOST, Integer.valueOf(8));
    register(Effect.CONFUSION, Integer.valueOf(9));
    register(Effect.REGENERATION, Integer.valueOf(10));
    register(Effect.RESISTANCE, Integer.valueOf(11));
    register(Effect.FIRE_RESISTANCE, Integer.valueOf(12));
    register(Effect.WATER_BREATHING, Integer.valueOf(13));
    register(Effect.INVISIBILITY, Integer.valueOf(14));
    register(Effect.BLINDNESS, Integer.valueOf(15));
    register(Effect.NIGHT_VISION, Integer.valueOf(16));
    register(Effect.HUNGER, Integer.valueOf(17));
    register(Effect.WEAKNESS, Integer.valueOf(18));
    register(Effect.POISON, Integer.valueOf(19));
    register(Effect.WITHER_EFFECT, Integer.valueOf(20));
    register(Effect.HEALTH_BOOST, Integer.valueOf(21));
    register(Effect.ABSORPTION, Integer.valueOf(22));
    register(Effect.SATURATION, Integer.valueOf(23));
    
    register(EntityStatus.HURT_OR_MINECART_SPAWNER_DELAY_RESET, Integer.valueOf(1));
    register(EntityStatus.LIVING_HURT, Integer.valueOf(2));
    register(EntityStatus.DEAD, Integer.valueOf(3));
    register(EntityStatus.IRON_GOLEM_THROW, Integer.valueOf(4));
    register(EntityStatus.TAMING, Integer.valueOf(6));
    register(EntityStatus.TAMED, Integer.valueOf(7));
    register(EntityStatus.WOLF_SHAKING, Integer.valueOf(8));
    register(EntityStatus.FINISHED_EATING, Integer.valueOf(9));
    register(EntityStatus.SHEEP_GRAZING_OR_TNT_CART_EXPLODING, Integer.valueOf(10));
    register(EntityStatus.IRON_GOLEM_ROSE, Integer.valueOf(11));
    register(EntityStatus.VILLAGER_HEARTS, Integer.valueOf(12));
    register(EntityStatus.VILLAGER_ANGRY, Integer.valueOf(13));
    register(EntityStatus.VILLAGER_HAPPY, Integer.valueOf(14));
    register(EntityStatus.WITCH_MAGIC_PARTICLES, Integer.valueOf(15));
    register(EntityStatus.ZOMBIE_VILLAGER_SHAKING, Integer.valueOf(16));
    register(EntityStatus.FIREWORK_EXPLODING, Integer.valueOf(17));
    register(EntityStatus.ANIMAL_HEARTS, Integer.valueOf(18));
    register(EntityStatus.RESET_SQUID_ROTATION, Integer.valueOf(19));
    register(EntityStatus.EXPLOSION_PARTICLE, Integer.valueOf(20));
    register(EntityStatus.GUARDIAN_SOUND, Integer.valueOf(21));
    register(EntityStatus.ENABLE_REDUCED_DEBUG, Integer.valueOf(22));
    register(EntityStatus.DISABLE_REDUCED_DEBUG, Integer.valueOf(23));
    
    register(PositionElement.X, Integer.valueOf(0));
    register(PositionElement.Y, Integer.valueOf(1));
    register(PositionElement.Z, Integer.valueOf(2));
    register(PositionElement.PITCH, Integer.valueOf(3));
    register(PositionElement.YAW, Integer.valueOf(4));
    
    register(GlobalEntityType.LIGHTNING_BOLT, Integer.valueOf(1));
    
    register(MobType.ARMOR_STAND, Integer.valueOf(30));
    register(MobType.CREEPER, Integer.valueOf(50));
    register(MobType.SKELETON, Integer.valueOf(51));
    register(MobType.SPIDER, Integer.valueOf(52));
    register(MobType.GIANT_ZOMBIE, Integer.valueOf(53));
    register(MobType.ZOMBIE, Integer.valueOf(54));
    register(MobType.SLIME, Integer.valueOf(55));
    register(MobType.GHAST, Integer.valueOf(56));
    register(MobType.ZOMBIE_PIGMAN, Integer.valueOf(57));
    register(MobType.ENDERMAN, Integer.valueOf(58));
    register(MobType.CAVE_SPIDER, Integer.valueOf(59));
    register(MobType.SILVERFISH, Integer.valueOf(60));
    register(MobType.BLAZE, Integer.valueOf(61));
    register(MobType.MAGMA_CUBE, Integer.valueOf(62));
    register(MobType.ENDER_DRAGON, Integer.valueOf(63));
    register(MobType.WITHER, Integer.valueOf(64));
    register(MobType.BAT, Integer.valueOf(65));
    register(MobType.WITCH, Integer.valueOf(66));
    register(MobType.ENDERMITE, Integer.valueOf(67));
    register(MobType.GUARDIAN, Integer.valueOf(68));
    register(MobType.PIG, Integer.valueOf(90));
    register(MobType.SHEEP, Integer.valueOf(91));
    register(MobType.COW, Integer.valueOf(92));
    register(MobType.CHICKEN, Integer.valueOf(93));
    register(MobType.SQUID, Integer.valueOf(94));
    register(MobType.WOLF, Integer.valueOf(95));
    register(MobType.MOOSHROOM, Integer.valueOf(96));
    register(MobType.SNOWMAN, Integer.valueOf(97));
    register(MobType.OCELOT, Integer.valueOf(98));
    register(MobType.IRON_GOLEM, Integer.valueOf(99));
    register(MobType.HORSE, Integer.valueOf(100));
    register(MobType.RABBIT, Integer.valueOf(101));
    register(MobType.VILLAGER, Integer.valueOf(120));
    
    register(ObjectType.BOAT, Integer.valueOf(1));
    register(ObjectType.ITEM, Integer.valueOf(2));
    register(ObjectType.MINECART, Integer.valueOf(10));
    register(ObjectType.PRIMED_TNT, Integer.valueOf(50));
    register(ObjectType.ENDER_CRYSTAL, Integer.valueOf(51));
    register(ObjectType.ARROW, Integer.valueOf(60));
    register(ObjectType.SNOWBALL, Integer.valueOf(61));
    register(ObjectType.EGG, Integer.valueOf(62));
    register(ObjectType.GHAST_FIREBALL, Integer.valueOf(63));
    register(ObjectType.BLAZE_FIREBALL, Integer.valueOf(64));
    register(ObjectType.ENDER_PEARL, Integer.valueOf(65));
    register(ObjectType.WITHER_HEAD_PROJECTILE, Integer.valueOf(66));
    register(ObjectType.FALLING_BLOCK, Integer.valueOf(70));
    register(ObjectType.ITEM_FRAME, Integer.valueOf(71));
    register(ObjectType.EYE_OF_ENDER, Integer.valueOf(72));
    register(ObjectType.POTION, Integer.valueOf(73));
    register(ObjectType.FALLING_DRAGON_EGG, Integer.valueOf(74));
    register(ObjectType.EXP_BOTTLE, Integer.valueOf(75));
    register(ObjectType.FIREWORK_ROCKET, Integer.valueOf(76));
    register(ObjectType.LEASH_KNOT, Integer.valueOf(77));
    register(ObjectType.ARMOR_STAND, Integer.valueOf(78));
    register(ObjectType.FISH_HOOK, Integer.valueOf(90));
    
    register(MinecartType.NORMAL, Integer.valueOf(0));
    register(MinecartType.CHEST, Integer.valueOf(1));
    register(MinecartType.POWERED, Integer.valueOf(2));
    register(MinecartType.TNT, Integer.valueOf(3));
    register(MinecartType.MOB_SPAWNER, Integer.valueOf(4));
    register(MinecartType.HOPPER, Integer.valueOf(5));
    register(MinecartType.COMMAND_BLOCK, Integer.valueOf(6));
    
    register(HangingDirection.SOUTH, Integer.valueOf(0));
    register(HangingDirection.WEST, Integer.valueOf(1));
    register(HangingDirection.NORTH, Integer.valueOf(2));
    register(HangingDirection.EAST, Integer.valueOf(3));
    
    register(Art.KEBAB, "Kebab");
    register(Art.AZTEC, "Aztec");
    register(Art.ALBAN, "Alban");
    register(Art.AZTEC2, "Aztec2");
    register(Art.BOMB, "Bomb");
    register(Art.PLANT, "Plant");
    register(Art.WASTELAND, "Wasteland");
    register(Art.POOL, "Pool");
    register(Art.COURBET, "Courbet");
    register(Art.SEA, "Sea");
    register(Art.SUNSET, "Sunset");
    register(Art.CREEBET, "Creebet");
    register(Art.WANDERER, "Wanderer");
    register(Art.GRAHAM, "Graham");
    register(Art.MATCH, "Match");
    register(Art.BUST, "Bust");
    register(Art.STAGE, "Stage");
    register(Art.VOID, "Void");
    register(Art.SKULL_AND_ROSES, "SkullAndRoses");
    register(Art.WITHER, "Wither");
    register(Art.FIGHTERS, "Fighters");
    register(Art.POINTER, "Pointer");
    register(Art.PIG_SCENE, "Pigscene");
    register(Art.BURNING_SKULL, "BurningSkull");
    register(Art.SKELETON, "Skeleton");
    register(Art.DONKEY_KONG, "DonkeyKong");
    
    register(ScoreboardPosition.PLAYER_LIST, Integer.valueOf(0));
    register(ScoreboardPosition.SIDEBAR, Integer.valueOf(1));
    register(ScoreboardPosition.BELOW_NAME, Integer.valueOf(2));
    register(ScoreboardPosition.SIDEBAR_TEAM_BLACK, Integer.valueOf(3));
    register(ScoreboardPosition.SIDEBAR_TEAM_DARK_BLUE, Integer.valueOf(4));
    register(ScoreboardPosition.SIDEBAR_TEAM_DARK_GREEN, Integer.valueOf(5));
    register(ScoreboardPosition.SIDEBAR_TEAM_DARK_AQUA, Integer.valueOf(6));
    register(ScoreboardPosition.SIDEBAR_TEAM_DARK_RED, Integer.valueOf(7));
    register(ScoreboardPosition.SIDEBAR_TEAM_DARK_PURPLE, Integer.valueOf(8));
    register(ScoreboardPosition.SIDEBAR_TEAM_GOLD, Integer.valueOf(9));
    register(ScoreboardPosition.SIDEBAR_TEAM_GRAY, Integer.valueOf(10));
    register(ScoreboardPosition.SIDEBAR_TEAM_DARK_GRAY, Integer.valueOf(11));
    register(ScoreboardPosition.SIDEBAR_TEAM_BLUE, Integer.valueOf(12));
    register(ScoreboardPosition.SIDEBAR_TEAM_GREEN, Integer.valueOf(13));
    register(ScoreboardPosition.SIDEBAR_TEAM_AQUA, Integer.valueOf(14));
    register(ScoreboardPosition.SIDEBAR_TEAM_RED, Integer.valueOf(15));
    register(ScoreboardPosition.SIDEBAR_TEAM_LIGHT_PURPLE, Integer.valueOf(16));
    register(ScoreboardPosition.SIDEBAR_TEAM_YELLOW, Integer.valueOf(17));
    register(ScoreboardPosition.SIDEBAR_TEAM_WHITE, Integer.valueOf(18));
    
    register(ObjectiveAction.ADD, Integer.valueOf(0));
    register(ObjectiveAction.REMOVE, Integer.valueOf(1));
    register(ObjectiveAction.UPDATE, Integer.valueOf(2));
    
    register(TeamAction.CREATE, Integer.valueOf(0));
    register(TeamAction.REMOVE, Integer.valueOf(1));
    register(TeamAction.UPDATE, Integer.valueOf(2));
    register(TeamAction.ADD_PLAYER, Integer.valueOf(3));
    register(TeamAction.REMOVE_PLAYER, Integer.valueOf(4));
    
    register(ScoreboardAction.ADD_OR_UPDATE, Integer.valueOf(0));
    register(ScoreboardAction.REMOVE, Integer.valueOf(1));
    
    register(WindowType.GENERIC_INVENTORY, "minecraft:container");
    register(WindowType.ANVIL, "minecraft:anvil");
    register(WindowType.BEACON, "minecraft:beacon");
    register(WindowType.BREWING_STAND, "minecraft:brewing_stand");
    register(WindowType.CHEST, "minecraft:chest");
    register(WindowType.CRAFTING_TABLE, "minecraft:crafting_table");
    register(WindowType.DISPENSER, "minecraft:dispenser");
    register(WindowType.DROPPER, "minecraft:dropper");
    register(WindowType.ENCHANTING_TABLE, "minecraft:enchanting_table");
    register(WindowType.FURNACE, "minecraft:furnace");
    register(WindowType.HOPPER, "minecraft:hopper");
    register(WindowType.VILLAGER, "minecraft:villager");
    register(WindowType.HORSE, "EntityHorse");
    
    register(BrewingStandProperty.BREW_TIME, Integer.valueOf(0));
    
    register(EnchantmentTableProperty.LEVEL_SLOT_1, Integer.valueOf(0));
    register(EnchantmentTableProperty.LEVEL_SLOT_2, Integer.valueOf(1));
    register(EnchantmentTableProperty.LEVEL_SLOT_3, Integer.valueOf(2));
    register(EnchantmentTableProperty.XP_SEED, Integer.valueOf(3));
    register(EnchantmentTableProperty.ENCHANTMENT_SLOT_1, Integer.valueOf(4));
    register(EnchantmentTableProperty.ENCHANTMENT_SLOT_2, Integer.valueOf(5));
    register(EnchantmentTableProperty.ENCHANTMENT_SLOT_3, Integer.valueOf(6));
    
    register(FurnaceProperty.BURN_TIME, Integer.valueOf(0));
    register(FurnaceProperty.CURRENT_ITEM_BURN_TIME, Integer.valueOf(1));
    register(FurnaceProperty.COOK_TIME, Integer.valueOf(2));
    register(FurnaceProperty.TOTAL_COOK_TIME, Integer.valueOf(3));
    
    register(AnvilProperty.MAXIMUM_COST, Integer.valueOf(0));
    
    register(BlockBreakStage.RESET, Integer.valueOf(-1));
    register(BlockBreakStage.STAGE_1, Integer.valueOf(0));
    register(BlockBreakStage.STAGE_2, Integer.valueOf(1));
    register(BlockBreakStage.STAGE_3, Integer.valueOf(2));
    register(BlockBreakStage.STAGE_4, Integer.valueOf(3));
    register(BlockBreakStage.STAGE_5, Integer.valueOf(4));
    register(BlockBreakStage.STAGE_6, Integer.valueOf(5));
    register(BlockBreakStage.STAGE_7, Integer.valueOf(6));
    register(BlockBreakStage.STAGE_8, Integer.valueOf(7));
    register(BlockBreakStage.STAGE_9, Integer.valueOf(8));
    register(BlockBreakStage.STAGE_10, Integer.valueOf(9));
    register(BlockBreakStage.RESET, Integer.valueOf(255));
    
    register(UpdatedTileType.MOB_SPAWNER, Integer.valueOf(1));
    register(UpdatedTileType.COMMAND_BLOCK, Integer.valueOf(2));
    register(UpdatedTileType.BEACON, Integer.valueOf(3));
    register(UpdatedTileType.SKULL, Integer.valueOf(4));
    register(UpdatedTileType.FLOWER_POT, Integer.valueOf(5));
    register(UpdatedTileType.BANNER, Integer.valueOf(6));
    
    register(ClientNotification.INVALID_BED, Integer.valueOf(0));
    register(ClientNotification.START_RAIN, Integer.valueOf(1));
    register(ClientNotification.STOP_RAIN, Integer.valueOf(2));
    register(ClientNotification.CHANGE_GAMEMODE, Integer.valueOf(3));
    register(ClientNotification.ENTER_CREDITS, Integer.valueOf(4));
    register(ClientNotification.DEMO_MESSAGE, Integer.valueOf(5));
    register(ClientNotification.ARROW_HIT_PLAYER, Integer.valueOf(6));
    register(ClientNotification.RAIN_STRENGTH, Integer.valueOf(7));
    register(ClientNotification.THUNDER_STRENGTH, Integer.valueOf(8));
    
    register(DemoMessageValue.WELCOME, Integer.valueOf(0));
    register(DemoMessageValue.MOVEMENT_CONTROLS, Integer.valueOf(101));
    register(DemoMessageValue.JUMP_CONTROL, Integer.valueOf(102));
    register(DemoMessageValue.INVENTORY_CONTROL, Integer.valueOf(103));
    
    register(Achievement.OPEN_INVENTORY, "achievement.openInventory");
    register(Achievement.GET_WOOD, "achievement.mineWood");
    register(Achievement.MAKE_WORKBENCH, "achievement.buildWorkBench");
    register(Achievement.MAKE_PICKAXE, "achievement.buildPickaxe");
    register(Achievement.MAKE_FURNACE, "achievement.buildFurnace");
    register(Achievement.GET_IRON, "achievement.acquireIron");
    register(Achievement.MAKE_HOE, "achievement.buildHoe");
    register(Achievement.MAKE_BREAD, "achievement.makeBread");
    register(Achievement.MAKE_CAKE, "achievement.bakeCake");
    register(Achievement.MAKE_IRON_PICKAXE, "achievement.buildBetterPickaxe");
    register(Achievement.COOK_FISH, "achievement.cookFish");
    register(Achievement.RIDE_MINECART_1000_BLOCKS, "achievement.onARail");
    register(Achievement.MAKE_SWORD, "achievement.buildSword");
    register(Achievement.KILL_ENEMY, "achievement.killEnemy");
    register(Achievement.KILL_COW, "achievement.killCow");
    register(Achievement.FLY_PIG, "achievement.flyPig");
    register(Achievement.SNIPE_SKELETON, "achievement.snipeSkeleton");
    register(Achievement.GET_DIAMONDS, "achievement.diamonds");
    register(Achievement.GIVE_DIAMONDS, "achievement.diamondsToYou");
    register(Achievement.ENTER_PORTAL, "achievement.portal");
    register(Achievement.ATTACKED_BY_GHAST, "achievement.ghast");
    register(Achievement.GET_BLAZE_ROD, "achievement.blazeRod");
    register(Achievement.MAKE_POTION, "achievement.potion");
    register(Achievement.GO_TO_THE_END, "achievement.theEnd");
    register(Achievement.DEFEAT_ENDER_DRAGON, "achievement.theEnd2");
    register(Achievement.DEAL_18_OR_MORE_DAMAGE, "achievement.overkill");
    register(Achievement.MAKE_BOOKCASE, "achievement.bookcase");
    register(Achievement.BREED_COW, "achievement.breedCow");
    register(Achievement.SPAWN_WITHER, "achievement.spawnWither");
    register(Achievement.KILL_WITHER, "achievement.killWither");
    register(Achievement.MAKE_FULL_BEACON, "achievement.fullBeacon");
    register(Achievement.EXPLORE_ALL_BIOMES, "achievement.exploreAllBiomes");
    
    register(GenericStatistic.TIMES_LEFT_GAME, "stat.leaveGame");
    register(GenericStatistic.MINUTES_PLAYED, "stat.playOneMinute");
    register(GenericStatistic.BLOCKS_WALKED, "stat.walkOneCm");
    register(GenericStatistic.BLOCKS_SWAM, "stat.swimOneCm");
    register(GenericStatistic.BLOCKS_FALLEN, "stat.fallOneCm");
    register(GenericStatistic.BLOCKS_CLIMBED, "stat.climbOneCm");
    register(GenericStatistic.BLOCKS_FLOWN, "stat.flyOneCm");
    register(GenericStatistic.BLOCKS_DOVE, "stat.diveOneCm");
    register(GenericStatistic.BLOCKS_TRAVELLED_IN_MINECART, "stat.minecartOneCm");
    register(GenericStatistic.BLOCKS_TRAVELLED_IN_BOAT, "stat.boatOneCm");
    register(GenericStatistic.BLOCKS_RODE_ON_PIG, "stat.pigOneCm");
    register(GenericStatistic.BLOCKS_RODE_ON_HORSE, "stat.horseOneCm");
    register(GenericStatistic.TIMES_JUMPED, "stat.jump");
    register(GenericStatistic.TIMES_DROPPED_ITEMS, "stat.drop");
    register(GenericStatistic.TIMES_DEALT_DAMAGE, "stat.damageDealt");
    register(GenericStatistic.DAMAGE_TAKEN, "stat.damageTaken");
    register(GenericStatistic.DEATHS, "stat.deaths");
    register(GenericStatistic.MOB_KILLS, "stat.mobKills");
    register(GenericStatistic.ANIMALS_BRED, "stat.animalsBred");
    register(GenericStatistic.PLAYERS_KILLED, "stat.playerKills");
    register(GenericStatistic.FISH_CAUGHT, "stat.fishCaught");
    register(GenericStatistic.JUNK_FISHED, "stat.junkFished");
    register(GenericStatistic.TREASURE_FISHED, "stat.treasureFished");
    
    register(Particle.EXPLOSION_NORMAL, Integer.valueOf(0));
    register(Particle.EXPLOSION_LARGE, Integer.valueOf(1));
    register(Particle.EXPLOSION_HUGE, Integer.valueOf(2));
    register(Particle.FIREWORKS_SPARK, Integer.valueOf(3));
    register(Particle.WATER_BUBBLE, Integer.valueOf(4));
    register(Particle.WATER_SPLASH, Integer.valueOf(5));
    register(Particle.WATER_WAKE, Integer.valueOf(6));
    register(Particle.SUSPENDED, Integer.valueOf(7));
    register(Particle.SUSPENDED_DEPTH, Integer.valueOf(8));
    register(Particle.CRIT, Integer.valueOf(9));
    register(Particle.CRIT_MAGIC, Integer.valueOf(10));
    register(Particle.SMOKE_NORMAL, Integer.valueOf(11));
    register(Particle.SMOKE_LARGE, Integer.valueOf(12));
    register(Particle.SPELL, Integer.valueOf(13));
    register(Particle.SPELL_INSTANT, Integer.valueOf(14));
    register(Particle.SPELL_MOB, Integer.valueOf(15));
    register(Particle.SPELL_MOB_AMBIENT, Integer.valueOf(16));
    register(Particle.SPELL_WITCH, Integer.valueOf(17));
    register(Particle.DRIP_WATER, Integer.valueOf(18));
    register(Particle.DRIP_LAVA, Integer.valueOf(19));
    register(Particle.VILLAGER_ANGRY, Integer.valueOf(20));
    register(Particle.VILLAGER_HAPPY, Integer.valueOf(21));
    register(Particle.TOWN_AURA, Integer.valueOf(22));
    register(Particle.NOTE, Integer.valueOf(23));
    register(Particle.PORTAL, Integer.valueOf(24));
    register(Particle.ENCHANTMENT_TABLE, Integer.valueOf(25));
    register(Particle.FLAME, Integer.valueOf(26));
    register(Particle.LAVA, Integer.valueOf(27));
    register(Particle.FOOTSTEP, Integer.valueOf(28));
    register(Particle.CLOUD, Integer.valueOf(29));
    register(Particle.REDSTONE, Integer.valueOf(30));
    register(Particle.SNOWBALL, Integer.valueOf(31));
    register(Particle.SNOW_SHOVEL, Integer.valueOf(32));
    register(Particle.SLIME, Integer.valueOf(33));
    register(Particle.HEART, Integer.valueOf(34));
    register(Particle.BARRIER, Integer.valueOf(35));
    register(Particle.ICON_CRACK, Integer.valueOf(36));
    register(Particle.BLOCK_CRACK, Integer.valueOf(37));
    register(Particle.BLOCK_DUST, Integer.valueOf(38));
    register(Particle.WATER_DROP, Integer.valueOf(39));
    register(Particle.ITEM_TAKE, Integer.valueOf(40));
    register(Particle.MOB_APPEARANCE, Integer.valueOf(41));
    
    register(GenericSound.CLICK, "random.click");
    register(GenericSound.FIZZ, "random.fizz");
    register(GenericSound.FIRE_AMBIENT, "fire.fire");
    register(GenericSound.IGNITE_FIRE, "fire.ignite");
    register(GenericSound.WATER_AMBIENT, "liquid.water");
    register(GenericSound.LAVA_AMBIENT, "liquid.lava");
    register(GenericSound.LAVA_POP, "liquid.lavapop");
    register(GenericSound.HARP, "note.harp");
    register(GenericSound.BASS_DRUM, "note.bd");
    register(GenericSound.SNARE_DRUM, "note.snare");
    register(GenericSound.HI_HAT, "note.hat");
    register(GenericSound.DOUBLE_BASS, "note.bassattack");
    register(GenericSound.PISTON_EXTEND, "tile.piston.out");
    register(GenericSound.PISTON_RETRACT, "tile.piston.in");
    register(GenericSound.PORTAL_AMBIENT, "portal.portal");
    register(GenericSound.TNT_PRIMED, "game.tnt.primed");
    register(GenericSound.BOW_HIT, "random.bowhit");
    register(GenericSound.COLLECT_ITEM, "random.pop");
    register(GenericSound.COLLECT_EXP, "random.orb");
    register(GenericSound.SUCCESSFUL_HIT, "random.successful_hit");
    register(GenericSound.FIREWORK_BLAST, "fireworks.blast");
    register(GenericSound.FIREWORK_LARGE_BLAST, "fireworks.largeBlast");
    register(GenericSound.FIREWORK_FAR_BLAST, "fireworks.blast_far");
    register(GenericSound.FIREWORK_FAR_LARGE_BLAST, "fireworks.largeBlast_far");
    register(GenericSound.FIREWORK_TWINKLE, "fireworks.twinkle");
    register(GenericSound.FIREWORK_FAR_TWINKLE, "fireworks.twinkle_far");
    register(GenericSound.RAIN_AMBIENT, "ambient.weather.rain");
    register(GenericSound.WITHER_SPAWN, "mob.wither.spawn");
    register(GenericSound.ENDER_DRAGON_DEATH, "mob.enderdragon.end");
    register(GenericSound.FIRE_PROJECTILE, "random.bow");
    register(GenericSound.DOOR_OPEN, "random.door_open");
    register(GenericSound.DOOR_CLOSE, "random.door_close");
    register(GenericSound.GHAST_CHARGE, "mob.ghast.charge");
    register(GenericSound.GHAST_FIRE, "mob.ghast.fireball");
    register(GenericSound.POUND_WOODEN_DOOR, "mob.zombie.wood");
    register(GenericSound.POUND_METAL_DOOR, "mob.zombie.metal");
    register(GenericSound.BREAK_WOODEN_DOOR, "mob.zombie.woodbreak");
    register(GenericSound.WITHER_SHOOT, "mob.wither.shoot");
    register(GenericSound.BAT_TAKE_OFF, "mob.bat.takeoff");
    register(GenericSound.INFECT_VILLAGER, "mob.zombie.infect");
    register(GenericSound.DISINFECT_VILLAGER, "mob.zombie.unfect");
    register(GenericSound.ANVIL_BREAK, "random.anvil_break");
    register(GenericSound.ANVIL_USE, "random.anvil_use");
    register(GenericSound.ANVIL_LAND, "random.anvil_land");
    register(GenericSound.BREAK_SPLASH_POTION, "game.potion.smash");
    register(GenericSound.THORNS_DAMAGE, "damage.thorns");
    register(GenericSound.EXPLOSION, "random.explode");
    register(GenericSound.CAVE_AMBIENT, "ambient.cave.cave");
    register(GenericSound.OPEN_CHEST, "random.chestopen");
    register(GenericSound.CLOSE_CHEST, "random.chestclosed");
    register(GenericSound.DIG_STONE, "dig.stone");
    register(GenericSound.DIG_WOOD, "dig.wood");
    register(GenericSound.DIG_GRAVEL, "dig.gravel");
    register(GenericSound.DIG_GRASS, "dig.grass");
    register(GenericSound.DIG_CLOTH, "dig.cloth");
    register(GenericSound.DIG_SAND, "dig.sand");
    register(GenericSound.DIG_SNOW, "dig.snow");
    register(GenericSound.DIG_GLASS, "dig.glass");
    register(GenericSound.ANVIL_STEP, "step.anvil");
    register(GenericSound.LADDER_STEP, "step.ladder");
    register(GenericSound.STONE_STEP, "step.stone");
    register(GenericSound.WOOD_STEP, "step.wood");
    register(GenericSound.GRAVEL_STEP, "step.gravel");
    register(GenericSound.GRASS_STEP, "step.grass");
    register(GenericSound.CLOTH_STEP, "step.cloth");
    register(GenericSound.SAND_STEP, "step.sand");
    register(GenericSound.SNOW_STEP, "step.snow");
    register(GenericSound.BURP, "random.burp");
    register(GenericSound.SADDLE_HORSE, "mob.horse.leather");
    register(GenericSound.ENDER_DRAGON_FLAP_WINGS, "mob.enderdragon.wings");
    register(GenericSound.THUNDER_AMBIENT, "ambient.weather.thunder");
    register(GenericSound.LAUNCH_FIREWORKS, "fireworks.launch");
    register(GenericSound.CREEPER_PRIMED, "creeper.primed");
    register(GenericSound.ENDERMAN_STARE, "mob.endermen.stare");
    register(GenericSound.ENDERMAN_TELEPORT, "mob.endermen.portal");
    register(GenericSound.IRON_GOLEM_THROW, "mob.irongolem.throw");
    register(GenericSound.IRON_GOLEM_WALK, "mob.irongolem.walk");
    register(GenericSound.ZOMBIE_PIGMAN_ANGRY, "mob.zombiepig.zpigangry");
    register(GenericSound.SILVERFISH_STEP, "mob.silverfish.step");
    register(GenericSound.SKELETON_STEP, "mob.skeleton.step");
    register(GenericSound.SPIDER_STEP, "mob.spider.step");
    register(GenericSound.ZOMBIE_STEP, "mob.zombie.step");
    register(GenericSound.ZOMBIE_CURE, "mob.zombie.remedy");
    register(GenericSound.CHICKEN_LAY_EGG, "mob.chicken.plop");
    register(GenericSound.CHICKEN_STEP, "mob.chicken.step");
    register(GenericSound.COW_STEP, "mob.cow.step");
    register(GenericSound.HORSE_EATING, "eating");
    register(GenericSound.HORSE_LAND, "mob.horse.land");
    register(GenericSound.HORSE_WEAR_ARMOR, "mob.horse.armor");
    register(GenericSound.HORSE_GALLOP, "mob.horse.gallop");
    register(GenericSound.HORSE_BREATHE, "mob.horse.breathe");
    register(GenericSound.HORSE_WOOD_STEP, "mob.horse.wood");
    register(GenericSound.HORSE_SOFT_STEP, "mob.horse.soft");
    register(GenericSound.HORSE_JUMP, "mob.horse.jump");
    register(GenericSound.SHEAR_SHEEP, "mob.sheep.shear");
    register(GenericSound.PIG_STEP, "mob.pig.step");
    register(GenericSound.SHEEP_STEP, "mob.sheep.step");
    register(GenericSound.VILLAGER_YES, "mob.villager.yes");
    register(GenericSound.VILLAGER_NO, "mob.villager.no");
    register(GenericSound.WOLF_STEP, "mob.wolf.step");
    register(GenericSound.WOLF_SHAKE, "mob.wolf.shake");
    register(GenericSound.DRINK, "random.drink");
    register(GenericSound.EAT, "random.eat");
    register(GenericSound.LEVEL_UP, "random.levelup");
    register(GenericSound.FISH_HOOK_SPLASH, "random.splash");
    register(GenericSound.ITEM_BREAK, "random.break");
    register(GenericSound.SWIM, "game.neutral.swim");
    register(GenericSound.SPLASH, "game.neutral.swim.splash");
    register(GenericSound.HURT, "game.neutral.hurt");
    register(GenericSound.DEATH, "game.neutral.die");
    register(GenericSound.BIG_FALL, "game.neutral.hurt.fall.big");
    register(GenericSound.SMALL_FALL, "game.neutral.hurt.fall.small");
    register(GenericSound.MOB_SWIM, "game.hostile.swim");
    register(GenericSound.MOB_SPLASH, "game.hostile.swim.splash");
    register(GenericSound.PLAYER_SWIM, "game.player.swim");
    register(GenericSound.PLAYER_SPLASH, "game.player.swim.splash");
    register(GenericSound.ENDER_DRAGON_GROWL, "mob.enderdragon.growl");
    register(GenericSound.WITHER_IDLE, "mob.wither.idle");
    register(GenericSound.BLAZE_BREATHE, "mob.blaze.breathe");
    register(GenericSound.ENDERMAN_SCREAM, "mob.endermen.scream");
    register(GenericSound.ENDERMAN_IDLE, "mob.endermen.idle");
    register(GenericSound.GHAST_MOAN, "mob.ghast.moan");
    register(GenericSound.ZOMBIE_PIGMAN_IDLE, "mob.zombiepig.zpig");
    register(GenericSound.SILVERFISH_IDLE, "mob.silverfish.say");
    register(GenericSound.SKELETON_IDLE, "mob.skeleton.say");
    register(GenericSound.SPIDER_IDLE, "mob.spider.say");
    register(GenericSound.WITCH_IDLE, "mob.witch.idle");
    register(GenericSound.ZOMBIE_IDLE, "mob.zombie.say");
    register(GenericSound.BAT_IDLE, "mob.bat.idle");
    register(GenericSound.CHICKEN_IDLE, "mob.chicken.say");
    register(GenericSound.COW_IDLE, "mob.cow.say");
    register(GenericSound.HORSE_IDLE, "mob.horse.idle");
    register(GenericSound.DONKEY_IDLE, "mob.horse.donkey.idle");
    register(GenericSound.ZOMBIE_HORSE_IDLE, "mob.horse.zombie.idle");
    register(GenericSound.SKELETON_HORSE_IDLE, "mob.horse.skeleton.idle");
    register(GenericSound.OCELOT_PURR, "mob.cat.purr");
    register(GenericSound.OCELOT_PURR_MEOW, "mob.cat.purreow");
    register(GenericSound.OCELOT_MEOW, "mob.cat.meow");
    register(GenericSound.PIG_IDLE, "mob.pig.say");
    register(GenericSound.SHEEP_IDLE, "mob.sheep.say");
    register(GenericSound.VILLAGER_HAGGLE, "mob.villager.haggle");
    register(GenericSound.VILLAGER_IDLE, "mob.villager.idle");
    register(GenericSound.WOLF_GROWL, "mob.wolf.growl");
    register(GenericSound.WOLF_PANT, "mob.wolf.panting");
    register(GenericSound.WOLF_WHINE, "mob.wolf.whine");
    register(GenericSound.WOLF_BARK, "mob.wolf.bark");
    register(GenericSound.MOB_BIG_FALL, "game.hostile.hurt.fall.big");
    register(GenericSound.MOB_SMALL_FALL, "game.hostile.hurt.fall.small");
    register(GenericSound.PLAYER_BIG_FALL, "game.player.hurt.fall.big");
    register(GenericSound.PLAYER_SMALL_FALL, "game.player.hurt.fall.small");
    register(GenericSound.ENDER_DRAGON_HURT, "mob.enderdragon.hit");
    register(GenericSound.WITHER_HURT, "mob.wither.hurt");
    register(GenericSound.WITHER_DEATH, "mob.wither.death");
    register(GenericSound.BLAZE_HURT, "mob.blaze.hit");
    register(GenericSound.BLAZE_DEATH, "mob.blaze.death");
    register(GenericSound.CREEPER_HURT, "mob.creeper.say");
    register(GenericSound.CREEPER_DEATH, "mob.creeper.death");
    register(GenericSound.ENDERMAN_HURT, "mob.endermen.hit");
    register(GenericSound.ENDERMAN_DEATH, "mob.endermen.death");
    register(GenericSound.GHAST_HURT, "mob.ghast.scream");
    register(GenericSound.GHAST_DEATH, "mob.ghast.death");
    register(GenericSound.IRON_GOLEM_HURT, "mob.irongolem.hit");
    register(GenericSound.IRON_GOLEM_DEATH, "mob.irongolem.death");
    register(GenericSound.MOB_HURT, "game.hostile.hurt");
    register(GenericSound.MOB_DEATH, "game.hostile.die");
    register(GenericSound.ZOMBIE_PIGMAN_HURT, "mob.zombiepig.zpighurt");
    register(GenericSound.ZOMBIE_PIGMAN_DEATH, "mob.zombiepig.zpigdeath");
    register(GenericSound.SILVERFISH_HURT, "mob.silverfish.hit");
    register(GenericSound.SILVERFISH_DEATH, "mob.silverfish.kill");
    register(GenericSound.SKELETON_HURT, "mob.skeleton.hurt");
    register(GenericSound.SKELETON_DEATH, "mob.skeleton.death");
    register(GenericSound.SLIME, "mob.slime.small");
    register(GenericSound.BIG_SLIME, "mob.slime.big");
    register(GenericSound.SPIDER_DEATH, "mob.spider.death");
    register(GenericSound.WITCH_HURT, "mob.witch.hurt");
    register(GenericSound.WITCH_DEATH, "mob.witch.death");
    register(GenericSound.ZOMBIE_HURT, "mob.zombie.hurt");
    register(GenericSound.ZOMBIE_DEATH, "mob.zombie.death");
    register(GenericSound.PLAYER_HURT, "game.player.hurt");
    register(GenericSound.PLAYER_DEATH, "game.player.die");
    register(GenericSound.WOLF_HURT, "mob.wolf.hurt");
    register(GenericSound.WOLF_DEATH, "mob.wolf.death");
    register(GenericSound.VILLAGER_HURT, "mob.villager.hit");
    register(GenericSound.VILLAGER_DEATH, "mob.villager.death");
    register(GenericSound.PIG_DEATH, "mob.pig.death");
    register(GenericSound.OCELOT_HURT, "mob.cat.hitt");
    register(GenericSound.HORSE_HURT, "mob.horse.hit");
    register(GenericSound.DONKEY_HURT, "mob.horse.donkey.hit");
    register(GenericSound.ZOMBIE_HORSE_HURT, "mob.horse.zombie.hit");
    register(GenericSound.SKELETON_HORSE_HURT, "mob.horse.skeleton.hit");
    register(GenericSound.HORSE_DEATH, "mob.horse.death");
    register(GenericSound.DONKEY_DEATH, "mob.horse.donkey.death");
    register(GenericSound.ZOMBIE_HORSE_DEATH, "mob.horse.zombie.death");
    register(GenericSound.SKELETON_HORSE_DEATH, "mob.horse.skeleton.death");
    register(GenericSound.COW_HURT, "mob.cow.hurt");
    register(GenericSound.CHICKEN_HURT, "mob.chicken.hurt");
    register(GenericSound.BAT_HURT, "mob.bat.hurt");
    register(GenericSound.BAT_DEATH, "mob.bat.death");
    register(GenericSound.RABBIT_HURT, "mob.rabbit.hurt");
    register(GenericSound.RABBIT_HOP, "mob.rabbit.hop");
    register(GenericSound.RABBIT_IDLE, "mob.rabbit.idle");
    register(GenericSound.RABBIT_DEATH, "mob.rabbit.death");
    register(GenericSound.MOB_ATTACK, "mob.attack");
    
    register(NoteBlockValueType.HARP, Integer.valueOf(0));
    register(NoteBlockValueType.DOUBLE_BASS, Integer.valueOf(1));
    register(NoteBlockValueType.SNARE_DRUM, Integer.valueOf(2));
    register(NoteBlockValueType.HI_HAT, Integer.valueOf(3));
    register(NoteBlockValueType.BASS_DRUM, Integer.valueOf(4));
    
    register(PistonValueType.PUSHING, Integer.valueOf(0));
    register(PistonValueType.PULLING, Integer.valueOf(1));
    
    register(MobSpawnerValueType.RESET_DELAY, Integer.valueOf(1));
    
    register(ChestValueType.VIEWING_PLAYER_COUNT, Integer.valueOf(1));
    
    register(GenericBlockValueType.GENERIC, Integer.valueOf(1));
    
    register(PistonValue.DOWN, Integer.valueOf(0));
    register(PistonValue.UP, Integer.valueOf(1));
    register(PistonValue.SOUTH, Integer.valueOf(2));
    register(PistonValue.WEST, Integer.valueOf(3));
    register(PistonValue.NORTH, Integer.valueOf(4));
    register(PistonValue.EAST, Integer.valueOf(5));
    
    register(SoundEffect.CLICK, Integer.valueOf(1000));
    register(SoundEffect.EMPTY_DISPENSER_CLICK, Integer.valueOf(1001));
    register(SoundEffect.FIRE_PROJECTILE, Integer.valueOf(1002));
    register(SoundEffect.DOOR, Integer.valueOf(1003));
    register(SoundEffect.FIZZLE, Integer.valueOf(1004));
    register(SoundEffect.PLAY_RECORD, Integer.valueOf(1005));
    register(SoundEffect.GHAST_CHARGE, Integer.valueOf(1007));
    register(SoundEffect.GHAST_FIRE, Integer.valueOf(1008));
    register(SoundEffect.BLAZE_FIRE, Integer.valueOf(1009));
    register(SoundEffect.POUND_WOODEN_DOOR, Integer.valueOf(1010));
    register(SoundEffect.POUND_METAL_DOOR, Integer.valueOf(1011));
    register(SoundEffect.BREAK_WOODEN_DOOR, Integer.valueOf(1012));
    register(SoundEffect.WITHER_SPAWN, Integer.valueOf(1013));
    register(SoundEffect.WITHER_SHOOT, Integer.valueOf(1014));
    register(SoundEffect.BAT_TAKE_OFF, Integer.valueOf(1015));
    register(SoundEffect.INFECT_VILLAGER, Integer.valueOf(1016));
    register(SoundEffect.DISINFECT_VILLAGER, Integer.valueOf(1017));
    register(SoundEffect.ENDER_DRAGON_DEATH, Integer.valueOf(1018));
    register(SoundEffect.ANVIL_BREAK, Integer.valueOf(1020));
    register(SoundEffect.ANVIL_USE, Integer.valueOf(1021));
    register(SoundEffect.ANVIL_LAND, Integer.valueOf(1022));
    
    register(ParticleEffect.SMOKE, Integer.valueOf(2000));
    register(ParticleEffect.BREAK_BLOCK, Integer.valueOf(2001));
    register(ParticleEffect.BREAK_SPLASH_POTION, Integer.valueOf(2002));
    register(ParticleEffect.BREAK_EYE_OF_ENDER, Integer.valueOf(2003));
    register(ParticleEffect.MOB_SPAWN, Integer.valueOf(2004));
    register(ParticleEffect.BONEMEAL_GROW, Integer.valueOf(2005));
    register(ParticleEffect.HARD_LANDING_DUST, Integer.valueOf(2006));
    
    register(SmokeEffectData.SOUTH_EAST, Integer.valueOf(0));
    register(SmokeEffectData.SOUTH, Integer.valueOf(1));
    register(SmokeEffectData.SOUTH_WEST, Integer.valueOf(2));
    register(SmokeEffectData.EAST, Integer.valueOf(3));
    register(SmokeEffectData.UP, Integer.valueOf(4));
    register(SmokeEffectData.WEST, Integer.valueOf(5));
    register(SmokeEffectData.NORTH_EAST, Integer.valueOf(6));
    register(SmokeEffectData.NORTH, Integer.valueOf(7));
    register(SmokeEffectData.NORTH_WEST, Integer.valueOf(8));
    
    register(NameTagVisibility.ALWAYS, "always");
    register(NameTagVisibility.NEVER, "never");
    register(NameTagVisibility.HIDE_FOR_OTHER_TEAMS, "hideForOtherTeams");
    register(NameTagVisibility.HIDE_FOR_OWN_TEAM, "hideForOwnTeam");
    
    register(TeamColor.NONE, Integer.valueOf(-1));
    register(TeamColor.BLACK, Integer.valueOf(0));
    register(TeamColor.DARK_BLUE, Integer.valueOf(1));
    register(TeamColor.DARK_GREEN, Integer.valueOf(2));
    register(TeamColor.DARK_AQUA, Integer.valueOf(3));
    register(TeamColor.DARK_RED, Integer.valueOf(4));
    register(TeamColor.DARK_PURPLE, Integer.valueOf(5));
    register(TeamColor.GOLD, Integer.valueOf(6));
    register(TeamColor.GRAY, Integer.valueOf(7));
    register(TeamColor.DARK_GRAY, Integer.valueOf(8));
    register(TeamColor.BLUE, Integer.valueOf(9));
    register(TeamColor.GREEN, Integer.valueOf(10));
    register(TeamColor.AQUA, Integer.valueOf(11));
    register(TeamColor.RED, Integer.valueOf(12));
    register(TeamColor.LIGHT_PURPLE, Integer.valueOf(13));
    register(TeamColor.YELLOW, Integer.valueOf(14));
    register(TeamColor.WHITE, Integer.valueOf(15));
    
    register(ScoreType.INTEGER, "integer");
    register(ScoreType.HEARTS, "hearts");
    
    register(WorldBorderAction.SET_SIZE, Integer.valueOf(0));
    register(WorldBorderAction.LERP_SIZE, Integer.valueOf(1));
    register(WorldBorderAction.SET_CENTER, Integer.valueOf(2));
    register(WorldBorderAction.INITIALIZE, Integer.valueOf(3));
    register(WorldBorderAction.SET_WARNING_TIME, Integer.valueOf(4));
    register(WorldBorderAction.SET_WARNING_BLOCKS, Integer.valueOf(5));
    
    register(PlayerListEntryAction.ADD_PLAYER, Integer.valueOf(0));
    register(PlayerListEntryAction.UPDATE_GAMEMODE, Integer.valueOf(1));
    register(PlayerListEntryAction.UPDATE_LATENCY, Integer.valueOf(2));
    register(PlayerListEntryAction.UPDATE_DISPLAY_NAME, Integer.valueOf(3));
    register(PlayerListEntryAction.REMOVE_PLAYER, Integer.valueOf(4));
    
    register(TitleAction.TITLE, Integer.valueOf(0));
    register(TitleAction.SUBTITLE, Integer.valueOf(1));
    register(TitleAction.TIMES, Integer.valueOf(2));
    register(TitleAction.CLEAR, Integer.valueOf(3));
    register(TitleAction.RESET, Integer.valueOf(4));
    
    register(ResourcePackStatus.SUCCESSFULLY_LOADED, Integer.valueOf(0));
    register(ResourcePackStatus.DECLINED, Integer.valueOf(1));
    register(ResourcePackStatus.FAILED_DOWNLOAD, Integer.valueOf(2));
    register(ResourcePackStatus.ACCEPTED, Integer.valueOf(3));
  }
  
  private static void register(Enum<?> key, Object value)
  {
    values.put(key, value);
  }
  
  public static <T extends Enum<?>> T key(Class<T> keyType, Object value)
  {
    for (Enum<?> key : values.keySet())
    {
      Object val = values.get(key);
      if (keyType.isAssignableFrom(key.getClass()))
      {
        if ((val == value) || (val.equals(value))) {
          return key;
        }
        if ((Number.class.isAssignableFrom(val.getClass())) && (Number.class.isAssignableFrom(value.getClass())))
        {
          Number num = (Number)val;
          Number num2 = (Number)value;
          if (num.doubleValue() == num2.doubleValue()) {
            return key;
          }
        }
      }
    }
    return null;
  }
  
  public static <T> T value(Class<T> valueType, Enum<?> key)
  {
    Object val = values.get(key);
    if (val != null)
    {
      if (valueType.isAssignableFrom(val.getClass())) {
        return (T)val;
      }
      if (Number.class.isAssignableFrom(val.getClass()))
      {
        if (valueType == Byte.class) {
          return Byte.valueOf(((Number)val).byteValue());
        }
        if (valueType == Short.class) {
          return Short.valueOf(((Number)val).shortValue());
        }
        if (valueType == Integer.class) {
          return Integer.valueOf(((Number)val).intValue());
        }
        if (valueType == Long.class) {
          return Long.valueOf(((Number)val).longValue());
        }
        if (valueType == Float.class) {
          return Float.valueOf(((Number)val).floatValue());
        }
        if (valueType == Double.class) {
          return Double.valueOf(((Number)val).doubleValue());
        }
      }
    }
    return null;
  }
}
