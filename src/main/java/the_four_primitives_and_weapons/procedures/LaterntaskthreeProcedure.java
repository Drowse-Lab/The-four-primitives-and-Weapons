package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import the_four_primitives_and_weapons.network.TheFourPrimitivesAndWeaponsModVariables;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

public class LaterntaskthreeProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if ((TheFourPrimitivesAndWeaponsModVariables.MapVariables.get(world).questTaskThree).equals("Craft 64 Iron Nuggets")
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerQuestTaskThreeNumber < 64
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerHasCompletedQuestTaskThree == false
				&& itemstack.getItem() == TheFourPrimitivesAndWeaponsModItems.GOLD_KATANA.get()) {
			{
				double _setval = (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerQuestTaskThreeNumber + (itemstack).getCount();
				entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerQuestTaskThreeNumber = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
		if ((TheFourPrimitivesAndWeaponsModVariables.MapVariables.get(world).questTaskThree).equals("Craft 64 Iron Nuggets")
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerQuestTaskThreeNumber >= 64
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerHasCompletedQuestTaskThree == false) {
			{
				boolean _setval = true;
				entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerHasCompletedQuestTaskThree = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
	}
}
