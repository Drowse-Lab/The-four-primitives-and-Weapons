package the_four_primitives_and_weapons.util;

import net.minecraft.world.item.ItemStack;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 拵え台 ( KoshiraeBench ) の候補生成。 入力アイテムに応じて「選べる見た目」の一覧を作る。
 *
 * <ul>
 *   <li>刀 ( IRON_KATANA ): 柄巻きデザイン ( {@link KatanaFittings#WRAPS} ) ＋ 既定</li>
 *   <li>鞘 ( saya ): 仕立て ( 木目/着せ/刻/石目/鮫/漆各種 ) ＋ 既定</li>
 * </ul>
 *
 * <p>各候補は 入力アイテムのコピーに NBT を付けたもの ( 素材消費なしの「見た目替え」)。
 * 色 ( 染料 ) はここでは扱わない ( 作業台の染色レシピで )。</p>
 */
public final class KoshiraeFittings {

	private KoshiraeFittings() {}

	/** 鞘の仕立て候補 ( 漆/素地系 )。 木目は別途 全木材を追加。 */
	private static final String[] SAYA_STYLES = {
			"kise", "kizami", "ishime", "same", "kuroro", "roiro", "shunuri", "tame"
	};

	public static boolean isSupported(ItemStack in) {
		if (in.isEmpty()) return false;
		return in.getItem() == TheFourPrimitivesAndWeaponsModItems.IRON_KATANA.get()
				|| SayaDesign.isSaya(in);
	}

	/** 入力に対する見た目候補 ( 先頭は「既定」)。 対象外なら空。 */
	public static List<ItemStack> candidatesFor(ItemStack in) {
		List<ItemStack> out = new ArrayList<>();
		if (in.isEmpty()) return out;

		if (in.getItem() == TheFourPrimitivesAndWeaponsModItems.IRON_KATANA.get()) {
			out.add(katana(in, ""));                 // 既定 ( 元の柄 )
			for (String w : KatanaFittings.WRAPS) out.add(katana(in, w));
		} else if (SayaDesign.isSaya(in)) {
			out.add(saya(in, ""));                   // 既定 ( 塗鞘 )
			for (String s : SAYA_STYLES) out.add(saya(in, s));
			for (String wood : SayaStyles.WOODS) out.add(saya(in, "wood:minecraft:" + wood + "_planks"));
		}
		return out;
	}

	private static ItemStack katana(ItemStack in, String wrap) {
		ItemStack s = in.copy();
		s.setCount(1);
		KatanaFittings.setTsukaWrap(s, wrap);
		return s;
	}

	private static ItemStack saya(ItemStack in, String style) {
		ItemStack s = in.copy();
		s.setCount(1);
		SayaDesign.setStyle(s, style);
		return s;
	}
}
