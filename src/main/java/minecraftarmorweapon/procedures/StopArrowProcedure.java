package minecraftarmorweapon.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import minecraftarmorweapon.network.MinecraftArmorWeaponModVariables;

import javax.annotation.Nullable;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

@Mod.EventBusSubscriber
public class StopArrowProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player.getX(), event.player.getY(), event.player.getZ(), event.player);
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double dis4 = 0;
		double dis3 = 0;
		double dis2 = 0;
		double dis1 = 0;
		double dis = 0;
		if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).noa > 50) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(20 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
						.collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!(entityiterator == entity)) {
						if ((entityiterator instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ? _livEnt.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() : 0) < 90) {
							if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10000, 100, false, false));
						}
						if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
								|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof SmallFireball) {
							if (!entityiterator.isNoGravity()) {
								entityiterator.setNoGravity(true);
								entityiterator.setDeltaMovement(new Vec3((0.001 * entity.getDeltaMovement().x()), (0.001 * entity.getDeltaMovement().y()), (0.001 * entity.getDeltaMovement().z())));
							}
							if (entityiterator.isNoGravity()) {
								entityiterator.setDeltaMovement(new Vec3((1.005 * entity.getDeltaMovement().x()), (1.005 * entity.getDeltaMovement().y()), (1.005 * entity.getDeltaMovement().z())));
							}
						}
					}
				}
			}
		} else if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).noa < 50) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(100 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
						.collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if ((entityiterator instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ? _livEnt.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() : 0) > 10) {
						if (entityiterator instanceof LivingEntity _entity)
							_entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
					}
					if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
							|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof SmallFireball) {
						if (entityiterator.isNoGravity()) {
							entityiterator.setDeltaMovement(new Vec3((1000 * entity.getDeltaMovement().x()), (1000 * entity.getDeltaMovement().y()), (1000 * entity.getDeltaMovement().z())));
							entityiterator.setNoGravity(false);
						}
					}
				}
			}
			{
				double _setval = 0;
				entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.noa = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
	}
}
