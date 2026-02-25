package minecraftarmorweapon.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import com.mojang.math.Axis;

import minecraftarmorweapon.entity.DarkProjectileEntity;

public class DarkProjectileRenderer extends EntityRenderer<DarkProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/soul_sand.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);

    public DarkProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DarkProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 回転アニメーション
        float rotation = (entity.tickCount + partialTicks) * 20;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation * 0.7f));

        // 球体を描画
        VertexConsumer vertexconsumer = buffer.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        // 暗い紫色の球体
        float size = 0.3f;
        int color = 0x4B0082; // インディゴ色

        // 6面を描画して球体を作る
        renderCube(vertexconsumer, matrix4f, matrix3f, size, color, packedLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderCube(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f, float size, int color, int light) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // 前面
        vertex(consumer, matrix4f, matrix3f, -size, -size, size, 0, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, -size, size, 1, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, size, size, 1, 0, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, -size, size, size, 0, 0, r, g, b, light);

        // 背面
        vertex(consumer, matrix4f, matrix3f, -size, size, -size, 0, 0, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, size, -size, 1, 0, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, -size, -size, 1, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, -size, -size, -size, 0, 1, r, g, b, light);

        // 上面
        vertex(consumer, matrix4f, matrix3f, -size, size, -size, 0, 0, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, -size, size, size, 0, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, size, size, 1, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, size, -size, 1, 0, r, g, b, light);

        // 下面
        vertex(consumer, matrix4f, matrix3f, -size, -size, size, 0, 0, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, -size, -size, -size, 0, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, -size, -size, 1, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, -size, size, 1, 0, r, g, b, light);

        // 右面
        vertex(consumer, matrix4f, matrix3f, size, -size, -size, 0, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, size, -size, 1, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, size, size, 1, 0, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, size, -size, size, 0, 0, r, g, b, light);

        // 左面
        vertex(consumer, matrix4f, matrix3f, -size, -size, size, 0, 0, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, -size, size, size, 1, 0, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, -size, size, -size, 1, 1, r, g, b, light);
        vertex(consumer, matrix4f, matrix3f, -size, -size, -size, 0, 1, r, g, b, light);
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f, float x, float y, float z, float u, float v, float r, float g, float b, int light) {
        consumer.vertex(matrix4f, x, y, z)
                .color(r, g, b, 0.8f)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(matrix3f, 0, 1, 0)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(DarkProjectileEntity entity) {
        return TEXTURE;
    }
}
