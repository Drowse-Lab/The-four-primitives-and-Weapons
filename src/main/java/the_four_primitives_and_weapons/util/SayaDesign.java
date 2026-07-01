package the_four_primitives_and_weapons.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BannerBlockEntity;

/**
 * 鞘 ( saya ) の意匠を扱うヘルパー。
 *
 *   - 地の色 ( Base ): NBT "SayaBase" に <b>任意の RGB ( 0xRRGGBB )</b> を保存。
 *     作業台での染色 ( {@link the_four_primitives_and_weapons.item.SayaDyeRecipe} ) は単色、
 *     大釜での染色 ( {@link the_four_primitives_and_weapons.event.CauldronDyeHandler} ) は混合色。
 *     描画は {@link the_four_primitives_and_weapons.client.SayaColorClient} が tintindex 0 に反映。
 *   - 模様レイヤー: バニラの機織り機が書き込む BlockEntityTag/Patterns ( 旗フォーマット )。
 */
public final class SayaDesign {

	private SayaDesign() {}

	/** 地の色 ( RGB 0xRRGGBB ) を保存する NBT キー。 */
	public static final String BASE_KEY = "SayaBase";

	/** 鞘本体の「スタイル」( 塗鞘/木目鞘/着せ鞘/刻鞘 ) を保存する NBT キー。
	 *  reitou 判定の "SayaStyle" とは別キーにして干渉を避ける。 */
	public static final String STYLE_KEY = "SayaWrapStyle";

	/** スタイル文字列を返す ( 未設定 = 既定の塗鞘なので "" )。 */
	public static String getStyle(ItemStack saya) {
		CompoundTag t = saya.getTag();
		return (t != null && t.contains(STYLE_KEY)) ? t.getString(STYLE_KEY) : "";
	}

	/** 既定 ( 塗鞘 ) 以外のスタイルが設定されているか。 */
	public static boolean hasStyle(ItemStack saya) {
		String s = getStyle(saya);
		return !s.isEmpty() && !s.equals("nuri");
	}

	/** スタイルを設定。 null/空/"nuri" は既定 ( 塗鞘 ) に戻す ( タグ削除 )。 */
	public static void setStyle(ItemStack saya, String style) {
		if (style == null || style.isEmpty() || style.equals("nuri")) {
			if (saya.hasTag()) saya.getTag().remove(STYLE_KEY);
		} else {
			saya.getOrCreateTag().putString(STYLE_KEY, style);
		}
	}

	/** 漆の上塗り ( 下地 = スタイルとは別レイヤー )。 "black"=漆黒 / "red"=朱。 未塗り = ""。 */
	public static final String LACQUER_KEY = "SayaLacquer";

	/** 漆の上塗り色を返す ( 未塗り = "" )。 */
	public static String getLacquer(ItemStack saya) {
		CompoundTag t = saya.getTag();
		return (t != null && t.contains(LACQUER_KEY)) ? t.getString(LACQUER_KEY) : "";
	}

	/** 漆が塗られているか。 */
	public static boolean hasLacquer(ItemStack saya) {
		return !getLacquer(saya).isEmpty();
	}

	/** 漆の上塗りを設定。 null/空 は剥がす ( タグ削除 )。 下地スタイルはそのまま残る。 */
	public static void setLacquer(ItemStack saya, String lacquer) {
		if (lacquer == null || lacquer.isEmpty()) {
			if (saya.hasTag()) saya.getTag().remove(LACQUER_KEY);
		} else {
			saya.getOrCreateTag().putString(LACQUER_KEY, lacquer);
		}
	}

	/** 鞘かどうか。 */
	public static boolean isSaya(ItemStack stack) {
		return CuriosScabbardHelper.isScabbard(stack);
	}

	/** 地色が設定されているか。 */
	public static boolean hasBase(ItemStack saya) {
		CompoundTag t = saya.getTag();
		return t != null && t.contains(BASE_KEY);
	}

	/** 地の色を 0xRRGGBB で返す。 未設定なら -1。
	 *  皮装備方式にも対応: SayaBase が無ければ display.color を見る ( /give …{display:{color:N}} )。 */
	public static int getBaseRgb(ItemStack saya) {
		CompoundTag t = saya.getTag();
		if (t == null) return -1;
		if (t.contains(BASE_KEY)) return t.getInt(BASE_KEY) & 0xFFFFFF;
		if (t.contains("display", 10)) {
			CompoundTag d = t.getCompound("display");
			if (d.contains("color", 99)) return d.getInt("color") & 0xFFFFFF;
		}
		return -1;
	}

	/** 任意 RGB で地色を設定。 */
	public static void setBaseColorRgb(ItemStack saya, int rgb) {
		saya.getOrCreateTag().putInt(BASE_KEY, rgb & 0xFFFFFF);
	}

	/** 染料色 ( DyeColor ) で地色を設定。 */
	public static void setBaseColor(ItemStack saya, DyeColor color) {
		setBaseColorRgb(saya, dyeRgb(color));
	}

	/** 保存色に最も近い DyeColor を返す ( 機織りプレビューやツールチップ用。 未設定は白 )。 */
	public static DyeColor getBaseColor(ItemStack saya) {
		int rgb = getBaseRgb(saya);
		if (rgb < 0) return DyeColor.WHITE;
		int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
		DyeColor best = DyeColor.WHITE;
		long bestD = Long.MAX_VALUE;
		for (DyeColor c : DyeColor.values()) {
			int crgb = dyeRgb(c);
			int dr = ((crgb >> 16) & 255) - r, dg = ((crgb >> 8) & 255) - g, db = (crgb & 255) - b;
			long d = (long) dr * dr + (long) dg * dg + (long) db * db;
			if (d < bestD) { bestD = d; best = c; }
		}
		return best;
	}

	/** 地色を "#RRGGBB" で返す。 未染色は null。 */
	public static String getBaseHex(ItemStack saya) {
		int rgb = getBaseRgb(saya);
		return (rgb < 0) ? null : String.format("#%06X", rgb);
	}

	public static int dyeRgb(DyeColor color) {
		float[] c = color.getTextureDiffuseColors();
		return (((int) (c[0] * 255)) << 16) | (((int) (c[1] * 255)) << 8) | ((int) (c[2] * 255));
	}

	/** 模様 ( 旗フォーマットのレイヤー数 )。 */
	public static int patternCount(ItemStack saya) {
		return BannerBlockEntity.getPatternCount(saya);
	}

	/**
	 * 一番上に付けた模様を 1枚だけ剥がす ( 旗を大釜で洗うのと同じ )。
	 * 剥がせたら true。 模様が無ければ false。 最後の1枚を剥がしたら Patterns タグごと掃除する。
	 */
	public static boolean removeLastPattern(ItemStack saya) {
		CompoundTag tag = saya.getTag();
		if (tag == null || !tag.contains("BlockEntityTag", 10)) return false;
		CompoundTag be = tag.getCompound("BlockEntityTag");
		if (!be.contains("Patterns", 9)) return false;
		ListTag list = be.getList("Patterns", 10);
		if (list.isEmpty()) return false;
		list.remove(list.size() - 1);
		if (list.isEmpty()) {
			be.remove("Patterns");
			if (be.isEmpty()) tag.remove("BlockEntityTag");
		} else {
			be.put("Patterns", list);
		}
		if (tag.isEmpty()) saya.setTag(null);
		return true;
	}

	/** 地色か模様のいずれかが設定されているか。 */
	public static boolean hasDesign(ItemStack saya) {
		if (hasBase(saya)) return true;
		CompoundTag be = BlockItem.getBlockEntityData(saya);
		return be != null && be.contains("Patterns");
	}
}
