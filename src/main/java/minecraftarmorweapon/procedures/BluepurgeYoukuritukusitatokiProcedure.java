package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.core.particles.ParticleTypes;

public class BluepurgeYoukuritukusitatokiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		ItemStack pickaxe = ItemStack.EMPTY;
		double previousRecipe = 0;
		double EnchtSize = 0;
		double i = 0;
		double j = 0;
		double k = 0;
		double particleRadius = 0;
		double particleAmount = 0;
		particleAmount = 8;
		particleRadius = 2;
		for (int index0 = 0; index0 < (int) particleAmount; index0++) {
			world.addParticle(ParticleTypes.SWEEP_ATTACK, (x + 0 + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius), (y + 0 + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
					(z + 0 + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius), (Mth.nextDouble(RandomSource.create(), -0.001, 0.001)), (Mth.nextDouble(RandomSource.create(), -0.001, 0.001)),
					(Mth.nextDouble(RandomSource.create(), -0.001, 0.001)));
		}
	}
}
