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

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刀の柄 ( tintindex 1 の面 ) のテクスチャを「柄巻きデザイン」へ差し替える合成 BakedModel。
 * 各面に 柄巻きパターンを丸ごと貼る ( per-face full )。 tintindex 1 は維持するので
 * {@link KatanaColorClient} の柄色がそのまま乗る ( 巻き模様 × 柄色 )。
 */
@OnlyIn(Dist.CLIENT)
public final class KatanaTsukaModel implements BakedModel {

	private static final Map<String, KatanaTsukaModel> CACHE = new ConcurrentHashMap<>();

	private final BakedModel base;
	private final List<BakedQuad> swappedNull;
	private final Map<Direction, List<BakedQuad>> swappedBySide;

	private KatanaTsukaModel(BakedModel base, TextureAtlasSprite wrapSprite,
							TextureAtlasSprite tsubaSprite, boolean hideTsuba) {
		this.base = base;
		this.swappedBySide = new EnumMap<>(Direction.class);
		this.swappedNull = swap(base.getQuads(null, null, RandomSource.create(42L)), wrapSprite, tsubaSprite, hideTsuba);
		for (Direction d : Direction.values()) {
			swappedBySide.put(d, swap(base.getQuads(null, d, RandomSource.create(42L)), wrapSprite, tsubaSprite, hideTsuba));
		}
	}

	/** 柄巻き ( wrap ) と 鍔デザイン ( tsubaStyle ) を反映。 どちらも無く白鞘でもなければ base を返す。 */
	public static BakedModel maybe(BakedModel base, String wrap, String tsubaStyle) {
		if (base == null) return base;
		try {
			boolean hideTsuba = "shirasaya".equals(wrap);
			TextureAtlasSprite wrapSprite = sprite("katana_fitting/tsuka/", wrap);
			TextureAtlasSprite tsubaSprite = sprite("katana_fitting/tsuba/", tsubaStyle);
			if (wrapSprite == null && tsubaSprite == null && !hideTsuba) return base;
			String key = Integer.toHexString(System.identityHashCode(base)) + "@" + wrap + "#" + tsubaStyle;
			return CACHE.computeIfAbsent(key, k -> new KatanaTsukaModel(base, wrapSprite, tsubaSprite, hideTsuba));
		} catch (Throwable t) {
			return base;
		}
	}

	@Nullable
	private static TextureAtlasSprite sprite(String dir, String name) {
		if (name == null || name.isEmpty()) return null;
		var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
		TextureAtlasSprite s = atlas.getSprite(new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, dir + name));
		return (s == null || s.contents().name().equals(MissingTextureAtlasSprite.getLocation())) ? null : s;
	}

	/** 柄(tint1)の側面に柄巻きを、 鍔(tint2)に鍔デザインを貼る。 端面(頭/縁)は元のまま。 白鞘は鍔を消す。 */
	private static List<BakedQuad> swap(List<BakedQuad> srcQuads, TextureAtlasSprite wrapSprite,
										TextureAtlasSprite tsubaSprite, boolean hideTsuba) {
		List<BakedQuad> out = new ArrayList<>(srcQuads.size());
		for (BakedQuad q : srcQuads) {
			int ti = q.getTintIndex();
			if (ti == 1 && wrapSprite != null) {
				Direction d = q.getDirection();
				if (d == Direction.UP || d == Direction.DOWN) out.add(q); // 頭/縁は巻かない
				else out.add(remap(q, wrapSprite));
			} else if (ti == 2 && hideTsuba) {
				// 白鞘: 鍔を描かない
			} else if (ti == 2 && tsubaSprite != null) {
				out.add(remap(q, tsubaSprite)); // 鍔デザイン ( tint2 維持で色も乗る )
			} else {
				out.add(q);
			}
		}
		return out;
	}

	/** quad を複製し、 その面の自前UVを 0..1 に正規化して 柄巻きスプライト全体へ写す。 tint1 維持。 */
	private static BakedQuad remap(BakedQuad src, TextureAtlasSprite dst) {
		int[] v = src.getVertices().clone();
		int stride = v.length / 4;
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
		for (int i = 0; i < 4; i++) {
			int o = i * stride;
			float uFrac = (Float.intBitsToFloat(v[o + 4]) - umin) / uSpan;
			float vFrac = (Float.intBitsToFloat(v[o + 5]) - vmin) / vSpan;
			v[o + 4] = Float.floatToRawIntBits(du0 + uFrac * (du1 - du0));
			v[o + 5] = Float.floatToRawIntBits(dv0 + vFrac * (dv1 - dv0));
		}
		return new BakedQuad(v, src.getTintIndex(), src.getDirection(), dst, src.isShade());
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		List<BakedQuad> r = (side == null) ? swappedNull : swappedBySide.get(side);
		return (r != null) ? r : base.getQuads(state, side, rand);
	}

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
