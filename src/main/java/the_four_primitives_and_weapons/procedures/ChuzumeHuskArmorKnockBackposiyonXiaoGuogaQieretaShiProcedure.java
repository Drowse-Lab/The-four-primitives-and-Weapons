package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class ChuzumeHuskArmorKnockBackposiyonXiaoGuogaQieretaShiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK).setBaseValue((entity.getPersistentData().getDouble("the_four_primitives_and_weapons:chuzume_husk_armor_effect_knockback")));
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue((entity.getPersistentData().getDouble("the_four_primitives_and_weapons:chuzume_husk_armor_effect_damage")));
	}
}
