package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class NannkasouNightVisionEffectExpiresProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.NIGHT_VISION);
		MinecraftArmorWeaponMod.queueServerWork(2, () -> {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, (int) entity.getPersistentData().getDouble("minecraft_armor_weapon_sword_night_vision_tick"),
						(int) entity.getPersistentData().getDouble("minecraft_armor_weapon_sword_night_vision_lavel"), false, false));
		});
	}
}
