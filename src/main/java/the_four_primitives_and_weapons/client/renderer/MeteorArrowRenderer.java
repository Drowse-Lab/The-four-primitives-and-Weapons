
package the_four_primitives_and_weapons.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;

import the_four_primitives_and_weapons.entity.MeteorArrowEntity;

public class MeteorArrowRenderer extends HumanoidMobRenderer<MeteorArrowEntity, HumanoidModel<MeteorArrowEntity>> {
	public MeteorArrowRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(MeteorArrowEntity entity) {
		return new ResourceLocation("the_four_primitives_and_weapons:textures/entities/toumei.png");
	}
}
