package the_four_primitives_and_weapons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import the_four_primitives_and_weapons.client.ClientSoulFireState;

/**
 * 3人称: エンティティに重なる炎オーバーレイ (renderFlame) のスプライトを、
 * 魂の炎対象なら「青くした炎(blue_fire)」に差し替える。 炎の形はバニラと同じ。
 *
 * <p>1.20.1 の renderFlame は (PoseStack, MultiBufferSource, Entity) の 3 引数。
 * (Quaternionf 引数が付くのは 1.20.2 以降なので、ここでは含めない。)</p>
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherFireMixin {

	@Redirect(
			method = "renderFlame",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/resources/model/Material;sprite()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
					ordinal = 0))
	private TextureAtlasSprite tfpw$blueFire0(Material material, PoseStack pose, MultiBufferSource buffer,
											  Entity entity) {
		if (ClientSoulFireState.isSoul(entity)) {
			return ClientSoulFireState.BLUE_FIRE_0.sprite();
		}
		return material.sprite();
	}

	@Redirect(
			method = "renderFlame",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/resources/model/Material;sprite()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
					ordinal = 1))
	private TextureAtlasSprite tfpw$blueFire1(Material material, PoseStack pose, MultiBufferSource buffer,
											  Entity entity) {
		if (ClientSoulFireState.isSoul(entity)) {
			return ClientSoulFireState.BLUE_FIRE_1.sprite();
		}
		return material.sprite();
	}
}
