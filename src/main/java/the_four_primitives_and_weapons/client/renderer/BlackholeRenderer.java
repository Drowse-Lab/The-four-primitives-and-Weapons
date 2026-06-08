package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import the_four_primitives_and_weapons.entity.BlackholeEntity;
import the_four_primitives_and_weapons.entity.model.BlackholeModel;

/**
 * Blackhole エンティティのレンダラー (vanilla MobRenderer 版).
 * GeckoLib の GeoEntityRenderer は使わない。
 *
 * - LayerDefinition は {@link BlackholeModel#LAYER_LOCATION} で
 *   {@code TheFourPrimitivesAndWeaponsModEntityRenderers#registerLayerDefinitions}
 *   で event 経由でベイクされる。
 * - 描画は半透明 (translucent) — 元の RenderType.entityTranslucent と同じ。
 */
public class BlackholeRenderer extends MobRenderer<BlackholeEntity, BlackholeModel> {

    public BlackholeRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackholeModel(context.bakeLayer(BlackholeModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackholeEntity entity) {
        return new ResourceLocation("the_four_primitives_and_weapons",
            "textures/entities/" + entity.getTexture() + ".png");
    }

    @Override
    protected void scale(BlackholeEntity entity, PoseStack poseStack, float partialTickTime) {
        // 元の GeoEntityRenderer は scale なし (entity.getDimensions().scale(1) のまま) なので
        // ここでも scale を変更しない。
    }

    @Override
    protected RenderType getRenderType(BlackholeEntity entity, boolean visible,
                                       boolean visibleToPlayer, boolean glowing) {
        ResourceLocation texture = this.getTextureLocation(entity);
        // 元 GeckoLib renderer: RenderType.entityTranslucent(texture)
        return RenderType.entityTranslucent(texture);
    }
}
