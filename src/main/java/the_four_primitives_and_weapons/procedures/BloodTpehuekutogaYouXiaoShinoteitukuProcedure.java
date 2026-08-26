package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerPlayer;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.entity.SkeltonMobEntity;

/** 旧コードに残っていた到達不能な50ブロック走査とソートを除去。 */
public class BloodTpehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || entity.level().isClientSide() || !entity.getPersistentData().getBoolean("tp"))
			return;

		AABB area = new AABB(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
		for (LivingEntity nearby : world.getEntitiesOfClass(LivingEntity.class, area,
				candidate -> candidate != entity && !(candidate instanceof SkeltonMobEntity) && true)) {
			nearby.removeEffect(MobEffects.GLOWING);
		}

		entity.getPersistentData().putBoolean("tp", false);
		TheFourPrimitivesAndWeaponsMod.queueServerWork(4, () -> {
			if (entity.isAlive() && !entity.isRemoved())
				entity.getPersistentData().putBoolean("tp", true);
		});

		if (entity instanceof Player player && !isCreative(player)) {
			player.getAbilities().invulnerable = true;
			player.onUpdateAbilities();
		}
	}

	private static boolean isCreative(Player player) {
		return player instanceof ServerPlayer serverPlayer
				&& serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
	}
}
