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

	/** 拵え ( 柄/鍔/頭 ) を着せ替え・染色できる武器か。
	 *  本MODの 名前に katana / tyokuto / rapier を含む武器 全種 ( saya は除く )。 */
	public static boolean isFittingWeapon(ItemStack s) {
		if (s == null || s.isEmpty()) return false;
		net.minecraft.resources.ResourceLocation id =
				net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
		if (id == null || !id.getNamespace().equals(the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.MODID))
			return false;
		String n = id.getPath();
		if (n.contains("saya")) return false;
		return n.contains("katana") || n.contains("tyokuto") || n.contains("rapier");
	}

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

	/** 柄巻きの一覧 ( 順に切り替え )。 "" = 既定 ( 刀本体テクスチャ = ネイティブ )。
	 *  ※ これらは刀UVレイアウトで描いた絵を source-relative で貼る ( 刀本体テクスチャの色替え )。
	 *  ※ "tuka"(菱デザイン) は鞘が箱UVで直接使う専用テクスチャなので、 ここには入れない
	 *     ( 持っている刀の source-relative では崩れるため )。 */
	public static final String[] WRAPS = { "tuka_black", "tuka_red", "tuka_brown", "tuka_white" };
	/** 頭 ( かしら ) のデザイン一覧。 */
	public static final String[] KASHIRAS = { "kasira" };
	/** 縁 ( ふち ) のデザイン一覧。 */
	public static final String[] FUCHIS = { "fuchi" };
	public static final String KASHIRA_STYLE_KEY = "KashiraStyle";
	public static final String FUCHI_STYLE_KEY = "FuchiStyle";

	public static String getKashiraStyle(ItemStack s) {
		CompoundTag t = s.getTag();
		return (t != null && t.contains(KASHIRA_STYLE_KEY)) ? t.getString(KASHIRA_STYLE_KEY) : "";
	}
	public static String getFuchiStyle(ItemStack s) {
		CompoundTag t = s.getTag();
		return (t != null && t.contains(FUCHI_STYLE_KEY)) ? t.getString(FUCHI_STYLE_KEY) : "";
	}
	public static void setKashiraStyle(ItemStack s, String v) {
		if (v == null || v.isEmpty()) { if (s.hasTag()) s.getTag().remove(KASHIRA_STYLE_KEY); }
		else s.getOrCreateTag().putString(KASHIRA_STYLE_KEY, v);
	}
	public static void setFuchiStyle(ItemStack s, String v) {
		if (v == null || v.isEmpty()) { if (s.hasTag()) s.getTag().remove(FUCHI_STYLE_KEY); }
		else s.getOrCreateTag().putString(FUCHI_STYLE_KEY, v);
	}

	/** 鍔のデザイン ( 差し替え )。 "" = 既定。 */
	public static final String TSUBA_STYLE_KEY = "TsubaStyle";
	public static final String[] TSUBAS = { "tuba" };

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

	// 各部位: 専用キーが無ければ 皮装備方式 display.color を見る ( /give …{display:{color:N}} で全部位が同色になる )。
	public static int tsukaRgb(ItemStack stack)   { int c = rgb(stack, TSUKA_KEY);   return c >= 0 ? c : displayColor(stack); }
	public static int tsubaRgb(ItemStack stack)   { int c = rgb(stack, TSUBA_KEY);   return c >= 0 ? c : displayColor(stack); }
	public static int kashiraRgb(ItemStack stack) { int c = rgb(stack, KASHIRA_KEY); return c >= 0 ? c : displayColor(stack); }
	public static int fuchiRgb(ItemStack stack)   { return rgb(stack, FUCHI_KEY); }

	/** /give …{display:{color:N}} で入れた色 ( 革装備方式 )。 無ければ -1。 */
	private static int displayColor(ItemStack stack) {
		CompoundTag t = stack.getTag();
		if (t != null && t.contains("display", 10)) {
			CompoundTag d = t.getCompound("display");
			if (d.contains("color", 99)) return d.getInt("color") & 0xFFFFFF;
		}
		return -1;
	}

	private static int rgb(ItemStack stack, String key) {
		CompoundTag t = stack.getTag();
		return (t != null && t.contains(key)) ? (t.getInt(key) & 0xFFFFFF) : -1;
	}

	public static void setTsuka(ItemStack stack, int rgb) { stack.getOrCreateTag().putInt(TSUKA_KEY, rgb & 0xFFFFFF); }
	public static void setTsuba(ItemStack stack, int rgb) { stack.getOrCreateTag().putInt(TSUBA_KEY, rgb & 0xFFFFFF); }
	public static void setKashira(ItemStack stack, int rgb) { stack.getOrCreateTag().putInt(KASHIRA_KEY, rgb & 0xFFFFFF); }
	public static void setFuchi(ItemStack stack, int rgb) { stack.getOrCreateTag().putInt(FUCHI_KEY, rgb & 0xFFFFFF); }

	/** 色が「ほぼ黒」か。 黒染め時に 乗算tintで潰れるのを避け、 専用の黒テクスチャへ差し替える判定用。 */
	public static boolean isNearBlack(int rgb) {
		if (rgb < 0) return false;
		int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
		double l = 0.299 * r + 0.587 * g + 0.114 * b;
		return l < 45;
	}

	/** 染料色 → 0xRRGGBB。 */
	public static int dyeRgb(DyeColor color) {
		float[] c = color.getTextureDiffuseColors();
		return (((int) (c[0] * 255)) << 16) | (((int) (c[1] * 255)) << 8) | ((int) (c[2] * 255));
	}
}
