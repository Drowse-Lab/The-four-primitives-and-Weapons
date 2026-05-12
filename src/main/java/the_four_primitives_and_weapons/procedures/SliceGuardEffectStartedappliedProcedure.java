package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class SliceGuardEffectStartedappliedProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double dis4 = 0;
		double dis3 = 0;
		double dis2 = 0;
		double dis1 = 0;
		if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MobEffects.DAMAGE_RESISTANCE) : false) {
			entity.getPersistentData().putBoolean("the_four_primitives_and_weapons:muteki_the_four_primitives_and_weapons:slice_guard_muteki_resistance_copy_chuzume_copy", true);
			entity.getPersistentData().putDouble("the_four_primitives_and_weapons:slice_guard_muteki_resistance_level",
					(entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? _livEnt.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() : 0));
			entity.getPersistentData().putDouble("the_four_primitives_and_weapons:slice_guard_muteki_resistance_time",
					(entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? _livEnt.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration() : 0));
		}
		entity.getPersistentData().putDouble("the_four_primitives_and_weapons:slice_guard_muteki_knockback_resistance", ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).getBaseValue());
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
					entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get()) ? _livEnt.getEffect(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get()).getDuration() : 0, 5, true, false));
		TheFourPrimitivesAndWeaponsMod.queueServerWork(entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get()) ? _livEnt.getEffect(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get()).getDuration() : 0, () -> {
			GuardposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure.killNearestGuardArmorStand(entity);
			if (entity.getPersistentData().getBoolean("the_four_primitives_and_weapons:muteki_the_four_primitives_and_weapons:slice_guard_muteki_resistance_copy_chuzume_copy") == true) {
				entity.getPersistentData().putDouble("the_four_primitives_and_weapons:slice_guard_muteki_resistance_level",
						(entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? _livEnt.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() : 0));
				entity.getPersistentData().putDouble("the_four_primitives_and_weapons:slice_guard_muteki_resistance_time",
						(entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? _livEnt.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration() : 0));
				entity.getPersistentData().putBoolean("the_four_primitives_and_weapons:muteki_the_four_primitives_and_weapons:slice_guard_muteki_resistance_copy_chuzume_copy", false);
			}
			((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue((entity.getPersistentData().getDouble("the_four_primitives_and_weapons:slice_guard_muteki_knockback_resistance")));
		});
	}
}
