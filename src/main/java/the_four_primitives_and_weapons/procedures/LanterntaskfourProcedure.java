package the_four_primitives_and_weapons.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import the_four_primitives_and_weapons.network.TheFourPrimitivesAndWeaponsModVariables;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModBlocks;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class LanterntaskfourProcedure {
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != event.getEntity().getUsedItemHand())
			return;
		execute(event, event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((TheFourPrimitivesAndWeaponsModVariables.MapVariables.get(world).questTaskFour).equals("Craft 8 Torches")
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerQuestTaskFourNumber < 1
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerHasCompletedQuestTaskFour == false
				&& (world.getBlockState(new BlockPos((int) (x), (int) (y), (int) (z)))).getBlock() == TheFourPrimitivesAndWeaponsModBlocks.CUSTOM_CRAFTER_CRAFTING.get()) {
			{
				double _setval = (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerQuestTaskFourNumber + 1;
				entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerQuestTaskFourNumber = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
		if ((TheFourPrimitivesAndWeaponsModVariables.MapVariables.get(world).questTaskFour).equals("Craft 8 Torches")
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerQuestTaskFourNumber >= 1
				&& (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).playerHasCompletedQuestTaskFour == false
				&& (world.getBlockState(new BlockPos((int) (x), (int) (y), (int) (z)))).getBlock() == TheFourPrimitivesAndWeaponsModBlocks.CUSTOM_CRAFTER_CRAFTING.get()) {
			{
				boolean _setval = true;
				entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerHasCompletedQuestTaskFour = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
	}
}
