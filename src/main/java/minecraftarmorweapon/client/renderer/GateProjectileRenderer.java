package minecraftarmorweapon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import minecraftarmorweapon.entity.GateProjectileEntity;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

/**
 * Gate飛び道具レンダラー — 金の直刀が切先を進行方向に向けて飛ぶ
 */
public class GateProjectileRenderer extends EntityRenderer<GateProjectileEntity> {

    private final ItemRenderer itemRenderer;
    private static final ItemStack DISPLAY_ITEM = ItemStack.EMPTY;

    public GateProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(GateProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 速度ベクトルから進行方向を計算
        double dx = Mth.lerp(partialTick, entity.xOld, entity.getX()) - entity.xOld;
        double dy = Mth.lerp(partialTick, entity.yOld, entity.getY()) - entity.yOld;
        double dz = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - entity.zOld;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float)(Math.atan2(dx, dz) * (180.0 / Math.PI));
        float pitch = (float)(Math.atan2(dy, horizontalDist) * (180.0 / Math.PI));

        // 切先が進行方向（下/前方）を向くように回転
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch + 90)); // +90で縦向き（切先が前方/下方）

        poseStack.scale(0.8f, 0.8f, 0.8f);

        ItemStack displayStack = new ItemStack(MinecraftArmorWeaponModItems.GOLD_TYOKUTO.get());
        itemRenderer.renderStatic(displayStack, ItemDisplayContext.FIXED,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GateProjectileEntity entity) {
        return new ResourceLocation("minecraft_armor_weapon", "textures/item/aa.png");
    }
}
