package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

/** 遠距離武器切断。旧5段階走査とkillコマンドを1回の範囲検索へ統合。 */
public class LongRangeWeaponCutehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity instanceof LivingEntity living) {
			SliceGuardOnEffectActiveTickProcedure.guardProjectiles(world, x, y, z, living, 5.0);
		}
	}
}
