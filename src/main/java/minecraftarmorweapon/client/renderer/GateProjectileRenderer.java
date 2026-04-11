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

import net.minecraft.client.Minecraft;

import minecraftarmorweapon.entity.GateProjectileEntity;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

/**
 * Gate飛び道具レンダラー — 金の直刀が切先を進行方向に向けて飛ぶ
 */
public class GateProjectileRenderer extends EntityRenderer<GateProjectileEntity> {

    private final ItemRenderer itemRenderer;
    private static final ItemStack DISPLAY_ITEM = ItemStack.EMPTY;

    // @RotationParams(Gate直刀, cmd=/test gaterot {YAW_OFFSET} {PITCH_OFFSET} {ROLL_OFFSET} {SCALE_X} {SCALE_Y} {SCALE_Z})
    public static float YAW_OFFSET = 0f; // Y軸微調整
    public static float PITCH_OFFSET = 0f; // X軸微調整
    public static float ROLL_OFFSET = 0f; // Z軸微調整
    public static float SCALE_X = 0.8f; // Xサイズ
    public static float SCALE_Y = 0.8f; // Yサイズ
    public static float SCALE_Z = 0.8f; // Zサイズ
    // @EndRotationParams

    public GateProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(GateProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 速度ベクトルからYawを計算（水平方向の進行方向に追従）
        net.minecraft.world.phys.Vec3 vel = entity.getDeltaMovement();
        float yaw = (float)(Math.atan2(vel.x, vel.z) * (180.0 / Math.PI));

        // FlyingAttackerRenderer の剣表示と同じ方式
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw + YAW_OFFSET));
        poseStack.mulPose(Axis.XP.rotationDegrees(90f + PITCH_OFFSET));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-180f + ROLL_OFFSET));

        poseStack.scale(SCALE_X, SCALE_Y, SCALE_Z);

        ItemStack displayStack = new ItemStack(MinecraftArmorWeaponModItems.GOLD_TYOKUTO.get());
        Minecraft.getInstance().getItemRenderer().renderStatic(
                displayStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GateProjectileEntity entity) {
        return new ResourceLocation("minecraft_armor_weapon", "textures/item/aa.png");
    }
}
