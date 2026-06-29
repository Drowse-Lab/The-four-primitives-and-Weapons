package the_four_primitives_and_weapons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import the_four_primitives_and_weapons.util.SayaStyles;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鞘本体 ( wrap = tintindex 0 の面 ) のテクスチャを「スタイル」のテクスチャへ差し替える合成 BakedModel。
 *
 * <p>木目鞘ならバニラ板材、 着せ鞘/刻鞘なら専用テクスチャを、 鞘本体のUVレイアウトを保ったまま貼る
 * ( 面の元UVを元 wrap スプライト基準で 0..1 に正規化 → スタイルスプライトへ写す )。 tintindex 0 は
 * 維持するので、 染色 ( {@link SayaColorClient} ) で色を乗せられる。 武器など tint 以外の面はそのまま。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SayaStyleModel implements BakedModel {

	private static final Map<String, SayaStyleModel> CACHE = new ConcurrentHashMap<>();

	private final BakedModel base;
	private final List<BakedQuad> swappedNull;
	private final Map<Direction, List<BakedQuad>> swappedBySide;

	private SayaStyleModel(BakedModel base, TextureAtlasSprite sprite, boolean fixedColor) {
		this.base = base;
		this.swappedBySide = new EnumMap<>(Direction.class);
		this.swappedNull = swap(base.getQuads(null, null, RandomSource.create(42L)), sprite, fixedColor);
		for (Direction d : Direction.values()) {
			swappedBySide.put(d, swap(base.getQuads(null, d, RandomSource.create(42L)), sprite, fixedColor));
		}
	}

	/**
	 * 仕立て ( wrap テクスチャ丸ごと差し替え ) を反映したモデルを返す。 既定 ( nuri ) や解決失敗なら base。
	 *
	 * @param style 仕立て ( wood_&lt;木材&gt; / kise / kizami / ishime / same / kuroro / roiro / shunuri / tame )
	 */
	public static BakedModel maybe(BakedModel base, String style) {
		if (base == null) return base;
		try {
			ResourceLocation loc = SayaStyles.sprite(style);
			if (loc == null) return base; // 既定 ( 塗鞘 ) = 差し替えなし
			var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
			TextureAtlasSprite sprite = atlas.getSprite(loc);
			if (sprite == null || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
				return base;
			}
			boolean fixedColor = SayaStyles.isFixedColor(style);
			String key = Integer.toHexString(System.identityHashCode(base)) + "@" + style;
			return CACHE.computeIfAbsent(key, k -> new SayaStyleModel(base, sprite, fixedColor));
		} catch (Throwable t) {
			return base;
		}
	}

	/** tintindex 0 の面を 仕立てスプライトへ差し替える。 漆系 ( fixedColor ) は tint を外して
	 *  テクスチャの色をそのまま出す。 素地系は tint 0 を維持して 染色 ( ItemColor ) が乗る。 */
	private static List<BakedQuad> swap(List<BakedQuad> srcQuads, TextureAtlasSprite dst, boolean fixedColor) {
		List<BakedQuad> out = new ArrayList<>(srcQuads.size());
		for (BakedQuad q : srcQuads) {
			if (q.getTintIndex() == 0) {
				out.add(remap(q, dst, fixedColor));
			} else {
				out.add(q);
			}
		}
		return out;
	}

	/** quad を複製し、 元 wrap スプライト基準の相対UVで 仕立てスプライトへ写す ( 位置はそのまま )。 */
	private static BakedQuad remap(BakedQuad src, TextureAtlasSprite dst, boolean fixedColor) {
		int[] v = src.getVertices().clone();
		TextureAtlasSprite s = src.getSprite();
		float su0 = s.getU0(), su1 = s.getU1(), sv0 = s.getV0(), sv1 = s.getV1();
		float du0 = dst.getU0(), du1 = dst.getU1(), dv0 = dst.getV0(), dv1 = dst.getV1();
		float suSpan = (su1 != su0) ? (su1 - su0) : 1f;
		float svSpan = (sv1 != sv0) ? (sv1 - sv0) : 1f;
		int stride = v.length / 4;
		for (int i = 0; i < 4; i++) {
			int o = i * stride;
			float uFrac = (Float.intBitsToFloat(v[o + 4]) - su0) / suSpan;
			float vFrac = (Float.intBitsToFloat(v[o + 5]) - sv0) / svSpan;
			v[o + 4] = Float.floatToRawIntBits(du0 + uFrac * (du1 - du0));
			v[o + 5] = Float.floatToRawIntBits(dv0 + vFrac * (dv1 - dv0));
		}
		// 漆系は tint を外し ( -1 ) テクスチャ色をそのまま。 素地系は tint 0 で染色可。
		int tint = fixedColor ? -1 : src.getTintIndex();
		return new BakedQuad(v, tint, src.getDirection(), dst, src.isShade());
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		// base には委譲せず、 差し替え済みの面集合を返す ( 元 wrap テクスチャは出さない )。
		List<BakedQuad> r = (side == null) ? swappedNull : swappedBySide.get(side);
		return (r != null) ? r : base.getQuads(state, side, rand);
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
