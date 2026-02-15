package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;

public class TameteruehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 1, 1, 1, 1);
		if (!(entity.getPersistentData().getBoolean("minecraft_armor_weapon:test_bow_tameteru_true_or_false") == true)) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(MinecraftArmorWeaponModMobEffects.TAMETERU.get());
		}
		TestBowAnimetionProcedure.execute(entity);
		entity.setPose(Pose.FALL_FLYING);
	}
}
