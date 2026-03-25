package minecraftarmorweapon.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.PlayerModel;

import minecraftarmorweapon.entity.DebugMobEntity;

/**
 * デバッグ用Mobのレンダラー
 * Steveスキンを使用
 */
public class DebugMobRenderer extends HumanoidMobRenderer<DebugMobEntity, PlayerModel<DebugMobEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/player/wide/steve.png");

    public DebugMobRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(DebugMobEntity entity) {
        return TEXTURE;
    }
}
