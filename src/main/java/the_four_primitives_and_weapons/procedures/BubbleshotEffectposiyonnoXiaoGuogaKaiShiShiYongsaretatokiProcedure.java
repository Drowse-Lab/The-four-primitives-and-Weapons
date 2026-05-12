package the_four_primitives_and_weapons.procedures;

import net.minecraftforge.common.ForgeMod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class BubbleshotEffectposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putDouble("bubbleshoteswimspeed", ((LivingEntity) entity).getAttribute(ForgeMod.SWIM_SPEED.get()).getBaseValue());
		((LivingEntity) entity).getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue((((LivingEntity) entity).getAttribute(ForgeMod.SWIM_SPEED.get()).getBaseValue() + 9.5));
	}
}
