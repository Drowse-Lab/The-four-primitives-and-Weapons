package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class ZanngekitokubetuposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double loop = 0;
		double XRadius2 = 0;
		double ZRadius2 = 0;
		double X = 0;
		double Y = 0;
		double Z = 0;
		double Y_pos = 0;
		double a = 0;
		double b = 0;
		double c = 0;
		double y1 = 0;
		double x1 = 0;
		double z1 = 0;
		double y1pos = 0;
		double dis1 = 0;
		entity.getPersistentData().putDouble("local", Math.toRadians(entity.getYRot()));
		entity.getPersistentData().putDouble("helmet", (Mth.nextDouble(RandomSource.create(), -180, 180)));
	}
}
