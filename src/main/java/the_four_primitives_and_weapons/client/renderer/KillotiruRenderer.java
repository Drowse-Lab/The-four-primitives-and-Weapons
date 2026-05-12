
package the_four_primitives_and_weapons.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import the_four_primitives_and_weapons.entity.KillotiruEntity;

import the_four_primitives_and_weapons.client.model.Modelluna_Converted;

public class KillotiruRenderer extends MobRenderer<KillotiruEntity, Modelluna_Converted<KillotiruEntity>> {
	public KillotiruRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelluna_Converted(context.bakeLayer(Modelluna_Converted.LAYER_LOCATION)), 0f);
	}

	@Override
	public ResourceLocation getTextureLocation(KillotiruEntity entity) {
		return new ResourceLocation("the_four_primitives_and_weapons:textures/entities/nonono.png");
	}
}
