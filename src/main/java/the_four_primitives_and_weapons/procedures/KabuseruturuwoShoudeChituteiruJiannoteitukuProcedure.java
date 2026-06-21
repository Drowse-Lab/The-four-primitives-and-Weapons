package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;

public class KabuseruturuwoShoudeChituteiruJiannoteitukuProcedure {
	// 軽量化:
	//   旧: 毎 tick で capability を 7 回取得し、 hoverName を設定した ItemStack を新規生成 → 即破棄 (常に dead allocation)。
	//   結局 player の手札には何も反映されないため、 処理を no-op 化して per-tick の負荷をゼロに。
	public static void execute(Entity entity) {
		// no-op: kabuseru の hoverName 表示は別の場所で更新される (item の getName 等)。
		// per-tick での無意味な ItemStack 生成を廃止。
	}
}
