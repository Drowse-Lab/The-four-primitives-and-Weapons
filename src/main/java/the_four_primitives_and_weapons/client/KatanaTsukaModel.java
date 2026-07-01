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
 * 刀の拵えテクスチャ差し替え。 tintindex 1=柄 / 2=鍔 / 3=頭 / 4=縁 の面を、
 * それぞれ差し替えテクスチャへ <b>元UVに沿って(source-relative)</b> 貼る。
 *
 * <p>差し替えテクスチャは「武器テクスチャと同じUVレイアウトで描いた絵」を前提
 * ( 各面が自分のUV矩形の位置を差し替え先から取る )。 透明部分は元の武器が透ける。
 * tintindex はそのまま維持するので、 各部位の色 ( {@link KatanaColorClient} ) も乗る。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class KatanaTsukaModel implements BakedModel {

	private static final Map<String, KatanaTsukaModel> CACHE = new ConcurrentHashMap<>();
	private static final Map<String, KatanaTsukaModel> CACHE_GRAY = new ConcurrentHashMap<>();

	private final BakedModel base;
	private final List<BakedQuad> swappedNull;
	private final Map<Direction, List<BakedQuad>> swappedBySide;

	private KatanaTsukaModel(BakedModel base, TextureAtlasSprite[] byTint, boolean hideTsuba) {
		this.base = base;
		this.swappedBySide = new EnumMap<>(Direction.class);
		this.swappedNull = swap(base.getQuads(null, null, RandomSource.create(42L)), byTint, hideTsuba);
		for (Direction d : Direction.values()) {
			swappedBySide.put(d, swap(base.getQuads(null, d, RandomSource.create(42L)), byTint, hideTsuba));
		}
	}

	// 色付け用: 色を設定した部位の面を「その面自身のテクスチャのグレー版(_gray)」へ差し替える
	// ( グレー地に ItemColor の乗算で 任意の16進色が綺麗に乗る = 軍服の染色と同じ方式 )。
	private KatanaTsukaModel(BakedModel base, boolean[] coloredByTint) {
		this.base = base;
		this.swappedBySide = new EnumMap<>(Direction.class);
		this.swappedNull = swapGray(base.getQuads(null, null, RandomSource.create(42L)), coloredByTint);
		for (Direction d : Direction.values()) {
			swappedBySide.put(d, swapGray(base.getQuads(null, d, RandomSource.create(42L)), coloredByTint));
		}
	}

	/** 柄(wrap)/鍔(tsubaStyle)/頭(kashira)/縁(fuchi) のデザインを反映。 どれも無く白鞘でもなければ base。 */
	public static BakedModel maybe(BakedModel base, String wrap, String tsubaStyle, String kashira, String fuchi) {
		if (base == null) return base;
		try {
			boolean hideTsuba = "shirasaya".equals(wrap);
			TextureAtlasSprite[] byTint = new TextureAtlasSprite[5]; // index = tintindex
			byTint[1] = sprite("katana_fitting/tsuka/", wrap);
			byTint[2] = sprite("katana_fitting/tsuba/", tsubaStyle);
			byTint[3] = sprite("katana_fitting/kasira/", kashira);
			byTint[4] = sprite("katana_fitting/fuchi/", fuchi);
			boolean any = false;
			for (TextureAtlasSprite s : byTint) if (s != null) any = true;
			if (!any && !hideTsuba) return base;
			String key = Integer.toHexString(System.identityHashCode(base))
					+ "@" + wrap + "#" + tsubaStyle + "/" + kashira + "|" + fuchi;
			return CACHE.computeIfAbsent(key, k -> new KatanaTsukaModel(base, byTint, hideTsuba));
		} catch (Throwable t) {
			return base;
		}
	}

	/**
	 * 色付け: tintindex t ( 1=柄/2=鍔/3=頭/4=縁 ) が「色設定あり」の面を、 <b>その面自身のテクスチャの
	 * グレー版(_gray)</b> ( 例: tuka→tuka_gray, kasira→kasira_gray ) へ差し替える。 元と同レイアウトなので崩れず、
	 * グレー地に {@link KatanaColorClient} の乗算tintで 任意の16進色が綺麗に乗る ( 軍服の染色と同じ )。
	 * モデルが部位とテクスチャを混在させていても正しく機能する。
	 */
	public static BakedModel grayForTint(BakedModel base, boolean[] coloredByTint) {
		if (base == null) return base;
		boolean any = false;
		for (boolean b : coloredByTint) if (b) any = true;
		if (!any) return base;
		try {
			StringBuilder sb = new StringBuilder(Integer.toHexString(System.identityHashCode(base))).append("^gry");
			for (boolean b : coloredByTint) sb.append(b ? '1' : '0');
			return CACHE_GRAY.computeIfAbsent(sb.toString(), k -> new KatanaTsukaModel(base, coloredByTint));
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

	/** tintindex 1〜4 の面を、 対応する差し替えテクスチャへ元UVで貼る。 白鞘は鍔(2)を消す。 */
	private static List<BakedQuad> swap(List<BakedQuad> srcQuads, TextureAtlasSprite[] byTint, boolean hideTsuba) {
		List<BakedQuad> out = new ArrayList<>(srcQuads.size());
		for (BakedQuad q : srcQuads) {
			int ti = q.getTintIndex();
			if (ti == 2 && hideTsuba) {
				continue; // 白鞘: 鍔を描かない
			}
			TextureAtlasSprite dst = (ti >= 1 && ti <= 4) ? byTint[ti] : null;
			out.add(dst != null ? remap(q, dst) : q);
		}
		return out;
	}

	/** 各面を「自分自身のテクスチャのグレー版(_gray)」へ ( 色設定ありの tintindex のみ )。 */
	private static List<BakedQuad> swapGray(List<BakedQuad> srcQuads, boolean[] coloredByTint) {
		List<BakedQuad> out = new ArrayList<>(srcQuads.size());
		for (BakedQuad q : srcQuads) {
			int ti = q.getTintIndex();
			if (ti >= 1 && ti <= 4 && coloredByTint[ti]) {
				TextureAtlasSprite dst = grayVariant(q.getSprite());
				out.add(dst != null ? remap(q, dst) : q);
			} else {
				out.add(q);
			}
		}
		return out;
	}

	/** スプライトの "_gray" 版を返す ( 無ければ null )。 */
	@Nullable
	private static TextureAtlasSprite grayVariant(TextureAtlasSprite src) {
		if (src == null) return null;
		ResourceLocation n = src.contents().name();
		ResourceLocation gray = new ResourceLocation(n.getNamespace(), n.getPath() + "_gray");
		var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
		TextureAtlasSprite s = atlas.getSprite(gray);
		return (s == null || s.contents().name().equals(MissingTextureAtlasSprite.getLocation())) ? null : s;
	}

	/** quad を複製し、 元スプライト基準の相対UV ( source-relative ) で 差し替え先へ写す。 tintindex 維持。 */
	private static BakedQuad remap(BakedQuad src, TextureAtlasSprite dst) {
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
