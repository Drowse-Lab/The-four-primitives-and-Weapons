package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

public class SliceGuardOnEffectActiveTickProcedure {
	private static final String LAST_BLOCK_EFFECT_TICK = "SliceGuardLastBlockEffectTick";
	private static final MobEffect[] REMOVED_DEBUFFS = {
			MobEffects.WITHER, MobEffects.BLINDNESS, MobEffects.HUNGER,
			MobEffects.LEVITATION, MobEffects.DIG_SLOWDOWN, MobEffects.CONFUSION,
			MobEffects.POISON, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.HARM,
			MobEffects.WEAKNESS
	};

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof LivingEntity living) || living.level().isClientSide())
			return;

		guardProjectiles(world, x, y, z, living, 2.5);

		for (MobEffect debuff : REMOVED_DEBUFFS) {
			living.removeEffect(debuff);
		}
	}

	/** 範囲検索、飛び道具破棄、演出をSliceGuard系で共有する。 */
	public static boolean guardProjectiles(LevelAccessor world, double x, double y, double z, LivingEntity living, double radius) {
		if (living.level().isClientSide())
			return false;
		AABB area = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
		boolean blockedProjectile = false;
		for (Entity candidate : world.getEntitiesOfClass(Entity.class, area, SliceGuardOnEffectActiveTickProcedure::isGuardedProjectile)) {
			if (candidate.getPersistentData().getBoolean("Check"))
				continue;
			candidate.getPersistentData().putBoolean("Check", true);
			candidate.getPersistentData().putBoolean("Check2", true);
			candidate.discard();
			blockedProjectile = true;
		}

		if (blockedProjectile) {
			playBlockEffect(living);
		}
		return blockedProjectile;
	}

	/** Kitterukitteru の旧effect処理を置き換える、共通の軽量な弾き演出。 */
	public static void playBlockEffect(Entity entity) {
		if (!(entity instanceof LivingEntity living) || !(living.level() instanceof ServerLevel level))
			return;
		long gameTime = level.getGameTime();
		if (living.getPersistentData().getLong(LAST_BLOCK_EFFECT_TICK) == gameTime)
			return;
		living.getPersistentData().putLong(LAST_BLOCK_EFFECT_TICK, gameTime);
		double centerY = living.getY() + living.getBbHeight() * 0.55;
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, living.getX(), centerY, living.getZ(), 4, 0.65, 0.35, 0.65, 0.02);
		level.sendParticles(ParticleTypes.CRIT, living.getX(), centerY, living.getZ(), 6, 0.7, 0.45, 0.7, 0.08);
	}

	private static boolean isGuardedProjectile(Entity entity) {
		return entity instanceof Arrow || entity instanceof SpectralArrow
				|| entity instanceof ThrownTrident || entity instanceof LargeFireball
				|| entity instanceof DragonFireball || entity instanceof Snowball
				|| entity instanceof ThrownEgg || entity instanceof WitherSkull
				|| entity instanceof ShulkerBullet || entity instanceof LlamaSpit
				|| entity instanceof ThrownEnderpearl;
	}
}
