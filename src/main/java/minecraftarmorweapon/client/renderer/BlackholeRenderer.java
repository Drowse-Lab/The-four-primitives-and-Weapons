
package minecraftarmorweapon.client.renderer;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import minecraftarmorweapon.entity.model.BlackholeModel;
import minecraftarmorweapon.entity.BlackholeEntity;

import javax.annotation.Nullable;

public class BlackholeRenderer extends GeoEntityRenderer<BlackholeEntity> {
	public BlackholeRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new BlackholeModel());
		this.shadowRadius = 0f;
	}

	@Override
	public RenderType getRenderType(BlackholeEntity animatable, ResourceLocation texture,
			@Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}
}
