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
 */
public class ThrowingKnifeRenderer extends EntityRenderer<ThrowingKnifeEntity> {

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

        // 進行方向に整列 (矢方式 — 投げた方向そのままの姿勢で飛ぶ)
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

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
