package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class NannkasouNightVisionEffectStartedappliedProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MobEffects.NIGHT_VISION) : false) {
			entity.getPersistentData().putDouble("minecraft_armor_weapon_sword_night_vision_tick", (entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.NIGHT_VISION) ? _livEnt.getEffect(MobEffects.NIGHT_VISION).getDuration() : 0));
			entity.getPersistentData().putDouble("minecraft_armor_weapon_sword_night_vision_lavel",
					(entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.NIGHT_VISION) ? _livEnt.getEffect(MobEffects.NIGHT_VISION).getAmplifier() : 0));
		}
		MinecraftArmorWeaponMod.queueServerWork(
				entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.NANNKASOU_NIGHT_VISION.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.NANNKASOU_NIGHT_VISION.get()).getDuration() : 0, () -> {
					if (entity instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.NIGHT_VISION);
				});
	}
}
