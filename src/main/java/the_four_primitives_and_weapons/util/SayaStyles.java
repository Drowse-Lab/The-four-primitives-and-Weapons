package the_four_primitives_and_weapons.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * 鞘本体の「仕立て ( スタイル )」定義。 1つの {@link SayaDesign#STYLE_KEY} で鞘全体の見た目が決まる
 * ( 下地と漆を重ねる方式はやめた = 浮いて見えないよう wrap テクスチャを丸ごと差し替える )。
 *
 * <ul>
 *   <li>素地系 ( 染色で色が乗る ): "wood_&lt;木材&gt;" 木目 / "kise" 着せ / "kizami" 刻 / "ishime" 石目 / "same" 鮫</li>
 *   <li>漆系 ( 色が固定。 つやのある仕立て ): "kuroro" 黒呂塗 / "roiro" 呂色塗 / "shunuri" 朱漆塗 / "tame" 溜塗</li>
 * </ul>
 */
public final class SayaStyles {

	private SayaStyles() {}

	/** 木目鞘で使える木材 ( バニラ全種 )。 */
	public static final String[] WOODS = {
			"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
			"mangrove", "cherry", "bamboo", "crimson", "warped"
	};

	/** スタイル文字列 → wrap に貼るスプライト。 既定 ( nuri ) は null ( = 差し替えなし )。 */
	public static ResourceLocation sprite(String style) {
		if (style == null || style.isEmpty() || style.equals("nuri")) return null;
		// 木目鞘: "wood:<名前空間>:<板材パス>" ( 他modの板材も可 )。 その板材のブロックテクスチャを使う。
		if (style.startsWith("wood:")) {
			ResourceLocation id = ResourceLocation.tryParse(style.substring("wood:".length()));
			if (id == null) return null;
			return new ResourceLocation(id.getNamespace(), "block/" + id.getPath());
		}
		// 旧形式 "wood_<木材>" ( minecraft の板材 ) も後方互換で対応。
		if (style.startsWith("wood_")) {
			String wood = style.substring("wood_".length());
			if (wood.isEmpty()) return null;
			return new ResourceLocation("minecraft", "block/" + wood + "_planks");
		}
		switch (style) {
			case "kise":    return styleTex("kise");
			case "kizami":  return styleTex("kizami");
			case "ishime":  return styleTex("ishime");
			case "same":    return styleTex("same");
			case "kuroro":  return styleTex("kuroro");
			case "roiro":   return styleTex("roiro");
			case "shunuri": return styleTex("shunuri");
			case "tame":    return styleTex("tame");
			case "gunto":   return styleTex("gunto");
			default:        return null;
		}
	}

	private static ResourceLocation styleTex(String name) {
		return new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "saya_style/" + name);
	}

	/** 木目鞘か。 */
	public static boolean isWood(String style) {
		return style != null && (style.startsWith("wood:") || style.startsWith("wood_"));
	}

	/** 色が固定の仕立てか ( 漆系。 染色tintを掛けず テクスチャの色をそのまま出す )。 */
	public static boolean isFixedColor(String style) {
		switch (style == null ? "" : style) {
			case "kuroro": case "roiro": case "shunuri": case "tame": case "gunto":
				return true;
			default:
				return false;
		}
	}

	// ===== 表示名 =====

	/** 仕立ての表示名。 */
	public static Component styleName(String style) {
		if (style == null || style.isEmpty() || style.equals("nuri")) {
			return Component.translatable("saya.style.nuri");
		}
		if (style.startsWith("wood:")) {
			ResourceLocation id = ResourceLocation.tryParse(style.substring("wood:".length()));
			Component plank = (id != null)
					? Component.translatable("block." + id.getNamespace() + "." + id.getPath())
					: Component.literal(style);
			return Component.translatable("saya.style.wood", plank);
		}
		if (style.startsWith("wood_")) { // 旧形式
			String wood = style.substring("wood_".length());
			return Component.translatable("saya.style.wood",
					Component.translatable("block.minecraft." + wood + "_planks"));
		}
		return Component.translatable("saya.style." + style);
	}

	/**
	 * ツールチップ用。 lacquer 引数は旧仕様の名残で 現在は未使用 ( 常に "" )。 仕立てが無ければ null。
	 */
	public static Component finishName(String style, String lacquer) {
		if (style == null || style.isEmpty() || style.equals("nuri")) return null;
		return styleName(style);
	}
}
