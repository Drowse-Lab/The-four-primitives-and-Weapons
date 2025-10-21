package minecraftarmorweapon.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import minecraftarmorweapon.entity.CommonSoldierEntity;

/**
 * 一般兵エンティティのレンダラー
 *
 * プレイヤーモデル（Steve/Slim）を使用し、武器と防具を表示します
 */
public class CommonSoldierRenderer extends HumanoidMobRenderer<CommonSoldierEntity, PlayerModel<CommonSoldierEntity>> {

    private final PlayerModel<CommonSoldierEntity> normalModel;  // Steve（太い腕）
    private final PlayerModel<CommonSoldierEntity> slimModel;    // Alex（細い腕）

    public CommonSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);

        // 通常モデル（Steve - 太い腕）
        this.normalModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);

        // スリムモデル（Alex - 細い腕）
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        // 防具レイヤーを追加（鉄の防具を表示）
        this.addLayer(new HumanoidArmorLayer<>(
            this,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))
        ));

        // 武器レイヤーを追加（手に持っている刀を表示）
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(CommonSoldierEntity entity, float entityYaw, float partialTicks,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // UUID-based model selection - 各エンティティで一貫して同じモデルを使用
        if (shouldUseSlimModel(entity)) {
            this.model = this.slimModel;
        } else {
            this.model = this.normalModel;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * スリムモデル（Alex）を使用するかどうか
     * UUIDの最下位ビットで決定（50%の確率で各モデル）
     */
    private boolean shouldUseSlimModel(CommonSoldierEntity entity) {
        // UUIDの最下位ビットが奇数ならSlim、偶数ならSteve
        long leastSigBits = entity.getUUID().getLeastSignificantBits();
        return (leastSigBits & 1) == 1;
    }

    @Override
    public ResourceLocation getTextureLocation(CommonSoldierEntity entity) {
        // モデルに合わせてテクスチャを選択
        if (shouldUseSlimModel(entity)) {
            return new ResourceLocation("textures/entity/alex.png");
        }
        return new ResourceLocation("textures/entity/steve.png");
    }
}
