
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

	// @RotationParams(Blackhole, model=minecraft_armor_weapon:geo/blackhole.geo.json)
	public static float YAW_OFFSET = 0f; // Y軸回転
	public static float PITCH_OFFSET = 0f; // X軸回転
	public static float ROLL_OFFSET = 0f; // Z軸回転
	public static float SCALE = 1.0f; // 表示サイズ
	// @EndRotationParams

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
