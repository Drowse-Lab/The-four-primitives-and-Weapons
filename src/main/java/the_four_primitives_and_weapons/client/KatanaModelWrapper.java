package the_four_primitives_and_weapons.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import the_four_primitives_and_weapons.util.KatanaFittings;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 刀アイテムの動的モデル: 元の overrides ( custom_model_data 等 ) を解決した上で、
 * 柄巻きデザイン ( {@link KatanaFittings#getTsukaWrap} ) に応じて柄テクスチャを差し替える。
 */
@OnlyIn(Dist.CLIENT)
public final class KatanaModelWrapper implements BakedModel {

	private final BakedModel wrapped;
	private final boolean katanaDefaults;
	private final boolean colorBlackMode;
	private final DynamicOverrides overrides = new DynamicOverrides();

	public KatanaModelWrapper(BakedModel wrapped) {
		this(wrapped, false, false);
	}

	/** katanaDefaults=true: NBT未設定時に刀用の既定デザイン(tuka/tuba/kasira/fuchi)を出す。 */
	public KatanaModelWrapper(BakedModel wrapped, boolean katanaDefaults) {
		this(wrapped, katanaDefaults, false);
	}

	/**
	 * @param colorBlackMode 箱UVモデル(iron_katana)用。 部位色が「ほぼ黒」の時、 乗算tintで潰さず
	 *                       専用の黒テクスチャ(tuka_black/tuba_black/kasira_black)へ差し替える。
	 */
	public KatanaModelWrapper(BakedModel wrapped, boolean katanaDefaults, boolean colorBlackMode) {
		this.wrapped = wrapped;
		this.katanaDefaults = katanaDefaults;
		this.colorBlackMode = colorBlackMode;
	}

	private final class DynamicOverrides extends ItemOverrides {
		@Override
		public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
								  @Nullable LivingEntity entity, int seed) {
			BakedModel base = wrapped;
			ItemOverrides orig = wrapped.getOverrides();
			if (orig != null && orig != this) {
				BakedModel resolved = orig.resolve(wrapped, stack, level, entity, seed);
				if (resolved != null) base = resolved;
			}
			if (colorBlackMode) {
				// 箱UV(iron_katana): 部位に色を設定したら 各面を「その面自身のテクスチャのグレー版」へ差し替える。
				// グレー地に KatanaColorClient の乗算tintで 任意の16進色が綺麗に乗る ( 軍服と同じ )。
				// 未設定の部位は 元の菱デザインのまま。
				int[] mode = new int[5]; // 0=そのまま / 1=グレー版(tintで任意色) / 2=暗版(模様入りの黒)
				mode[1] = variantMode(KatanaFittings.tsukaRgb(stack));
				mode[2] = variantMode(KatanaFittings.tsubaRgb(stack));
				mode[3] = variantMode(KatanaFittings.kashiraRgb(stack));
				// 部位ごとのデザイン ( 拵え台で選択、 同フォルダの別ファイルへ差し替え )
				String[] design = new String[5]; // 1=柄(grip) 2=鍔(guard) 3=頭(pommel)
				design[1] = KatanaFittings.getTsukaWrap(stack);
				design[2] = KatanaFittings.getTsubaStyle(stack);
				design[3] = KatanaFittings.getKashiraStyle(stack);
				return KatanaTsukaModel.styleColor(base, design, mode);
			}
			String wrap = or(KatanaFittings.getTsukaWrap(stack), "tuka");
			String tsuba = or(KatanaFittings.getTsubaStyle(stack), "tuba");
			String kashira = or(KatanaFittings.getKashiraStyle(stack), "kasira");
			String fuchi = or(KatanaFittings.getFuchiStyle(stack), "fuchi");
			return KatanaTsukaModel.maybe(base, wrap, tsuba, kashira, fuchi);
		}

		private String or(String v, String def) {
			if (v != null && !v.isEmpty()) return v;
			return katanaDefaults ? def : "";
		}
	}

	/** 色→差し替えモード: 未設定=0 / ほぼ黒=2(模様入り暗版) / それ以外=1(グレー版+tint)。 */
	private static int variantMode(int rgb) {
		if (rgb < 0) return 0;
		return KatanaFittings.isNearBlack(rgb) ? 2 : 1;
	}

	@Override public ItemOverrides getOverrides() { return overrides; }

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		return wrapped.getQuads(state, side, rand);
	}

	@Override public boolean useAmbientOcclusion() { return wrapped.useAmbientOcclusion(); }
	@Override public boolean isGui3d() { return wrapped.isGui3d(); }
	@Override public boolean usesBlockLight() { return wrapped.usesBlockLight(); }
	@Override public boolean isCustomRenderer() { return wrapped.isCustomRenderer(); }
	@SuppressWarnings("deprecation")
	@Override public TextureAtlasSprite getParticleIcon() { return wrapped.getParticleIcon(); }
	@SuppressWarnings("deprecation")
	@Override public ItemTransforms getTransforms() { return wrapped.getTransforms(); }

	@Override
	public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftFlip) {
		wrapped.applyTransform(context, poseStack, leftFlip);
		return this;
	}
}
