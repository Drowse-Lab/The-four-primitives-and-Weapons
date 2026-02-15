package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import minecraftarmorweapon.network.MinecraftArmorWeaponModVariables;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

public class LaterntaskthreeProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if ((MinecraftArmorWeaponModVariables.MapVariables.get(world).questTaskThree).equals("Craft 64 Iron Nuggets")
				&& (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerQuestTaskThreeNumber < 64
				&& (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerHasCompletedQuestTaskThree == false
				&& itemstack.getItem() == MinecraftArmorWeaponModItems.GOLD_KATANA.get()) {
			{
				double _setval = (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerQuestTaskThreeNumber + (itemstack).getCount();
				entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerQuestTaskThreeNumber = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
		if ((MinecraftArmorWeaponModVariables.MapVariables.get(world).questTaskThree).equals("Craft 64 Iron Nuggets")
				&& (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerQuestTaskThreeNumber >= 64
				&& (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerHasCompletedQuestTaskThree == false) {
			{
				boolean _setval = true;
				entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerHasCompletedQuestTaskThree = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
	}
}
