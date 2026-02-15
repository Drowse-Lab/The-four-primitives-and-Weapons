package minecraftarmorweapon.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;

public class NkigaYasaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (!((entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.KURUTIMENASI.get()) : false)
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) : false) && (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MobEffects.BLINDNESS) : false)
						&& (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MobEffects.HUNGER) : false) && (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MobEffects.CONFUSION) : false))) {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.KURUTIMENASI.get(), 60, 1, true, false));
		}
	}
}
