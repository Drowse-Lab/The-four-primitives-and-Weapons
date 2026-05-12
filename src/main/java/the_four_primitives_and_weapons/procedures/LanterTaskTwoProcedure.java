package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import the_four_primitives_and_weapons.network.TheFourPrimitivesAndWeaponsModVariables;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

public class LanterTaskTwoProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if ((TheFourPrimitivesAndWeaponsModVariables.MapVariables.get(world).questTaskTwo).equals("Smelt 8 Iron Ore")
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerQuestTaskTwoNumber < 8
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerHasCompletedQuestTaskTwo == false
				&& itemstack.getItem() == TheFourPrimitivesAndWeaponsModItems.IRON_KATANA.get()) {
			{
				double _setval = (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerQuestTaskTwoNumber + (itemstack).getCount();
				entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerQuestTaskTwoNumber = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
		if ((TheFourPrimitivesAndWeaponsModVariables.MapVariables.get(world).questTaskTwo).equals("Smelt 8 Iron Ore")
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerQuestTaskTwoNumber >= 8
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerHasCompletedQuestTaskTwo == false) {
			{
				boolean _setval = true;
				entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerHasCompletedQuestTaskTwo = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
	}
}
