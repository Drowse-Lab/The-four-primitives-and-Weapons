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
			"kise", "kizami", "ishime", "same", "kuroro", "roiro", "shunuri", "tame", "gunto"
	};

	public static boolean isSupported(ItemStack in) {
		if (in.isEmpty()) return false;
		return KatanaFittings.isFittingWeapon(in) || SayaDesign.isSaya(in);
	}

	/** 入力に対する見た目候補 ( 先頭は「既定」)。 対象外なら空。 */
	public static List<ItemStack> candidatesFor(ItemStack in) {
		return candidatesFor(in, ItemStack.EMPTY);
	}

	/**
	 * 入力(+染料)に対する候補。
	 * <ul>
	 *   <li>染料あり: <b>部位ごとの色変更</b> ( 刀=柄/鍔/縁/頭 を個別に染料色へ、 鞘=地色 )。</li>
	 *   <li>染料なし: 見た目 ( 刀=柄巻きデザイン、 鞘=仕立て )。</li>
	 * </ul>
	 * 大釜は「一気に全部同色」なのに対し、 拵え台は「部位ごとに部分的に」変えられる。
	 */
	public static List<ItemStack> candidatesFor(ItemStack in, ItemStack dye) {
		List<ItemStack> out = new ArrayList<>();
		if (in.isEmpty()) return out;
		boolean hasDye = dye != null && dye.getItem() instanceof net.minecraft.world.item.DyeItem;

		if (KatanaFittings.isFittingWeapon(in)) {
			if (hasDye) {
				int rgb = KatanaFittings.dyeRgb(((net.minecraft.world.item.DyeItem) dye.getItem()).getDyeColor());
				out.add(katanaColor(in, "tsuka", rgb, "柄を染める"));
				out.add(katanaColor(in, "tsuba", rgb, "鍔を染める"));
				out.add(katanaColor(in, "kashira", rgb, "頭を染める"));
			} else {
				out.add(katana(in, ""));                 // 既定 ( 元の柄 )
				for (String w : KatanaFittings.WRAPS) out.add(katana(in, w));
			}
		} else if (SayaDesign.isSaya(in)) {
			if (hasDye) {
				int rgb = SayaDesign.dyeRgb(((net.minecraft.world.item.DyeItem) dye.getItem()).getDyeColor());
				ItemStack s = in.copy(); s.setCount(1);
				SayaDesign.setBaseColorRgb(s, rgb);
				s.setHoverName(net.minecraft.network.chat.Component.literal("地色を染める"));
				out.add(s);
			} else {
				out.add(saya(in, ""));                   // 既定 ( 塗鞘 )
				for (String st : SAYA_STYLES) out.add(saya(in, st));
				for (String wood : SayaStyles.WOODS) out.add(saya(in, "wood:minecraft:" + wood + "_planks"));
			}
		}
		return out;
	}

	private static ItemStack katanaColor(ItemStack in, String part, int rgb, String label) {
		ItemStack s = in.copy();
		s.setCount(1);
		switch (part) {
			case "tsuba":   KatanaFittings.setTsuba(s, rgb); break;
			case "fuchi":   KatanaFittings.setFuchi(s, rgb); break;
			case "kashira": KatanaFittings.setKashira(s, rgb); break;
			default:        KatanaFittings.setTsuka(s, rgb);
		}
		s.setHoverName(net.minecraft.network.chat.Component.literal(label));
		return s;
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
