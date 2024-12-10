
package minecraftarmorweapon.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.SlimeModel;

import minecraftarmorweapon.entity.AlchemyCraftBlockEntityEntity;

public class AlchemyCraftBlockEntityRenderer extends MobRenderer<AlchemyCraftBlockEntityEntity, SlimeModel<AlchemyCraftBlockEntityEntity>> {
	public AlchemyCraftBlockEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new SlimeModel(context.bakeLayer(ModelLayers.SLIME)), 0.1f);
	}

	@Override
	public ResourceLocation getTextureLocation(AlchemyCraftBlockEntityEntity entity) {
		return new ResourceLocation("minecraft_armor_weapon:textures/entities/toumei.png");
	}
}
