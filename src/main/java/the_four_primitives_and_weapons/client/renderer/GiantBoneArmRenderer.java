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
import net.minecraft.util.Mth;

import the_four_primitives_and_weapons.entity.GiantBoneArmEntity;
import the_four_primitives_and_weapons.entity.model.GiantBoneArmModel;
import the_four_primitives_and_weapons.entity.model.BoneCageModel;

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
	/** 侵食属性時に骨表面へ重ねるガラス被膜 (ピンク〜紫に色付けして使用)。 */
	private static final ResourceLocation GLASS_TEXTURE = new ResourceLocation(
			"the_four_primitives_and_weapons", "textures/entities/giant_bone_arm_glass.png");

	/** 巨大化倍率 (1単位=1/16ブロック。モデル全長 約85単位 ≒ 5.3ブロック × 倍率)。 */
	private static final float RENDER_SCALE = 1.8f;
	/** 向き微調整 (度)。腕が左右逆を向く場合に 180 などへ。 */
	private static final float YAW_OFFSET = 0f;
	/** ガラス被膜が最大になる侵食レベル。 */
	private static final float GLASS_MAX_LEVEL = 12f;

	private final GiantBoneArmModel model;
	private final BoneCageModel cageModel;

	public GiantBoneArmRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0f;
		this.model = new GiantBoneArmModel(context.bakeLayer(GiantBoneArmModel.LAYER_LOCATION));
		this.cageModel = new BoneCageModel(context.bakeLayer(BoneCageModel.LAYER_LOCATION));
	}

	// ===== ガード(手だけで横向きに包む)用の配置 — 実機調整しやすいよう定数化 =====
	/** ガード(手)の倍率。大きめにしてプレイヤーを包む。 */
	private static final float GUARD_SCALE = 1.35f;
	/** ガード(手)の高さ (胸の高さ, ブロック)。 */
	private static final float GUARD_Y = 1.0f;
	/** 手(手のひら)をプレイヤー中心へ寄せる前後オフセット (モデル単位)。隠れた前腕は奥へ逃げる。 */
	private static final float GUARD_HAND_Z = 3.5f;
	/** 両手ガードの左右の手のずらし量 (ブロック)。 */
	private static final float GUARD_SIDE_X = 0.35f;

	@Override
	public void render(GiantBoneArmEntity entity, float entityYaw, float partialTicks,
					   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		float age = entity.tickCount + partialTicks;
		poseStack.pushPose();

		if (entity.isGuard()) {
			// ガード: 左右2体の手で両側からプレイヤーを包む。
			int side = entity.getGuardSide(); // -1=左手 / +1=右手
			poseStack.translate(0.0, GUARD_Y, 0.0);
			poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getCastYaw() + YAW_OFFSET));
			poseStack.translate(side * GUARD_SIDE_X, 0.0, 0.0); // 左右に振り分け
			poseStack.scale(GUARD_SCALE, GUARD_SCALE, GUARD_SCALE);
			poseStack.scale(-1.0f, -1.0f, 1.0f);
			if (side < 0)
				poseStack.scale(-1.0f, 1.0f, 1.0f); // 左手にミラー
			poseStack.translate(0.0, 0.0, -GUARD_HAND_Z); // 手をプレイヤー中心へ(前腕は奥へ)
			this.model.setupAnim(entity, 0f, 0f, age, 0f, 0f);
			renderModel(entity, poseStack, buffer, packedLight);
		} else {
			// 通常 (薙ぎ→叩き): cast 方向へ向けて巨大化。
			poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getCastYaw() + YAW_OFFSET));
			poseStack.scale(RENDER_SCALE, RENDER_SCALE, RENDER_SCALE);
			poseStack.scale(-1.0f, -1.0f, 1.0f);
			this.model.setupAnim(entity, 0f, 0f, age, 0f, 0f);
			renderModel(entity, poseStack, buffer, packedLight);
		}

		poseStack.popPose();
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
	}

	/** 骨本体 + (侵食属性なら) ガラス結晶/被膜 の2パス描画。 */
	private void renderModel(GiantBoneArmEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		// 骨本体 (ガラス結晶は骨パスでは隠す)
		this.model.setShardsVisible(false);
		VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY,
				1.0f, 1.0f, 1.0f, 1.0f);

		// 侵食属性: 骨から生えるピンク〜紫のガラス結晶 + 表面の薄い被膜
		int level = entity.getCorrosionLevel();
		if (level > 0) {
			this.model.setShardsVisible(true);
			float cov = Math.min(level / GLASS_MAX_LEVEL, 1.0f);
			float r = Mth.lerp(cov, 1.0f, 0.62f);   // ピンク → 紫
			float g = Mth.lerp(cov, 0.45f, 0.22f);
			float b = Mth.lerp(cov, 0.78f, 0.96f);
			// 透明度: レベルに比例して不透明に。Lv100 で約0.8 (2割透明)。
			float alpha = Math.min(0.30f + level * 0.005f, 0.80f);
			float glassScale = 1.0f + 0.05f * cov;  // 表面被膜の厚み
			poseStack.pushPose();
			poseStack.scale(glassScale, glassScale, glassScale);
			VertexConsumer gvc = buffer.getBuffer(RenderType.entityTranslucent(GLASS_TEXTURE));
			this.model.renderToBuffer(poseStack, gvc,
					net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
					r, g, b, alpha);
			poseStack.popPose();
		}
	}

	@Override
	public ResourceLocation getTextureLocation(GiantBoneArmEntity entity) {
		return TEXTURE;
	}
}
