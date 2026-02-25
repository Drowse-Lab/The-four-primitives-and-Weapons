package minecraftarmorweapon.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;

public class DarknessAttackEffectehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.hurt(entity.damageSources().dragonBreath(), (float) Math
				.ceil((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get()).getAmplifier() : 0)
						/ 2));
	}
}
