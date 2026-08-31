package the_four_primitives_and_weapons.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

import the_four_primitives_and_weapons.entity.TargetDummyEntity;

/**
 * ターゲットダミーのレンダラー。
 *
 * <p>防具を着せて「魔法属性ダメージが防具を貫通するか」を見るので、
 * 防具レイヤーを付けて装備が見えるようにしている。</p>
 */
public class TargetDummyRenderer
        extends HumanoidMobRenderer<TargetDummyEntity, PlayerModel<TargetDummyEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/player/wide/steve.png");

    public TargetDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new DummyStatsRenderLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(TargetDummyEntity entity) {
        return TEXTURE;
    }
}
