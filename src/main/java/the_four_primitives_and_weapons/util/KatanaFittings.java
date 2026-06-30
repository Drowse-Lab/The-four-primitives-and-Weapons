package the_four_primitives_and_weapons.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/**
 * 刀の拵え ( こしらえ ): 柄 ( つか ) と 鍔 ( つば ) の色を NBT に保存するヘルパー。
 *
 * <p>モデルでは 柄の面に tintindex 1、 鍔の面に tintindex 2 を付けてある。
 * {@link the_four_primitives_and_weapons.client.KatanaColorClient} がその番号で
 * ここに保存した色を返して着色する ( 未設定なら白 = 無着色 )。</p>
 */
public final class KatanaFittings {

	private KatanaFittings() {}

	/** 柄巻きの色 ( 0xRRGGBB )。 */
	public static final String TSUKA_KEY = "TsukaColor";
	/** 鍔の色 ( 0xRRGGBB )。 */
	public static final String TSUBA_KEY = "TsubaColor";
	/** 頭 ( かしら ) の色。 */
	public static final String KASHIRA_KEY = "KashiraColor";
	/** 縁 ( ふち ) の色。 */
	public static final String FUCHI_KEY = "FuchiColor";
	/** 柄巻きの巻き方 ( デザイン )。 */
	public static final String TSUKA_WRAP_KEY = "TsukaWrap";

	/** 柄巻きの巻き方の一覧 ( 順に切り替え )。 "" = 既定 ( 元テクスチャ )。 */
	public static final String[] WRAPS = {
			"hishi",    // 菱巻 ( 最も一般的 )
			"hira",     // 平巻 ( 太刀巻 )
			"katate",   // 片手巻 ( 螺旋 )
			"hineri",   // 捻巻
			"tsumami",  // 撮巻/摘巻
			"jabara",   // 蛇腹巻
			"kake",     // 掛巻
			"hijiri",   // 聖柄 ( 巻き無しの素柄 )
			"shirasaya",// 白鞘 ( 素柄 + つばなし )
			"saber"     // 軍刀 ( サーベル柄: 革巻き+索条 )
	};

	/** 鍔のデザイン ( 差し替え )。 "" = 既定。 "saber" = サーベルの鍔。 */
	public static final String TSUBA_STYLE_KEY = "TsubaStyle";
	public static final String[] TSUBAS = { "saber" };

	public static String getTsubaStyle(ItemStack stack) {
		CompoundTag t = stack.getTag();
		return (t != null && t.contains(TSUBA_STYLE_KEY)) ? t.getString(TSUBA_STYLE_KEY) : "";
	}

	public static void setTsubaStyle(ItemStack stack, String style) {
		if (style == null || style.isEmpty()) {
			if (stack.hasTag()) stack.getTag().remove(TSUBA_STYLE_KEY);
		} else {
			stack.getOrCreateTag().putString(TSUBA_STYLE_KEY, style);
		}
	}

	/** 次の鍔デザインへ ( 既定→TSUBAS[0]→…→末尾→既定 )。 */
	public static String nextTsuba(String current) {
		if (current == null || current.isEmpty()) return TSUBAS[0];
		for (int i = 0; i < TSUBAS.length; i++) {
			if (TSUBAS[i].equals(current)) return (i + 1 < TSUBAS.length) ? TSUBAS[i + 1] : "";
		}
		return TSUBAS[0];
	}

	/** 現在の柄巻きデザイン ( 未設定 = "" )。 */
	public static String getTsukaWrap(ItemStack stack) {
		CompoundTag t = stack.getTag();
		return (t != null && t.contains(TSUKA_WRAP_KEY)) ? t.getString(TSUKA_WRAP_KEY) : "";
	}

	/** 柄巻きデザインを設定 ( null/"" は元テクスチャに戻す )。 */
	public static void setTsukaWrap(ItemStack stack, String wrap) {
		if (wrap == null || wrap.isEmpty()) {
			if (stack.hasTag()) stack.getTag().remove(TSUKA_WRAP_KEY);
		} else {
			stack.getOrCreateTag().putString(TSUKA_WRAP_KEY, wrap);
		}
	}

	/** 次の柄巻きデザインへ切り替えた値を返す ( 既定→WRAPS[0]→…→末尾→既定 )。 */
	public static String nextWrap(String current) {
		if (current == null || current.isEmpty()) return WRAPS[0];
		for (int i = 0; i < WRAPS.length; i++) {
			if (WRAPS[i].equals(current)) {
				return (i + 1 < WRAPS.length) ? WRAPS[i + 1] : ""; // 末尾の次は既定に戻す
			}
		}
		return WRAPS[0];
	}

	public static int tsukaRgb(ItemStack stack) { return rgb(stack, TSUKA_KEY); }
	public static int tsubaRgb(ItemStack stack) { return rgb(stack, TSUBA_KEY); }
	public static int kashiraRgb(ItemStack stack) { return rgb(stack, KASHIRA_KEY); }
	public static int fuchiRgb(ItemStack stack) { return rgb(stack, FUCHI_KEY); }

	private static int rgb(ItemStack stack, String key) {
		CompoundTag t = stack.getTag();
		return (t != null && t.contains(key)) ? (t.getInt(key) & 0xFFFFFF) : -1;
	}

	public static void setTsuka(ItemStack stack, int rgb) { stack.getOrCreateTag().putInt(TSUKA_KEY, rgb & 0xFFFFFF); }
	public static void setTsuba(ItemStack stack, int rgb) { stack.getOrCreateTag().putInt(TSUBA_KEY, rgb & 0xFFFFFF); }
	public static void setKashira(ItemStack stack, int rgb) { stack.getOrCreateTag().putInt(KASHIRA_KEY, rgb & 0xFFFFFF); }
	public static void setFuchi(ItemStack stack, int rgb) { stack.getOrCreateTag().putInt(FUCHI_KEY, rgb & 0xFFFFFF); }

	/** 染料色 → 0xRRGGBB。 */
	public static int dyeRgb(DyeColor color) {
		float[] c = color.getTextureDiffuseColors();
		return (((int) (c[0] * 255)) << 16) | (((int) (c[1] * 255)) << 8) | ((int) (c[2] * 255));
	}
}
