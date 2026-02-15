package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class OtiruyooehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		entity.fallDistance = 0;
		if (!entity.isOnGround()) {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.OTIRUYOO.get(), 5, 1, true, false));
		}
		if (entity.isOnGround()) {
			MinecraftArmorWeaponMod.queueServerWork(10, () -> {
				if (entity instanceof LivingEntity _entity)
					_entity.removeEffect(MinecraftArmorWeaponModMobEffects.OTIRUYOO.get());
			});
		}
	}
}
