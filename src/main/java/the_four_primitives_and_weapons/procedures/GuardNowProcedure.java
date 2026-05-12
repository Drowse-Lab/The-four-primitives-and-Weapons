package the_four_primitives_and_weapons.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.entity.CommonSoldierEntity;

import java.util.List;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class GuardNowProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingHurtEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(Entity entity, Entity sourceentity) {
		execute(null, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;

		// Skip A-Life AI entities (they don't have the required attributes)
		if (entity instanceof CommonSoldierEntity) {
			return;
		}

		if (sourceentity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get()) : false) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.ATTACK_IN_GUARD.get(), 60, 1, false, false));

			// パリィ成功: 攻撃者をノックバック
			if (entity instanceof LivingEntity attacker) {
				double knockX = -(attacker.getX() - sourceentity.getX());
				double knockZ = -(attacker.getZ() - sourceentity.getZ());
				double dist = Math.sqrt(knockX * knockX + knockZ * knockZ);
				if (dist > 0.01) {
					attacker.setDeltaMovement(attacker.getDeltaMovement().add(knockX / dist * 1.0, 0.3, knockZ / dist * 1.0));
					attacker.hurtMarked = true;
				}
			}

			// 周囲3ブロックの敵にSlowness II (2秒)
			if (sourceentity.level() instanceof ServerLevel serverLevel) {
				Vec3 pos = sourceentity.position();
				List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class,
						new AABB(pos, pos).inflate(3.0), e -> e != sourceentity && e.isAlive());
				for (LivingEntity mob : nearby) {
					mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true)); // Slowness II, 2s
				}

				// エフェクト
				serverLevel.sendParticles(ParticleTypes.CRIT, sourceentity.getX(), sourceentity.getY() + 1, sourceentity.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
				serverLevel.playSound(null, sourceentity.getX(), sourceentity.getY(), sourceentity.getZ(),
						SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS, 1.0f, 1.5f);
				serverLevel.playSound(null, sourceentity.getX(), sourceentity.getY(), sourceentity.getZ(),
						SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 2.0f);
			}
		}
	}
}
