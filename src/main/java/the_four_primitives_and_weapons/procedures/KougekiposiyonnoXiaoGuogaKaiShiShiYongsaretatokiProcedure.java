package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class KougekiposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putDouble("local", Math.toRadians(entity.getYRot()));
		entity.getPersistentData().putDouble("local1", Math.toRadians(entity.getYRot() + 180));
		entity.getPersistentData().putDouble("helmet", (Mth.nextDouble(RandomSource.create(), -180, 180)));
		entity.getPersistentData().putDouble("X", x);
		entity.getPersistentData().putDouble("Ypos", y);
		entity.getPersistentData().putDouble("Z", z);
		entity.getPersistentData().putDouble("dis", 0);
		entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
		entity.getPersistentData().putDouble("distance", 3);
	}
}
