package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class OtiruyoenteiteigasuponsitaShiProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.OTIRO.get()) : false)) {
			for (int index0 = 0; index0 < 10; index0++) {
				TheFourPrimitivesAndWeaponsMod.queueServerWork(1, () -> {
					entity.setDeltaMovement(new Vec3(0, 0, 0));
				});
				entity.setDeltaMovement(new Vec3(0, 0, 0));
			}
		}
		TheFourPrimitivesAndWeaponsMod.queueServerWork(2, () -> {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.OTIRO.get(), 2, 1, true, false));
		});
	}
}
