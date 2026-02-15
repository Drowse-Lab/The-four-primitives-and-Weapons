package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class OtiruyoenteiteigasuponsitaShiProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.OTIRO.get()) : false)) {
			for (int index0 = 0; index0 < 10; index0++) {
				MinecraftArmorWeaponMod.queueServerWork(1, () -> {
					entity.setDeltaMovement(new Vec3(0, 0, 0));
				});
				entity.setDeltaMovement(new Vec3(0, 0, 0));
			}
		}
		MinecraftArmorWeaponMod.queueServerWork(2, () -> {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.OTIRO.get(), 2, 1, true, false));
		});
	}
}
