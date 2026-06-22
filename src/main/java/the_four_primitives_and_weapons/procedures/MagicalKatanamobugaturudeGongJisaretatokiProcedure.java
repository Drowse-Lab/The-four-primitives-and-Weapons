package the_four_primitives_and_weapons.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * MagicalKatana で Mob を倒した時のイベント。
 * かつては kill 毎に MAGIC_MCRYSTAL を drop していたが、 ユーザー要望で無効化。
 *
 * 既存呼び出し (procedures registry 等) を壊さないために class とメソッドは残し、
 * 中身を no-op にしている。
 */
@Mod.EventBusSubscriber
public class MagicalKatanamobugaturudeGongJisaretatokiProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		// no-op: クリスタル drop を無効化
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		// no-op
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, Entity sourceentity) {
		// no-op
	}
}
