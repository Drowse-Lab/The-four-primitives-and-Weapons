package minecraftarmorweapon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;

import minecraftarmorweapon.entity.MomentumHookEntity;

/**
 * Momentum Hookshot のフックエンティティレンダラー.
 *
 * 元データパックの hook は bat エンティティで、leash で player と繋がっている形 →
 * **vanilla の leash 描画**がそのまま使われている。
 *
 * Java 移植では:
 *  - フック本体は invisible (元 bat も極小で目立たないので)
 *  - ロープは vanilla MobRenderer.renderLeash と同じロジックで描画
 *  - **どちらの手で発射されたかを entity に問い合わせて hand position を切替**
 *    (vanilla FishingHookRenderer 同様の handMul 計算)
 */
public class MomentumHookRenderer extends EntityRenderer<MomentumHookEntity> {

    public MomentumHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MomentumHookEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Player owner = entity.getOwnerPlayer();
        if (owner != null) {
            renderLeash(entity, owner, partialTicks, poseStack, buffer);
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * vanilla MobRenderer.renderLeash + FishingHookRenderer の hand position 計算を組み合わせ.
     */
    private void renderLeash(MomentumHookEntity hook, Player owner, float partialTicks,
                             PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();

        // フック位置
        double hx = Mth.lerp(partialTicks, hook.xo, hook.getX());
        double hy = Mth.lerp(partialTicks, hook.yo, hook.getY());
        double hz = Mth.lerp(partialTicks, hook.zo, hook.getZ());

        // どちらの手で持っているか — vanilla FishingHookRenderer と同じロジック
        boolean rightArm = owner.getMainArm() == HumanoidArm.RIGHT;
        int handMul = rightArm ? 1 : -1;
        if (hook.isOffHand()) handMul = -handMul;

        // 手の位置を計算 (vanilla FishingHookRenderer の hand position 公式を移植)
        float yawRad = Mth.lerp(partialTicks, owner.yBodyRotO, owner.yBodyRot)
                     * (float)(Math.PI / 180.0);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double offsetX = handMul * 0.35;
        double offsetY = 0.8;
        double playerX = Mth.lerp(partialTicks, owner.xo, owner.getX());
        double playerY = Mth.lerp(partialTicks, owner.yo, owner.getY());
        double playerZ = Mth.lerp(partialTicks, owner.zo, owner.getZ());
        double handX = playerX - cosYaw * offsetX - sinYaw * 0.8;
        double handY = playerY + owner.getEyeHeight() - offsetY;
        double handZ = playerZ - sinYaw * offsetX + cosYaw * 0.8;

        float dx = (float)(handX - hx);
        float dy = (float)(handY - hy);
        float dz = (float)(handZ - hz);

        VertexConsumer vc = buffer.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();

        // 厚み計算
        float xzInv = Mth.invSqrt(dx * dx + dz * dz);
        float xzScale = xzInv * 0.025f / 2f;
        float xPart = dz * xzScale;
        float zPart = dx * xzScale;

        // ライティング
        BlockPos hookBP = BlockPos.containing(hx, hy, hz);
        BlockPos holderBP = BlockPos.containing(handX, handY, handZ);
        int hookBlockLight = hook.level().getBrightness(LightLayer.BLOCK, hookBP);
        int holderBlockLight = owner.level().getBrightness(LightLayer.BLOCK, holderBP);
        int hookSkyLight = hook.level().getBrightness(LightLayer.SKY, hookBP);
        int holderSkyLight = owner.level().getBrightness(LightLayer.SKY, holderBP);

        for (int i = 0; i <= 24; i++) {
            addLeashVertex(vc, matrix, dx, dy, dz,
                hookBlockLight, holderBlockLight, hookSkyLight, holderSkyLight,
                0.025f, 0.025f, xPart, zPart, i, false);
        }
        for (int i = 24; i >= 0; i--) {
            addLeashVertex(vc, matrix, dx, dy, dz,
                hookBlockLight, holderBlockLight, hookSkyLight, holderSkyLight,
                0.025f, 0.0f, xPart, zPart, i, true);
        }

        poseStack.popPose();
    }

    private static void addLeashVertex(VertexConsumer vc, Matrix4f matrix,
                                       float dx, float dy, float dz,
                                       int hookLight, int holderLight, int hookSky, int holderSky,
                                       float scale, float zScale, float xPart, float zPart,
                                       int idx, boolean reverse) {
        float t = (float) idx / 24f;
        int light = (int) Mth.lerp(t, (float) hookLight, (float) holderLight);
        int sky = (int) Mth.lerp(t, (float) hookSky, (float) holderSky);
        int packedLight = LightTexture.pack(light, sky);
        float color = (idx % 2 == (reverse ? 1 : 0)) ? 0.7f : 1.0f;
        float r = 0.5f * color;
        float g = 0.4f * color;
        float b = 0.3f * color;
        float x = dx * t;
        float y = dy > 0 ? dy * t * t : dy - dy * (1f - t) * (1f - t);
        float z = dz * t;
        vc.vertex(matrix, x - xPart, y + zScale, z + zPart)
          .color(r, g, b, 1.0f).uv2(packedLight).endVertex();
        vc.vertex(matrix, x + xPart, y + scale - zScale, z - zPart)
          .color(r, g, b, 1.0f).uv2(packedLight).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(MomentumHookEntity entity) {
        return new ResourceLocation("minecraft", "textures/block/iron_block.png");
    }
}
