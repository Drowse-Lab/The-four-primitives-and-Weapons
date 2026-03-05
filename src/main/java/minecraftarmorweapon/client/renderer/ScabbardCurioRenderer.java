package minecraftarmorweapon.client.renderer;

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
            // プレイヤーのボディ回転に追従
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
        // ZP.rotationDegrees(角度) — 傾きの調整
        //   +で反時計回り（柄が左上へ）、-で時計回り（柄が右上へ）
        //
        // YP.rotationDegrees(180) — アイテムモデルの前後反転（基本変更不要）
        //
        // scale(X, Y, Z) — サイズの調整
        //   Y を大きくすると縦に伸びる、X/Z を大きくすると横に太くなる
        // =============================================

        if ("belt".equals(slotId)) {
            // === ベルト（左腰） ===
            poseStack.translate(-0.35, 0.55, 0.1);   // 左腰の位置
            poseStack.mulPose(Axis.ZP.rotationDegrees(-80));  // ほぼ水平
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.scale(1.4f, 1.65f, 1.4f);
        } else if ("back".equals(slotId)) {
            // === 背中（斜め掛け） ===
            poseStack.translate(0.05, 0.2, 0.15);    // 背中の中央やや右
            poseStack.mulPose(Axis.ZP.rotationDegrees(35));   // 斜め35度
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.scale(1.15f, 1.35f, 1.15f);
        }

        // 鞘のアイテムモデルを描画（CustomModelDataが反映される）
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
