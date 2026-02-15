package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class BlackholeenteiteigasuponsitaShiProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		MinecraftArmorWeaponMod.queueServerWork(400, () -> {
			if (!entity.level.isClientSide())
				entity.discard();
		});
	}
}
