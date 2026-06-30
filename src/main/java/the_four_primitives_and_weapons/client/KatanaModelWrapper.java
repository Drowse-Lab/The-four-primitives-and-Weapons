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
	private final DynamicOverrides overrides = new DynamicOverrides();

	public KatanaModelWrapper(BakedModel wrapped) {
		this.wrapped = wrapped;
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
			return KatanaTsukaModel.maybe(base, KatanaFittings.getTsukaWrap(stack));
		}
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
