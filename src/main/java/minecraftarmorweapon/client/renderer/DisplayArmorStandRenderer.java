package minecraftarmorweapon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.decoration.ArmorStand;

import minecraftarmorweapon.entity.DisplayArmorStandEntity;

/**
 * 透明アーマースタンドのレンダラ。pose に応じて全体回転＋平行移動。
 *   POSE_HORIZONTAL : 体を右に倒す（Z軸 -90°）。さらに刀掛けモデルを下に描画。
 *   POSE_HANGING    : 上下逆さ。
 *   POSE_STUCK      : 上下逆さ + 沈める。
 */
public class DisplayArmorStandRenderer extends ArmorStandRenderer {

	public DisplayArmorStandRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(ArmorStand entity, float entityYaw, float partialTick,
					   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		if (entity instanceof DisplayArmorStandEntity das) {
			int pose = das.getDisplayPose();

			// HORIZONTAL: 先に刀掛けモデルを描画（エンティティのポーズ回転は適用しない）
			if (pose == DisplayArmorStandEntity.POSE_HORIZONTAL) {
				poseStack.pushPose();
				// エンティティYawに合わせる（vanilla LivingEntityRenderer 同様の式）
				poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
				KatanaRackRenderer.render(poseStack, buffer, packedLight);
				poseStack.popPose();
			}

			poseStack.pushPose();
			switch (pose) {
				case DisplayArmorStandEntity.POSE_HORIZONTAL: {
					// 体を右に倒した後、手位置を刀掛けの柱の上 (中央, ≈7/16) に来るよう平行移動。
					// 右手は entity local で (+0.31, 0.9, 0) → Z軸-90°後 (0.9, -0.31, 0)。
					// それを (0, 0.45, 0) に持っていくには translate(-0.9, 0.76, 0) が必要。
					poseStack.translate(-0.9D, 0.76D, 0.0D);
					poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
					break;
				}
				case DisplayArmorStandEntity.POSE_HANGING: {
					poseStack.translate(0.0D, 1.95D, 0.0D);
					poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
					break;
				}
				case DisplayArmorStandEntity.POSE_STUCK: {
					poseStack.translate(0.0D, 0.6D, 0.0D);
					poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
					break;
				}
				default:
					break;
			}
			super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
			poseStack.popPose();
			return;
		}
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}
}
