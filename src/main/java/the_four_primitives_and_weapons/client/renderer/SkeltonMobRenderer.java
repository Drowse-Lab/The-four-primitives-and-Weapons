
package the_four_primitives_and_weapons.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import the_four_primitives_and_weapons.entity.SkeltonMobEntity;

import the_four_primitives_and_weapons.client.model.Modelskeleton_Converted;

public class SkeltonMobRenderer extends MobRenderer<SkeltonMobEntity, Modelskeleton_Converted<SkeltonMobEntity>> {
	public SkeltonMobRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelskeleton_Converted(context.bakeLayer(Modelskeleton_Converted.LAYER_LOCATION)), 0f);
	}

	@Override
	public ResourceLocation getTextureLocation(SkeltonMobEntity entity) {
		return new ResourceLocation("the_four_primitives_and_weapons:textures/entities/skeleton.png");
	}
}
