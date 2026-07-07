package the_four_primitives_and_weapons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import the_four_primitives_and_weapons.client.ClientSoulFireState;

/**
 * 1人称: 自分が燃えているときの画面炎オーバーレイ (renderFire) のスプライトを、
 * 魂の炎対象なら「青くした炎(blue_fire_1)」に差し替える。
 * blue_fire は同じ block アトラス上にあるためテクスチャバインドはそのまま有効。
 */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectFireMixin {

	@Redirect(
			method = "renderFire",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/resources/model/Material;sprite()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
					ordinal = 0))
	private static TextureAtlasSprite tfpw$blueFire(Material material, Minecraft minecraft, PoseStack pose) {
		if (minecraft.player != null && ClientSoulFireState.isSoul(minecraft.player)) {
			return ClientSoulFireState.BLUE_FIRE_1.sprite();
		}
		return material.sprite();
	}
}
