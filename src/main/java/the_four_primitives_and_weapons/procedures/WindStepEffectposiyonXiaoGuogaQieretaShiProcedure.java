package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class WindStepEffectposiyonXiaoGuogaQieretaShiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((entity.getPersistentData().getDouble("windstepmovementspeed")));
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).setBaseValue((entity.getPersistentData().getDouble("windstepattackspeed")));
	}
}
