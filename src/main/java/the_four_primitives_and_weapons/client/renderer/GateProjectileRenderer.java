package the_four_primitives_and_weapons.client.renderer;

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

import the_four_primitives_and_weapons.entity.GateProjectileEntity;
import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

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
    private static ItemStack sharedStack;
    

    public GateProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(GateProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 投げナイフと同じく entity の YRot/XRot (モーションから projectile 基底が
        // 毎tick更新) を使って 3D で整列する。これで上下方向にも切先が向く。
        float yaw   = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // 進行方向に整列 (矢方式): Y 軸で左右, Z 軸で上下
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90f + YAW_OFFSET));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + PITCH_OFFSET));
        // 金の直刀 (THIRD_PERSON_RIGHT_HAND) のモデル基底姿勢に合わせて補正
        poseStack.mulPose(Axis.XP.rotationDegrees(0f));
        // モデルの刃を motion 方向に揃える (GROUND display の-180°反転を考慮して+90°)
        poseStack.mulPose(Axis.ZP.rotationDegrees(90f));

        poseStack.scale(SCALE_X, SCALE_Y, SCALE_Z);

        // 毎フレームの new ItemStack を避けて使い回す ( 表示専用で内容は不変 )
        if (sharedStack == null) {
            sharedStack = new ItemStack(TheFourPrimitivesAndWeaponsModItems.GOLD_TYOKUTO.get());
        }
        ItemStack displayStack = sharedStack;
        Minecraft.getInstance().getItemRenderer().renderStatic(
                displayStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GateProjectileEntity entity) {
        return new ResourceLocation("the_four_primitives_and_weapons", "textures/item/aa.png");
    }
}
