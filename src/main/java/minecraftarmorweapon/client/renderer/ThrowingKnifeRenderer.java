package minecraftarmorweapon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import minecraftarmorweapon.entity.ThrowingKnifeEntity;
import minecraftarmorweapon.init.CustomEntityInit;

/**
 * 投げナイフ飛翔体レンダラー — 進行方向に切先を向けて飛び、
 * ブロックに刺さった時はその姿勢のまま残る。
 *
 * 向きが合わない場合は下のパラメータ (YAW_OFFSET / PITCH_OFFSET / ROLL_OFFSET / SCALE) を編集して再ビルドする。
 */
public class ThrowingKnifeRenderer extends EntityRenderer<ThrowingKnifeEntity> {

    // @RotationParams(ThrowingKnife)
    public static float YAW_OFFSET = 0f;    // Y軸回転 (進行方向左右の追加回転)
    public static float PITCH_OFFSET = 0f;  // X軸回転 (上下の追加回転)
    public static float ROLL_OFFSET = 0f;   // Z軸回転 (進行方向まわりのロール)
    public static float SCALE = 1.0f;       // 表示サイズ
    // @EndRotationParams

    // SCREW: 進行方向を軸にリップタイド風の連続ロール (度/tick, 20tick=1秒)
    // 90°/tick = 1800°/sec = 5回転/秒。リップタイドと同等以上の高速スピン
    public static float SCREW_SPIN_DEG_PER_TICK = 90f;

    public ThrowingKnifeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ThrowingKnifeEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ItemStack stack = new ItemStack(CustomEntityInit.THROWING_KNIFE.get());

        poseStack.pushPose();

        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        // 進行方向に整列 (矢方式)
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90f + YAW_OFFSET));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + PITCH_OFFSET));
        // ロール (進行方向まわりに刃の表裏を回す)
        // SCREW かつ飛翔中のみ、連続回転アニメーションを付加 (リップタイド風スピン)
        float roll = ROLL_OFFSET;
        if (entity.getKnifeType() == ThrowingKnifeEntity.KnifeType.SCREW && !entity.isStuck()) {
            roll += (entity.tickCount + partialTicks) * SCREW_SPIN_DEG_PER_TICK;
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));
        // モデルの刃を motion 方向に揃える (GROUND display の-180°反転を考慮して+90°)
        poseStack.mulPose(Axis.ZP.rotationDegrees(90f));

        if (SCALE != 1.0f) poseStack.scale(SCALE, SCALE, SCALE);

        Minecraft.getInstance().getItemRenderer().renderStatic(
            stack,
            ItemDisplayContext.GROUND,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            entity.level(),
            entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrowingKnifeEntity entity) {
        return new ResourceLocation("minecraft", "textures/block/iron_block.png");
    }
}
