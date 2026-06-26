package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import the_four_primitives_and_weapons.entity.GiantBoneArmEntity;
import the_four_primitives_and_weapons.entity.model.GiantBoneArmModel;

/**
 * 「巨骨の腕」レンダラー。
 *
 * - 肩 (entity 位置) を起点に、cast 時の向き (castYaw) へ腕を伸ばす。
 * - 巨大化のため {@link #RENDER_SCALE} 倍。
 * - 向き/上下が合わない場合は YAW_OFFSET / RENDER_SCALE / 反転符号で微調整可能。
 */
public class GiantBoneArmRenderer extends EntityRenderer<GiantBoneArmEntity> {

	private static final ResourceLocation TEXTURE = new ResourceLocation(
			"the_four_primitives_and_weapons", "textures/entities/giant_bone_arm.png");

	/** 巨大化倍率 (1単位=1/16ブロック。モデル全長 約85単位 ≒ 5.3ブロック × 倍率)。 */
	private static final float RENDER_SCALE = 1.8f;
	/** 向き微調整 (度)。腕が左右逆を向く場合に 180 などへ。 */
	private static final float YAW_OFFSET = 0f;

	private final GiantBoneArmModel model;

	public GiantBoneArmRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0f;
		this.model = new GiantBoneArmModel(context.bakeLayer(GiantBoneArmModel.LAYER_LOCATION));
	}

	@Override
	public void render(GiantBoneArmEntity entity, float entityYaw, float partialTicks,
					   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		poseStack.pushPose();

		// cast 方向へ向ける
		poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getCastYaw() + YAW_OFFSET));
		// 巨大化 + モデル空間へ反転
		poseStack.scale(RENDER_SCALE, RENDER_SCALE, RENDER_SCALE);
		poseStack.scale(-1.0f, -1.0f, 1.0f);

		float age = entity.tickCount + partialTicks;
		this.model.setupAnim(entity, 0f, 0f, age, 0f, 0f);

		VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY,
				1.0f, 1.0f, 1.0f, 1.0f);

		poseStack.popPose();
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(GiantBoneArmEntity entity) {
		return TEXTURE;
	}
}
