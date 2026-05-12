package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;

public class DarknessAttackEffectehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.hurt(entity.damageSources().dragonBreath(), (float) Math
				.ceil((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.DARKNESS_ATTACK_EFFECT.get()) ? _livEnt.getEffect(TheFourPrimitivesAndWeaponsModMobEffects.DARKNESS_ATTACK_EFFECT.get()).getAmplifier() : 0)
						/ 2));
	}
}
