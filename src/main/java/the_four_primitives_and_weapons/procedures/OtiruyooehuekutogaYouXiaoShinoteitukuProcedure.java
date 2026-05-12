package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class OtiruyooehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		entity.fallDistance = 0;
		if (!entity.onGround()) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.OTIRUYOO.get(), 5, 1, true, false));
		}
		if (entity.onGround()) {
			TheFourPrimitivesAndWeaponsMod.queueServerWork(10, () -> {
				if (entity instanceof LivingEntity _entity)
					_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.OTIRUYOO.get());
			});
		}
	}
}
