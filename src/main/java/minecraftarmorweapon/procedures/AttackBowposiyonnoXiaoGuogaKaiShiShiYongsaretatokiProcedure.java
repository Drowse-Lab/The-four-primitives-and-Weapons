package minecraftarmorweapon.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;

public class AttackBowposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _entity)
			_entity.hurt(_entity.damageSources().magic(), 10);
	}
}
