package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

public class DarknessaitemuwoShoudeChituteiruJiannoteitukuProcedure {
	// 軽量化:
	//   旧: 毎 tick (=20Hz) で 2-tick の BlackHoleEffect を addEffect → 毎 tick 上書きで負荷大。
	//   新: 5 tick (4Hz) に 1 回 / duration を 10 tick (0.5秒) に拡張して同等の "持っている間継続" を維持。
	public static void execute(Entity entity) {
		if (entity == null) return;
		if (!(entity instanceof LivingEntity living)) return;
		if (living.level().isClientSide()) return;
		if ((living.tickCount % 5) != 0) return;

		ItemStack main = living.getMainHandItem();
		ItemStack off  = living.getOffhandItem();
		boolean holding = main.getItem() == TheFourPrimitivesAndWeaponsModItems.DARKNESS.get()
				|| off.getItem() == TheFourPrimitivesAndWeaponsModItems.DARKNESS.get();
		if (!holding) return;

		// 既に十分残ってる効果は再付与しない (毎周期の addEffect オーバーヘッド削減)
		MobEffectInstance existing = living.getEffect(TheFourPrimitivesAndWeaponsModMobEffects.BLACK_HOLE_EFFECT.get());
		if (existing != null && existing.getAmplifier() >= 1 && existing.getDuration() > 5) return;

		living.addEffect(new MobEffectInstance(
				TheFourPrimitivesAndWeaponsModMobEffects.BLACK_HOLE_EFFECT.get(),
				10, 1, true, false));
	}
}
