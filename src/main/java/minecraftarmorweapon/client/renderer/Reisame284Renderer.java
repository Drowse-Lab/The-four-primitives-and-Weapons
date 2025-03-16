
package minecraftarmorweapon.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import minecraftarmorweapon.entity.Reisame284Entity;

import minecraftarmorweapon.client.model.Modelplayer_slim__Converted;

public class Reisame284Renderer extends MobRenderer<Reisame284Entity, Modelplayer_slim__Converted<Reisame284Entity>> {
	public Reisame284Renderer(EntityRendererProvider.Context context) {
		super(context, new Modelplayer_slim__Converted(context.bakeLayer(Modelplayer_slim__Converted.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(Reisame284Entity entity) {
		return new ResourceLocation("minecraft_armor_weapon:textures/entities/43640277d3e29bd7.png");
	}
}
