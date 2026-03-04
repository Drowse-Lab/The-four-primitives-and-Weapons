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

        if ("belt".equals(slotId)) {
            // 利き手でない方（左腰）の側面、反りが上向き
            poseStack.translate(-0.35, 0.55, 0.1);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-80));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.scale(1.4f, 1.65f, 1.4f);
        } else if ("back".equals(slotId)) {
            // 利き手側（右背中）、反りが上向き
            poseStack.translate(0.05, 0.25, 0.3);
            poseStack.mulPose(Axis.ZP.rotationDegrees(40));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.scale(1.1f, 1.3f, 1.1f);
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
