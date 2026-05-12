package the_four_primitives_and_weapons.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import the_four_primitives_and_weapons.entity.BlackholeEntity;

public class BlackholeModel extends GeoModel<BlackholeEntity> {
	@Override
	public ResourceLocation getAnimationResource(BlackholeEntity entity) {
		return new ResourceLocation("the_four_primitives_and_weapons", "animations/blackhole.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BlackholeEntity entity) {
		return new ResourceLocation("the_four_primitives_and_weapons", "geo/blackhole.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BlackholeEntity entity) {
		return new ResourceLocation("the_four_primitives_and_weapons", "textures/entities/" + entity.getTexture() + ".png");
	}

}
