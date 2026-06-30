package the_four_primitives_and_weapons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鞘 ( saya ) の機織り模様を描画する合成 BakedModel。
 *
 * <p>ベースの鞘モデルの「鞘本体 ( tintindex 0 ) の面」をそのまま複製し、 サンプル先テクスチャを
 * バニラの旗模様スプライトへ差し替えたオーバーレイ面を上に重ねる。 UV レイアウトは
 * 鞘本体のものを流用するため、 旗模様が「鞘に合った形」で貼り付く。</p>
 *
 * <p>各模様レイヤーは tintindex = レイヤー番号+1 を持ち、 色は
 * {@link SayaColorClient} がその番号でレイヤーの染料色を返して着ける。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SayaPatternModel implements BakedModel {

	/** ベースモデル identity + 模様スプライト並び でキャッシュ。 */
	private static final Map<String, SayaPatternModel> CACHE = new ConcurrentHashMap<>();

	private final BakedModel base;
	private final List<BakedQuad> overlayNull;
	private final Map<Direction, List<BakedQuad>> overlayBySide;

	private SayaPatternModel(BakedModel base, List<TextureAtlasSprite> sprites) {
		this.base = base;
		this.overlayBySide = new EnumMap<>(Direction.class);
		this.overlayNull = buildOverlay(base.getQuads(null, null, RandomSource.create(42L)), sprites);
		for (Direction d : Direction.values()) {
			overlayBySide.put(d, buildOverlay(base.getQuads(null, d, RandomSource.create(42L)), sprites));
		}
	}

	/** 旗模様id → カテゴリ ( ファイルを種類別に整理するためのサブフォルダ )。 */
	private static String category(String id) {
		switch (id) {
			case "square_top_left": case "square_top_right":
			case "square_bottom_left": case "square_bottom_right":
				return "square";
			case "triangle_top": case "triangle_bottom":
			case "triangles_top": case "triangles_bottom":
			case "diagonal_left": case "diagonal_right":
			case "diagonal_up_left": case "diagonal_up_right":
				return "triangle";
			case "stripe_top": case "stripe_bottom": case "stripe_middle":
			case "stripe_left": case "stripe_right": case "stripe_center":
			case "small_stripes": case "stripe_downleft": case "stripe_downright":
				return "stripe";
			case "half_horizontal": case "half_horizontal_bottom":
			case "half_vertical": case "half_vertical_right":
				return "half";
			case "cross": case "straight_cross":
				return "cross";
			case "border": case "curly_border":
				return "border";
			case "gradient": case "gradient_up":
				return "gradient";
			case "circle": case "rhombus":
				return "shape";
			case "base": case "bricks":
				return "fill";
			case "creeper": case "skull": case "flower":
			case "mojang": case "globe": case "piglin":
				return "figure";
			case "saya_mon": case "saya_wave": case "saya_scale":
			case "saya_erosion": case "saya_thunder": case "saya_rust":
				return "saya"; // 鞘専用模様
			default:
				return "other";
		}
	}

	/** SayaType → saya_pattern のサブフォルダ名。 */
	private static String typeDir(@Nullable the_four_primitives_and_weapons.util.SayaRegistry.SayaType type) {
		if (type == null) return null;
		switch (type) {
			case KATANA:  return "katana";
			case SWORD:   return "sword";
			case TYOKUTO: return "tyokuto";
			case RAPIER:  return "rapier";
			default:      return null;
		}
	}

	/** 模様付きならキャッシュした合成モデルを、 無ければ base をそのまま返す。 */
	public static BakedModel maybe(BakedModel base, ListTag patterns,
								   @Nullable the_four_primitives_and_weapons.util.SayaRegistry.SayaType type) {
		if (base == null || patterns == null || patterns.isEmpty()) return base;
		try {
			String dir = typeDir(type);
			// patterns と同じ並びの配列 ( 用意したテクスチャが無い模様は null = 表示しない )。
			List<TextureAtlasSprite> sprites = resolveSprites(patterns, dir);
			boolean any = false;
			StringBuilder key = new StringBuilder(Integer.toHexString(System.identityHashCode(base)));
			key.append('@').append(dir);
			for (int i = 0; i < sprites.size(); i++) {
				TextureAtlasSprite s = sprites.get(i);
				key.append('|').append(i).append(':').append(s == null ? "-" : s.contents().name());
				if (s != null) any = true;
			}
			if (!any) return base; // 表示できる模様が1つも無ければ base のまま
			return CACHE.computeIfAbsent(key.toString(), k -> new SayaPatternModel(base, sprites));
		} catch (Throwable t) {
			return base; // 何か失敗してもベース描画にフォールバック ( クラッシュさせない )
		}
	}

	private static final org.apache.logging.log4j.Logger LOGGER =
			org.apache.logging.log4j.LogManager.getLogger("MAW/SayaPattern");
	private static final java.util.concurrent.atomic.AtomicBoolean LOGGED =
			new java.util.concurrent.atomic.AtomicBoolean(false);

	/**
	 * 各模様レイヤーを 鞘専用テクスチャ ( saya_pattern/... ) へ解決する。 patterns と同じ並びで返し、
	 * 用意したテクスチャが無い模様は null ( = 表示しない )。 バニラ旗模様へのフォールバックはしない
	 * ので、 「用意した特定の模様だけ」 が鞘に出る。
	 */
	private static List<TextureAtlasSprite> resolveSprites(ListTag patterns, @Nullable String typeDir) {
		List<TextureAtlasSprite> out = new ArrayList<>();
		for (int i = 0; i < patterns.size(); i++) out.add(null);
		var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
		net.minecraft.resources.ResourceLocation missing =
				net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
		String modid = the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.MODID;
		boolean logOnce = LOGGED.compareAndSet(false, true);
		for (int i = 0; i < patterns.size(); i++) {
			CompoundTag c = patterns.getCompound(i);
			String hash = c.getString("Pattern");
			var holder = BannerPattern.byHash(hash);
			if (holder == null) continue;
			var keyOpt = holder.unwrapKey();
			if (keyOpt.isEmpty()) continue;
			ResourceKey<BannerPattern> rk = keyOpt.get();
			String path = rk.location().getPath();

			String cat = category(path);
			TextureAtlasSprite sprite = null;
			String picked = null;
			// 模様テクスチャは全種類共通 ( 面ごとに丸ごと貼るので種類別レイアウト不要 )。
			// カテゴリ別 > 共通 の順に探す。 種類別を置きたい場合のみ先頭に追加。
			String[] candidates = (typeDir != null)
					? new String[]{
						"saya_pattern/" + typeDir + "/" + cat + "/" + path,
						"saya_pattern/" + typeDir + "/" + path,
						"saya_pattern/" + cat + "/" + path,
						"saya_pattern/" + path}
					: new String[]{
						"saya_pattern/" + cat + "/" + path,
						"saya_pattern/" + path};
			for (String cand : candidates) {
				TextureAtlasSprite s = atlas.getSprite(new net.minecraft.resources.ResourceLocation(modid, cand));
				if (s != null && !s.contents().name().equals(missing)) { sprite = s; picked = cand; break; }
			}
			if (logOnce) {
				LOGGER.info("[MAW] saya pattern '{}' ({}) -> {}", hash, typeDir, picked == null ? "(none, skipped)" : picked);
			}
			out.set(i, sprite); // 見つからなければ null のまま ( この模様は表示しない )
		}
		return out;
	}

	/**
	 * 鞘本体 ( tintindex 0 ) の面に模様テクスチャを重ねる。
	 *
	 * <p>up/down のうち、 <b>一番上の up ( 鞘本体の先端部分 = 刀を差す口 )</b> と
	 * <b>一番下の down ( 鞘本体のメイン部分 = 先端 )</b> だけは模様を出さない。 それ以外の
	 * up/down ( 段と段の継ぎ目 ) には模様を付けるので、 継ぎ目に地色が覗くのを防ぐ。</p>
	 */
	private static List<BakedQuad> buildOverlay(List<BakedQuad> srcQuads, List<TextureAtlasSprite> sprites) {
		// 模様を出さない 2面 ( 最上の up と 最下の down ) の Y を求める。
		float topUpY = -Float.MAX_VALUE, botDownY = Float.MAX_VALUE;
		for (BakedQuad q : srcQuads) {
			if (q.getTintIndex() != 0) continue;
			Direction d = q.getDirection();
			if (d == Direction.UP)   topUpY   = Math.max(topUpY,   faceY(q));
			if (d == Direction.DOWN) botDownY = Math.min(botDownY, faceY(q));
		}
		List<BakedQuad> out = new ArrayList<>();
		for (BakedQuad q : srcQuads) {
			if (q.getTintIndex() != 0) continue;                 // 鞘本体の面だけ
			Direction d = q.getDirection();
			if (d == Direction.UP   && Math.abs(faceY(q) - topUpY)   < 1e-4f) continue; // 刀を差す口
			if (d == Direction.DOWN && Math.abs(faceY(q) - botDownY) < 1e-4f) continue; // 先端
			for (int layer = 0; layer < sprites.size(); layer++) {
				TextureAtlasSprite s = sprites.get(layer);
				if (s == null) continue; // 用意していない模様は飛ばす ( 色番号 tintindex は layer のまま維持 )
				out.add(remap(q, s, layer + 1, 0.0008f * (layer + 1)));
			}
		}
		return out;
	}

	/** up/down 面の Y ( 水平面なので最初の頂点の Y で代表 )。 */
	private static float faceY(BakedQuad q) {
		return Float.intBitsToFloat(q.getVertices()[1]);
	}

	/**
	 * quad を複製し、 その面 1枚に模様テクスチャ全体 ( 0..1 ) を貼る ( per-face full mapping )。
	 *
	 * <p>鞘 wrap は細長いセグメントの集まりで、 各セグメントの 4つの長い面 ( N/S/E/W ) が
	 * それぞれ別のUV区画を持つ。 面の「自前の頂点UVの範囲」を 0..1 に正規化して模様スプライト全体へ
	 * 写すので、 どの面 ( 側面含む ) にも模様デザイン1枚が丸ごと出る。 模様テクスチャは縦長
	 * ( 長さ方向に模様が並ぶ ) に描いてある前提。</p>
	 */
	private static BakedQuad remap(BakedQuad src, TextureAtlasSprite dst, int tint, float push) {
		int[] v = src.getVertices().clone();
		int stride = v.length / 4;
		// この面自身の頂点UVの範囲 ( = 面が占める区画 ) を求める。
		float umin = Float.MAX_VALUE, umax = -Float.MAX_VALUE, vmin = Float.MAX_VALUE, vmax = -Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			int o = i * stride;
			float u = Float.intBitsToFloat(v[o + 4]);
			float w = Float.intBitsToFloat(v[o + 5]);
			umin = Math.min(umin, u); umax = Math.max(umax, u);
			vmin = Math.min(vmin, w); vmax = Math.max(vmax, w);
		}
		float uSpan = (umax != umin) ? (umax - umin) : 1f;
		float vSpan = (vmax != vmin) ? (vmax - vmin) : 1f;
		float du0 = dst.getU0(), du1 = dst.getU1(), dv0 = dst.getV0(), dv1 = dst.getV1();
		Vec3i n = src.getDirection().getNormal();
		for (int i = 0; i < 4; i++) {
			int o = i * stride;
			float x = Float.intBitsToFloat(v[o]);
			float y = Float.intBitsToFloat(v[o + 1]);
			float z = Float.intBitsToFloat(v[o + 2]);
			v[o] = Float.floatToRawIntBits(x + n.getX() * push);
			v[o + 1] = Float.floatToRawIntBits(y + n.getY() * push);
			v[o + 2] = Float.floatToRawIntBits(z + n.getZ() * push);
			v[o + 3] = 0xFFFFFFFF; // 頂点色は白 ( 色は tintindex+ItemColor で着ける )
			// 面内0..1 → 模様スプライト全体へ。 面ごとに模様1枚を丸ごと貼る。
			float uFrac = (Float.intBitsToFloat(v[o + 4]) - umin) / uSpan;
			float vFrac = (Float.intBitsToFloat(v[o + 5]) - vmin) / vSpan;
			v[o + 4] = Float.floatToRawIntBits(du0 + uFrac * (du1 - du0));
			v[o + 5] = Float.floatToRawIntBits(dv0 + vFrac * (dv1 - dv0));
		}
		return new BakedQuad(v, tint, src.getDirection(), dst, src.isShade());
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		List<BakedQuad> baseQuads = base.getQuads(state, side, rand);
		List<BakedQuad> add = (side == null) ? overlayNull : overlayBySide.get(side);
		if (add == null || add.isEmpty()) return baseQuads;
		List<BakedQuad> all = new ArrayList<>(baseQuads.size() + add.size());
		all.addAll(baseQuads);
		all.addAll(add);
		return all;
	}

	// ===== BakedModel 委譲 =====
	@Override public boolean useAmbientOcclusion() { return base.useAmbientOcclusion(); }
	@Override public boolean isGui3d() { return base.isGui3d(); }
	@Override public boolean usesBlockLight() { return base.usesBlockLight(); }
	@Override public boolean isCustomRenderer() { return false; }
	@SuppressWarnings("deprecation")
	@Override public TextureAtlasSprite getParticleIcon() { return base.getParticleIcon(); }
	@SuppressWarnings("deprecation")
	@Override public ItemTransforms getTransforms() { return base.getTransforms(); }
	@Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }

	@Override
	public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context,
									 com.mojang.blaze3d.vertex.PoseStack poseStack, boolean leftFlip) {
		base.applyTransform(context, poseStack, leftFlip);
		return this;
	}
}
