package minecraftarmorweapon.procedures;

import net.minecraft.world.entity.Entity;

public class AlchemyCraftBlockEntityRightClickedOnEntityProcedure {
	public static void execute(Entity sourceentity) {
		if (sourceentity == null)
			return;
		if (!sourceentity.level().isClientSide())
			sourceentity.discard();
	}
}
