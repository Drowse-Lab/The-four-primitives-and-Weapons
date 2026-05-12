package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;

public class TunderbolteffrctposiyonXiaoGuogaQieretaShiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;

		// エフェクトが切れた時（5秒経過後）に雷を落とす
		if (VersionHelper.getLevel(entity) instanceof ServerLevel serverLevel) {
			// 大量のFLASHパーティクル（雷が来る直前）
			serverLevel.sendParticles(ParticleTypes.FLASH,
				entity.getX(),
				entity.getY() + entity.getBbHeight() / 2,
				entity.getZ(),
				20, // 大量に
				0.5, 1.0, 0.5,
				0);

			// 雷を複数落とす（3本）
			for (int i = 0; i < 3; i++) {
				LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
				if (lightning != null) {
					double offsetX = (Math.random() - 0.5) * 2;
					double offsetZ = (Math.random() - 0.5) * 2;
					lightning.moveTo(Vec3.atBottomCenterOf(entity.blockPosition()
						.offset((int)offsetX, 0, (int)offsetZ)));
					lightning.setVisualOnly(false);
					serverLevel.addFreshEntity(lightning);
				}
			}

			// 電気エフェクト
			serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				entity.getX(), entity.getY() + 1, entity.getZ(),
				50, 1.0, 1.0, 1.0, 0.2);

			// 雷のサウンド
			serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0f, 1.0f);
		}
	}
}
