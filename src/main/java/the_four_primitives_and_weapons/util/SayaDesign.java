package the_four_primitives_and_weapons.util;

import net.minecraft.nbt.CompoundTag;
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

	/** 鞘かどうか。 */
	public static boolean isSaya(ItemStack stack) {
		return CuriosScabbardHelper.isScabbard(stack);
	}

	/** 地色が設定されているか。 */
	public static boolean hasBase(ItemStack saya) {
		CompoundTag t = saya.getTag();
		return t != null && t.contains(BASE_KEY);
	}

	/** 地の色を 0xRRGGBB で返す。 未設定なら -1。 */
	public static int getBaseRgb(ItemStack saya) {
		CompoundTag t = saya.getTag();
		return (t != null && t.contains(BASE_KEY)) ? (t.getInt(BASE_KEY) & 0xFFFFFF) : -1;
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

	/** 地色か模様のいずれかが設定されているか。 */
	public static boolean hasDesign(ItemStack saya) {
		if (hasBase(saya)) return true;
		CompoundTag be = BlockItem.getBlockEntityData(saya);
		return be != null && be.contains("Patterns");
	}
}
