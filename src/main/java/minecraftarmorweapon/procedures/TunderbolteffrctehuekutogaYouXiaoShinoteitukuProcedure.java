package minecraftarmorweapon.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level().ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class TunderbolteffrctehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;

		// 毎tick FLASHパーティクルを表示（雷が来る予兆）
		if (entity.level() instanceof ServerLevel serverLevel) {
			// FLASHパーティクル（白い閃光）
			serverLevel.sendParticles(ParticleTypes.FLASH,
				entity.getX(),
				entity.getY() + entity.getBbHeight() / 2,
				entity.getZ(),
				2, // パーティクル数
				0.3, 0.5, 0.3, // 範囲
				0);

			// ELECTRIC_SPARKパーティクル（電気の火花）
			serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				entity.getX(),
				entity.getY() + entity.getBbHeight() / 2,
				entity.getZ(),
				3,
				0.3, 0.5, 0.3,
				0.02);
		}
	}
}
