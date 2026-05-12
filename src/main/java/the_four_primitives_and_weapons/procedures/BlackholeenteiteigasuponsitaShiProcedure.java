package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class BlackholeenteiteigasuponsitaShiProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		TheFourPrimitivesAndWeaponsMod.queueServerWork(400, () -> {
			if (!entity.level().isClientSide())
				entity.discard();
		});
	}
}
