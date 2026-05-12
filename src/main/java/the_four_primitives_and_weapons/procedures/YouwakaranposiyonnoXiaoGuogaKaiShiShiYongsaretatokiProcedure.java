package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class YouwakaranposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putDouble("local", (Math.toRadians(entity.getYRot()) - 180 * Mth.nextDouble(RandomSource.create(), 0, 1)));
		entity.getPersistentData().putDouble("angle", (entity.getXRot()));
	}
}
