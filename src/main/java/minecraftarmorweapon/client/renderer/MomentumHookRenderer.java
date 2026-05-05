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
 *    (色: 茶色レザー (0.5,0.4,0.3) ×明暗交互, 厚み 0.025, 放物線 sag, ライティング有り)
 */
public class MomentumHookRenderer extends EntityRenderer<MomentumHookEntity> {

    public MomentumHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MomentumHookEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // フック本体は描画しない (元 bat は invisible に近い)
        Player owner = entity.getOwnerPlayer();
        if (owner != null) {
            renderLeash(entity, owner, partialTicks, poseStack, buffer);
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * vanilla MobRenderer.renderLeash の移植.
     * フックエンティティの origin から player の rope hold position まで rope を描画する.
     */
    private void renderLeash(MomentumHookEntity hook, Player owner, float partialTicks,
                             PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();

        // フック位置 (現 entity の補間位置)
        double hx = Mth.lerp(partialTicks, hook.xo, hook.getX());
        double hy = Mth.lerp(partialTicks, hook.yo, hook.getY());
        double hz = Mth.lerp(partialTicks, hook.zo, hook.getZ());

        // プレイヤーの rope hold position (vanilla 標準実装)
        net.minecraft.world.phys.Vec3 holdPos = owner.getRopeHoldPosition(partialTicks);
        float dx = (float)(holdPos.x - hx);
        float dy = (float)(holdPos.y - hy);
        float dz = (float)(holdPos.z - hz);

        VertexConsumer vc = buffer.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();

        // 厚み計算: 進行方向に垂直な offset を 0.025/2 に設定
        float xzInv = Mth.invSqrt(dx * dx + dz * dz);
        float xzScale = xzInv * 0.025f / 2f;
        float xPart = dz * xzScale;
        float zPart = dx * xzScale;

        // ライティング: 両端の block/sky light を線形補間
        BlockPos hookBP = BlockPos.containing(hx, hy, hz);
        BlockPos holderBP = BlockPos.containing(holdPos.x, holdPos.y, holdPos.z);
        int hookBlockLight = hook.level().getBrightness(LightLayer.BLOCK, hookBP);
        int holderBlockLight = owner.level().getBrightness(LightLayer.BLOCK, holderBP);
        int hookSkyLight = hook.level().getBrightness(LightLayer.SKY, hookBP);
        int holderSkyLight = owner.level().getBrightness(LightLayer.SKY, holderBP);

        // 24 セグメントで rope を描画 (vanilla と同じ解像度)
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

    /** vanilla MobRenderer.addVertexPair 相当. */
    private static void addLeashVertex(VertexConsumer vc, Matrix4f matrix,
                                       float dx, float dy, float dz,
                                       int hookLight, int holderLight, int hookSky, int holderSky,
                                       float scale, float zScale, float xPart, float zPart,
                                       int idx, boolean reverse) {
        float t = (float) idx / 24f;
        int light = (int) Mth.lerp(t, (float) hookLight, (float) holderLight);
        int sky = (int) Mth.lerp(t, (float) hookSky, (float) holderSky);
        int packedLight = LightTexture.pack(light, sky);
        // 明暗交互で rope の編み目感を出す (vanilla と同じ)
        float color = (idx % 2 == (reverse ? 1 : 0)) ? 0.7f : 1.0f;
        float r = 0.5f * color;
        float g = 0.4f * color;
        float b = 0.3f * color;
        float x = dx * t;
        // 放物線 sag (vanilla: y = dy*t² if dy>0 else dy - dy*(1-t)²)
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
