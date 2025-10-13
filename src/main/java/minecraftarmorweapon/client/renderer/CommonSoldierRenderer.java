package minecraftarmorweapon.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.PlayerModel;

import minecraftarmorweapon.entity.CommonSoldierEntity;

/**
 * 一般兵エンティティのレンダラー
 *
 * プレイヤーモデルを使用してレンダリングします
 */
public class CommonSoldierRenderer extends MobRenderer<CommonSoldierEntity, PlayerModel<CommonSoldierEntity>> {

    public CommonSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CommonSoldierEntity entity) {
        // プレイヤースキンのデフォルトテクスチャ（スティーブ）
        return new ResourceLocation("textures/entity/steve.png");
    }
}
