package minecraftarmorweapon.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;

public class BloodBottlepureiyagaaitemuwoShiiZhongwaruProcedure {
public static void execute(Entity entity) {
	if (entity == null)
		return;

	if (entity instanceof ServerPlayer _plr0 && _plr0.level instanceof ServerLevel
			&& _plr0.getAdvancements().getOrStartProgress(_plr0.server.getAdvancements().getAdvancement(
					new ResourceLocation("minecraft_armor_weapon:you_have_become_a_vampire"))).isDone()) {

		if (entity instanceof LivingEntity _livingEntity) {
			if (!_livingEntity.level.isClientSide()) {
				_livingEntity.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 4, false, false)); // 満腹度+4
			}

			float currentHealth = _livingEntity.getHealth();
			float maxHealth = _livingEntity.getMaxHealth();

			if (currentHealth < maxHealth) {
				_livingEntity.setHealth(Math.min(currentHealth + 4.0f, maxHealth)); // 最大値超え防止
			}
		}
	} else {
		if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide()) {
			_entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 1, true, true));
		}
	}
}

}
