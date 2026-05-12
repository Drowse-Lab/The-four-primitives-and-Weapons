
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.GameRules;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TheFourPrimitivesAndWeaponsModGameRules {
	public static final GameRules.Key<GameRules.IntegerValue> QUEST_TIME_DURATION = GameRules.register("questTimeDuration", GameRules.Category.MISC, GameRules.IntegerValue.create(72000));
	public static final GameRules.Key<GameRules.BooleanValue> RPG_BOOK_GIVE = GameRules.register("rpgBookGive", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
}
