package the_four_primitives_and_weapons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.client.renderer.Sheets;

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
		RandomSource rand = RandomSource.create(42L);
		this.overlayNull = buildOverlay(base.getQuads(null, null, rand), sprites);
		for (Direction d : Direction.values()) {
			overlayBySide.put(d, buildOverlay(base.getQuads(null, d, RandomSource.create(42L)), sprites));
		}
	}

	/** 模様付きならキャッシュした合成モデルを、 無ければ base をそのまま返す。 */
	public static BakedModel maybe(BakedModel base, ListTag patterns) {
		if (base == null || patterns == null || patterns.isEmpty()) return base;
		try {
			List<TextureAtlasSprite> sprites = resolveSprites(patterns);
			if (sprites.isEmpty()) return base;
			StringBuilder key = new StringBuilder(Integer.toHexString(System.identityHashCode(base)));
			for (TextureAtlasSprite s : sprites) key.append('|').append(s.contents().name());
			return CACHE.computeIfAbsent(key.toString(), k -> new SayaPatternModel(base, sprites));
		} catch (Throwable t) {
			return base; // 何か失敗してもベース描画にフォールバック ( クラッシュさせない )
		}
	}

	private static final org.apache.logging.log4j.Logger LOGGER =
			org.apache.logging.log4j.LogManager.getLogger("MAW/SayaPattern");
	private static final java.util.concurrent.atomic.AtomicBoolean LOGGED =
			new java.util.concurrent.atomic.AtomicBoolean(false);

	/** ListTag ( 旗フォーマット ) の各レイヤーをブロックアトラス上の旗模様スプライトへ解決。 */
	private static List<TextureAtlasSprite> resolveSprites(ListTag patterns) {
		List<TextureAtlasSprite> out = new ArrayList<>();
		var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
		net.minecraft.resources.ResourceLocation missing =
				net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
		boolean logOnce = LOGGED.compareAndSet(false, true);
		for (int i = 0; i < patterns.size(); i++) {
			CompoundTag c = patterns.getCompound(i);
			String hash = c.getString("Pattern");
			var holder = BannerPattern.byHash(hash);
			if (holder == null) continue;
			var keyOpt = holder.unwrapKey();
			if (keyOpt.isEmpty()) continue;
			ResourceKey<BannerPattern> rk = keyOpt.get();
			Material mat = Sheets.getBannerMaterial(rk);
			if (mat == null) continue;
			TextureAtlasSprite sprite = atlas.getSprite(mat.texture());
			boolean isMissing = sprite == null || sprite.contents().name().equals(missing);
			if (logOnce) {
				LOGGER.info("[MAW] saya pattern '{}' -> tex {} : {}",
						hash, mat.texture(), isMissing ? "MISSING (not on block atlas)" : "ok");
			}
			if (isMissing) {
				// ブロックアトラスに無い → 模様を描くと欠落テクスチャになるので描かない ( 地色のみ )
				return new ArrayList<>();
			}
			out.add(sprite);
		}
		return out;
	}

	/** 鞘本体 ( tintindex 0 ) の面を模様スプライトへ差し替えて複製。 */
	private static List<BakedQuad> buildOverlay(List<BakedQuad> baseQuads, List<TextureAtlasSprite> sprites) {
		List<BakedQuad> out = new ArrayList<>();
		for (BakedQuad q : baseQuads) {
			if (q.getTintIndex() != 0) continue; // 鞘本体の面だけ
			for (int layer = 0; layer < sprites.size(); layer++) {
				out.add(remap(q, sprites.get(layer), layer + 1, 0.005f * (layer + 1)));
			}
		}
		return out;
	}

	/** quad を複製し、 UV を src スプライト基準から dst スプライトへ相対マップ、 法線方向へ少し押し出す。 */
	private static BakedQuad remap(BakedQuad src, TextureAtlasSprite dst, int tint, float push) {
		int[] v = src.getVertices().clone();
		TextureAtlasSprite s = src.getSprite();
		float su0 = s.getU0(), su1 = s.getU1(), sv0 = s.getV0(), sv1 = s.getV1();
		float du0 = dst.getU0(), du1 = dst.getU1(), dv0 = dst.getV0(), dv1 = dst.getV1();
		Vec3i n = src.getDirection().getNormal();
		int stride = v.length / 4;
		for (int i = 0; i < 4; i++) {
			int o = i * stride;
			float x = Float.intBitsToFloat(v[o]);
			float y = Float.intBitsToFloat(v[o + 1]);
			float z = Float.intBitsToFloat(v[o + 2]);
			v[o] = Float.floatToRawIntBits(x + n.getX() * push);
			v[o + 1] = Float.floatToRawIntBits(y + n.getY() * push);
			v[o + 2] = Float.floatToRawIntBits(z + n.getZ() * push);
			v[o + 3] = 0xFFFFFFFF; // 頂点色は白 ( 色は tintindex+ItemColor で着ける )
			float u = Float.intBitsToFloat(v[o + 4]);
			float uv = Float.intBitsToFloat(v[o + 5]);
			float ur = (su1 != su0) ? (u - su0) / (su1 - su0) : 0f;
			float vr = (sv1 != sv0) ? (uv - sv0) / (sv1 - sv0) : 0f;
			v[o + 4] = Float.floatToRawIntBits(du0 + ur * (du1 - du0));
			v[o + 5] = Float.floatToRawIntBits(dv0 + vr * (dv1 - dv0));
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
