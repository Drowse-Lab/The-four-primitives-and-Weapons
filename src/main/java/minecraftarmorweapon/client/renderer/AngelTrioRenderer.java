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

import minecraftarmorweapon.entity.AngelTrioEntity;

/**
 * 天使の三人組レンダラー。
 * 人格ごとに異なるスキンを返す。
 *
 *  0: 真面目（女・ボブ・黒髪に緑）   → angel_serious.png / Slim
 *  1: 馬鹿にする（女・ストレート・黒髪に白） → angel_mocker1.png / Slim
 *  2: 馬鹿にする（男・結んだ髪・黒髪に赤黒） → angel_mocker2.png / Wide
 */
public class AngelTrioRenderer extends HumanoidMobRenderer<AngelTrioEntity, PlayerModel<AngelTrioEntity>> {

    private final PlayerModel<AngelTrioEntity> normalModel;
    private final PlayerModel<AngelTrioEntity> slimModel;

    // 人格ごとのスキン
    private static final ResourceLocation SKIN_SERIOUS = new ResourceLocation("minecraft_armor_weapon", "textures/entity/angel_trio/angel_serious.png");
    private static final ResourceLocation SKIN_MOCKER1 = new ResourceLocation("minecraft_armor_weapon", "textures/entity/angel_trio/angel_mocker1.png");
    private static final ResourceLocation SKIN_MOCKER2 = new ResourceLocation("minecraft_armor_weapon", "textures/entity/angel_trio/angel_mocker2.png");
    // フォールバック
    private static final ResourceLocation SKIN_FALLBACK_SLIM = new ResourceLocation("textures/entity/player/slim/alex.png");
    private static final ResourceLocation SKIN_FALLBACK_WIDE = new ResourceLocation("textures/entity/player/wide/steve.png");

    public AngelTrioRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);

        this.normalModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        this.addLayer(new HumanoidArmorLayer<>(
            this,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()
        ));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new AILevelRenderLayer<>(this));
    }

    @Override
    public void render(AngelTrioEntity entity, float entityYaw, float partialTicks,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 全員Slim
        this.model = this.slimModel;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(AngelTrioEntity entity, PoseStack poseStack, float partialTicks) {
        poseStack.scale(0.95f, 1.0f, 0.95f); // 全員Slim体型
        super.scale(entity, poseStack, partialTicks);
    }

    @Override
    public ResourceLocation getTextureLocation(AngelTrioEntity entity) {
        return switch (entity.getPersonality()) {
            case AngelTrioEntity.PERSONALITY_SERIOUS  -> SKIN_SERIOUS;
            case AngelTrioEntity.PERSONALITY_MOCKER_1 -> SKIN_MOCKER1;
            case AngelTrioEntity.PERSONALITY_MOCKER_2 -> SKIN_MOCKER2;
            default -> SKIN_FALLBACK_SLIM;
        };
    }
}
