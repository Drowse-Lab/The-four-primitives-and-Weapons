package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class ScabbardCurioRenderer implements ICurioRenderer {

    // =============================================
    // デバッグモード: ScabbardDebugScreen (F8) でリアルタイム調整する時に使う
    // 通常時は false にしておく（ハードコード値が使われる）
    // =============================================
    // public static boolean DEBUG_MODE = true;
    public static boolean DEBUG_MODE = false;

    // --- デバッグ用 static変数（DEBUG_MODE=true の時のみ使用） ---
    // --- ベルト ---
    public static double beltX = 0.280, beltY = 0.660, beltZ = -0.240;
    public static float beltRotX = -100f, beltRotY = 180f, beltRotZ = 0f;
    public static float beltScaleX = 1.15f, beltScaleY = 1.3f, beltScaleZ = 1.15f;
    // --- 背中 ---
    public static double backX = -0.330, backY = 0.040, backZ = 0.130;
    public static float backRotX = 0f, backRotY = 270f, backRotZ = 140f;
    public static float backScaleX = 1.15f, backScaleY = 1.3f, backScaleZ = 1.15f;

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource bufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        poseStack.pushPose();

        // ボディモデルの座標系に変換
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            @SuppressWarnings("unchecked")
            HumanoidModel<LivingEntity> model = (HumanoidModel<LivingEntity>) humanoidModel;
            ICurioRenderer.followBodyRotations(slotContext.entity(), model);
            model.body.translateAndRotate(poseStack);
        }

        String slotId = slotContext.identifier();

        // =============================================
        // translate(X, Y, Z) — 位置の調整
        //   X: +で右、-で左（背面視点）
        //   Y: +で上、-で下
        //   Z: +で背中から離れる、-で体に近づく
        //
        // rotationDegrees — 回転の調整（translate地点を中心に回転）
        //   XP: 前後に傾く（+で前に倒れる、-で後ろに倒れる）
        //   YP: 左右に回す（+で左回転、-で右回転）※180で前後反転
        //   ZP: 斜めに傾ける（+で反時計回り、-で時計回り）
        //
        // scale(X, Y, Z) — サイズの調整
        //   Y を大きくすると縦に伸びる、X/Z を大きくすると横に太くなる
        // =============================================

        if ("belt".equals(slotId)) {
            if (DEBUG_MODE) {
                poseStack.translate(beltX, beltY, beltZ);
                poseStack.mulPose(Axis.XP.rotationDegrees(beltRotX));
                poseStack.mulPose(Axis.ZP.rotationDegrees(beltRotZ));
                poseStack.mulPose(Axis.YP.rotationDegrees(beltRotY));
                poseStack.scale(beltScaleX, beltScaleY, beltScaleZ);
            } else {
                // === ベルト（確定値） ===
                poseStack.translate(0.280, 0.660, -0.240);
                poseStack.mulPose(Axis.XP.rotationDegrees(-100f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(180f));
                poseStack.scale(1.15f, 1.3f, 1.15f);
            }
        } else if ("back".equals(slotId)) {
            if (DEBUG_MODE) {
                poseStack.translate(backX, backY, backZ);
                poseStack.mulPose(Axis.XP.rotationDegrees(backRotX));
                poseStack.mulPose(Axis.ZP.rotationDegrees(backRotZ));
                poseStack.mulPose(Axis.YP.rotationDegrees(backRotY));
                poseStack.scale(backScaleX, backScaleY, backScaleZ);
            } else {
                // === 背中（確定値） ===
                poseStack.translate(-0.330, 0.040, 0.130);
                poseStack.mulPose(Axis.XP.rotationDegrees(0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(140f));
                poseStack.mulPose(Axis.YP.rotationDegrees(270f));
                poseStack.scale(1.15f, 1.25f, 1.15f);
            }
        }

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                slotContext.entity().level(),
                slotContext.entity().getId()
        );

        poseStack.popPose();
    }
}
