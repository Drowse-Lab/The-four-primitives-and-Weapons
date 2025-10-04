package minecraftarmorweapon.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import minecraftarmorweapon.entity.TornadoEntity;

/**
 * Renderer for TornadoEntity
 * The tornado effect is created using particles, so this renderer is mostly invisible
 */
public class TornadoRenderer extends EntityRenderer<TornadoEntity> {

    public TornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f; // No shadow
    }

    @Override
    public ResourceLocation getTextureLocation(TornadoEntity entity) {
        // Return a dummy texture - tornado is rendered with particles
        return new ResourceLocation("minecraft", "textures/entity/creeper/creeper.png");
    }

    @Override
    public void render(TornadoEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // The tornado effect is created using particles in the entity's tick method
        // This renderer doesn't need to draw anything
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }
}