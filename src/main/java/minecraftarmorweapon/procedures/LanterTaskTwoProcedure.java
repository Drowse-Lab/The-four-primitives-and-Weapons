package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import minecraftarmorweapon.network.MinecraftArmorWeaponModVariables;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

public class LanterTaskTwoProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if ((MinecraftArmorWeaponModVariables.MapVariables.get(world).questTaskTwo).equals("Smelt 8 Iron Ore")
				&& (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerQuestTaskTwoNumber < 8
				&& (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerHasCompletedQuestTaskTwo == false
				&& itemstack.getItem() == MinecraftArmorWeaponModItems.IRON_KATANA.get()) {
			{
				double _setval = (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerQuestTaskTwoNumber + (itemstack).getCount();
				entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerQuestTaskTwoNumber = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
		if ((MinecraftArmorWeaponModVariables.MapVariables.get(world).questTaskTwo).equals("Smelt 8 Iron Ore")
				&& (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerQuestTaskTwoNumber >= 8
				&& (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).playerHasCompletedQuestTaskTwo == false) {
			{
				boolean _setval = true;
				entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.playerHasCompletedQuestTaskTwo = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
	}
}
