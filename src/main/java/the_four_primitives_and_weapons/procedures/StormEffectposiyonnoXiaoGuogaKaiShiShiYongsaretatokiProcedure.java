package the_four_primitives_and_weapons.procedures;

import net.minecraftforge.common.ForgeMod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;

public class StormEffectposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.TUNDERBOLTEFFRCT.get());
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.BUBBLESHOT_EFFECT.get());
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.WIND_STEP_EFFECT.get());
		entity.getPersistentData().putDouble("stormeffectattackspeed", ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).getBaseValue());
		entity.getPersistentData().putDouble("stormeffectmovementspeed", ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue());
		entity.getPersistentData().putDouble("stormeffectswimspeed", ((LivingEntity) entity).getAttribute(ForgeMod.SWIM_SPEED.get()).getBaseValue());
		entity.getPersistentData().putDouble("stormeffectattackdamege", ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue());
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).getBaseValue() + 2));
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
				.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue() + 0.1));
		((LivingEntity) entity).getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue((((LivingEntity) entity).getAttribute(ForgeMod.SWIM_SPEED.get()).getBaseValue() + 10));
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
				.setBaseValue((((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() + 5));
	}
}
