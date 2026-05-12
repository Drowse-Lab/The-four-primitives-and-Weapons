package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class EnmazangekiOnEffectActiveTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double random = 0;
		double loop = 0;
		double XRadius2 = 0;
		double ZRadius2 = 0;
		double X = 0;
		double Y = 0;
		double Z = 0;
		double Y_pos = 0;
		double yknockback = 0;
		double dis = 0;
		double zknockback = 0;
		double r = 0;
		double alpha = 0;
		double xknockback = 0;
		double beta = 0;
		double count = 0;
		double count1 = 0;
		loop = entity.getPersistentData().getDouble("local");
		XRadius2 = 3;
		ZRadius2 = 3;
		Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
		r = 1;
		alpha = entity.getYRot();
		beta = entity.getXRot();
		for (int index0 = 0; index0 < 50; index0++) {
			count = 0;
			count1 = 0;
			for (int index1 = 0; index1 < 10; index1++) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CRIT, (x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha)) + count), ((y + 1) - r * Math.sin(Math.toRadians(beta))),
							(z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha)) + count1), 2, 0.1, 0.1, 0.1, 0.1);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CRIT, (x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha)) + count1), ((y + 1) - r * Math.sin(Math.toRadians(beta))),
							(z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha)) + count), 2, 0.1, 0.1, 0.1, 0.1);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CRIT, (x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha)) + count), ((y + 1) - r * Math.sin(Math.toRadians(beta))),
							(z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha)) + count), 2, 0.1, 0.1, 0.1, 0.1);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CRIT, (x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha)) + count1), ((y + 1) - r * Math.sin(Math.toRadians(beta))),
							(z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha)) + count1), 2, 0.1, 0.1, 0.1, 0.1);
				count = count + 1;
				count1 = count1 - 1;
			}
			r = r + 0.2;
		}
	}
}
