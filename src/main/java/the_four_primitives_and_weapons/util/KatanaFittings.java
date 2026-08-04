package the_four_primitives_and_weapons.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刀の拵え ( こしらえ ): 柄 ( つか ) と 鍔 ( つば ) の色を NBT に保存するヘルパー。
 *
 * <p>モデルでは 柄の面に tintindex 1、 鍔の面に tintindex 2 を付けてある。
 * {@link the_four_primitives_and_weapons.client.KatanaColorClient} がその番号で
 * ここに保存した色を返して着色する ( 未設定なら白 = 無着色 )。</p>
 */
public final class KatanaFittings {

	private KatanaFittings() {}

	public static final TagKey<Item> FITTING_DYEABLE_TAG = TagKey.create(
			ForgeRegistries.ITEMS.getRegistryKey(),
			new ResourceLocation(the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.MODID, "koshirae/fitting_dyeable"));
	public static final TagKey<Item> NO_FITTING_DYE_TAG = TagKey.create(
			ForgeRegistries.ITEMS.getRegistryKey(),
			new ResourceLocation(the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.MODID, "koshirae/no_fitting_dye"));
	/** 木刀など 練習用武器。 拵えの染色/差し替え も 納刀 も 一切できない ( 最優先で除外 )。 */
	public static final TagKey<Item> PRACTICE_WEAPON_TAG = TagKey.create(
			ForgeRegistries.ITEMS.getRegistryKey(),
			new ResourceLocation(the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.MODID, "koshirae/practice_weapon"));

	/** 木刀など ( {@link #PRACTICE_WEAPON_TAG} ) か。 染色不可・納刀不可の判定に使う。 */
	public static boolean isPracticeWeapon(ItemStack s) {
		return s != null && !s.isEmpty() && s.is(PRACTICE_WEAPON_TAG);
	}

	private static final Map<Item, Boolean> OWN_MODEL_FITTING_CACHE = new ConcurrentHashMap<>();
	private static final Map<Item, Boolean> MODEL_FITTING_CACHE = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, JsonObject> MODEL_JSON_CACHE = new ConcurrentHashMap<>();

	/** 拵え ( 柄/鍔/頭 ) を着せ替え・染色できる武器か。
	 *  本MODの 名前に katana / tyokuto / rapier を含む武器 全種 ( saya は除く )。 */
	public static boolean isFittingWeapon(ItemStack s) {
		if (s == null || s.isEmpty()) return false;
		if (s.is(PRACTICE_WEAPON_TAG)) return false; // 木刀など は 最優先で染色/差し替え不可
		if (s.is(FITTING_DYEABLE_TAG)) return true;
		if (hasOwnCompleteFittingModel(s.getItem())) return true;
		if (s.is(NO_FITTING_DYE_TAG)) return false;
		if (hasCompleteFittingModel(s.getItem())) return true;
		net.minecraft.resources.ResourceLocation id =
				net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
		if (id == null || !id.getNamespace().equals(the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.MODID))
			return false;
		String n = id.getPath();
		if (n.contains("saya")) return false;
		return n.contains("katana") || n.contains("tyokuto") || n.contains("rapier") || n.contains("dagger")
				|| isDyeableKnife(n);
	}

	/**
	 * 投げナイフ系か ( 柄だけ染色できる )。
	 *
	 * <p>ナイフの item model は 刀身 + 柄 の 2 テクスチャしか無いので
	 * {@link #hasCompleteFittingModel} ( 刀身/鍔/頭/柄 の 4 つを要求 ) を通らない。
	 * 名前で拾う。 発射装置 ( knife_launcher ) はナイフ本体ではないので除く。</p>
	 */
	private static boolean isDyeableKnife(String path) {
		return path.contains("knife") && !path.contains("launcher");
	}

	/**
	 * Client registration 用の広めの候補判定。
	 * 実際に染色するかは {@link #isFittingWeapon(ItemStack)} がタグ/NBT/モデルで再判定する。
	 */
	public static boolean isPotentialFittingWeaponItem(Item item) {
		if (item == null) return false;
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
		if (id == null) return false;
		String n = id.getPath();
		if (n.contains("saya")) return false;
		if (hasCompleteFittingModel(item)) return true;
		if (item instanceof net.minecraft.world.item.SwordItem) return true;
		return id.getNamespace().equals(the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.MODID)
				&& (n.contains("katana") || n.contains("tyokuto") || n.contains("rapier") || n.contains("dagger")
					|| isDyeableKnife(n));
	}

	/** item model json 自身に刀身/鍔/頭/柄の4テクスチャが揃っているか。 no_fitting_dye より優先する。 */
	public static boolean hasOwnCompleteFittingModel(Item item) {
		return OWN_MODEL_FITTING_CACHE.computeIfAbsent(item, KatanaFittings::detectOwnCompleteFittingModel);
	}

	/** 親モデル継承後も含め、刀身/鍔/頭/柄の4テクスチャが揃っているか。 addon 自動対応用。 */
	public static boolean hasCompleteFittingModel(Item item) {
		return MODEL_FITTING_CACHE.computeIfAbsent(item, KatanaFittings::detectCompleteFittingModel);
	}

	private static boolean detectOwnCompleteFittingModel(Item item) {
		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
		if (itemId == null) return false;
		JsonObject model = readModel(new ResourceLocation(itemId.getNamespace(), "item/" + itemId.getPath()));
		if (model == null) return false;
		return hasCompleteFittingTextureSet(readTextures(model));
	}

	private static boolean detectCompleteFittingModel(Item item) {
		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
		if (itemId == null) return false;
		Map<String, String> textures = collectTextures(new ResourceLocation(itemId.getNamespace(), "item/" + itemId.getPath()), 0);
		return hasCompleteFittingTextureSet(textures);
	}

	private static Map<String, String> collectTextures(ResourceLocation modelId, int depth) {
		Map<String, String> out = new HashMap<>();
		if (modelId == null || depth > 16) return out;
		JsonObject model = readModel(modelId);
		if (model == null) return out;

		JsonElement parent = model.get("parent");
		if (parent != null && parent.isJsonPrimitive()) {
			ResourceLocation parentId = ResourceLocation.tryParse(parent.getAsString());
			if (parentId != null) {
				out.putAll(collectTextures(parentId, depth + 1));
			}
		}
		out.putAll(readTextures(model));
		return out;
	}

	private static Map<String, String> readTextures(JsonObject model) {
		Map<String, String> out = new HashMap<>();
		if (model == null || !model.has("textures") || !model.get("textures").isJsonObject()) return out;
		JsonObject textures = model.getAsJsonObject("textures");
		for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
			JsonElement value = entry.getValue();
			if (value != null && value.isJsonPrimitive()) {
				out.put(entry.getKey(), value.getAsString());
			}
		}
		return out;
	}

	private static boolean hasCompleteFittingTextureSet(Map<String, String> textures) {
		if (textures == null || textures.isEmpty()) return false;
		boolean blade = textures.containsKey("0") || containsPath(textures, "blade/");
		boolean tsuba = textures.containsKey("1") || containsAnyPath(textures, "katana_fitting/tsuba", "/tsuba/", "/tuba/", "guard");
		boolean kashira = textures.containsKey("3") || containsAnyPath(textures, "katana_fitting/kasira", "/kasira/", "kashira", "pommel");
		boolean tsuka = textures.containsKey("4") || containsAnyPath(textures, "katana_fitting/tsuka", "/tsuka/", "/tuka/", "grip");
		return blade && tsuba && kashira && tsuka;
	}

	private static boolean containsAnyPath(Map<String, String> textures, String... needles) {
		for (String needle : needles) {
			if (containsPath(textures, needle)) return true;
		}
		return false;
	}

	private static boolean containsPath(Map<String, String> textures, String needle) {
		String lowerNeedle = needle.toLowerCase(Locale.ROOT);
		for (String value : textures.values()) {
			if (value != null && value.toLowerCase(Locale.ROOT).contains(lowerNeedle)) {
				return true;
			}
		}
		return false;
	}

	private static JsonObject readModel(ResourceLocation modelId) {
		if (modelId == null) return null;
		if (modelId.getPath().startsWith("builtin/")) return null;
		return MODEL_JSON_CACHE.computeIfAbsent(modelId, KatanaFittings::loadModelJson);
	}

	private static JsonObject loadModelJson(ResourceLocation modelId) {
		String path = "assets/" + modelId.getNamespace() + "/models/" + modelId.getPath() + ".json";
		try (InputStream in = KatanaFittings.class.getClassLoader().getResourceAsStream(path)) {
			if (in == null) return null;
			try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				JsonElement parsed = JsonParser.parseReader(reader);
				return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
			}
		} catch (Exception ignored) {
			return null;
		}
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
