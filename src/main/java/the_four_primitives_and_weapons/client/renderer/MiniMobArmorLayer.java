package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import the_four_primitives_and_weapons.entity.MiniMobEntity;
import the_four_primitives_and_weapons.entity.model.MiniMobModel;

/**
 * ミニmobが着ている鉄防具のレイヤー。
 *
 * 元mobが鉄防具を着たアーマースタンドだったので、体そのものは素のまま描き、
 * 防具はこのレイヤーで重ねる。テクスチャは MOD 内蔵の固定パスで、バニラの
 * textures/models/armor/iron_layer_*.png を一切参照しない。
 * つまりリソースパックで鉄防具の見た目が差し替えられていても影響を受けない。
 *
 * 装備スロットの中身も見ない。飼い主が何を持たせても防具の見た目は変わらない。
 */
public class MiniMobArmorLayer extends RenderLayer<MiniMobEntity, MiniMobModel> {

    private static final ResourceLocation ARMOR_TEXTURE =
            new ResourceLocation("the_four_primitives_and_weapons", "textures/entity/mini_mob_armor.png");

    /** 胴と肩当て用。OUTER_ARMOR = CubeDeformation(1.0) で体より一回り大きい。 */
    private final HumanoidModel<MiniMobEntity> outerModel;
    /** 脚用。INNER_ARMOR = CubeDeformation(0.5)。太いと左右の脚がくっついて見える。 */
    private final HumanoidModel<MiniMobEntity> innerModel;

    public MiniMobArmorLayer(RenderLayerParent<MiniMobEntity, MiniMobModel> parent, EntityModelSet models) {
        super(parent);
        this.outerModel = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        this.innerModel = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, MiniMobEntity entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (entity.isInvisible())
            return;

        VertexConsumer consumer = buffer.getBuffer(RenderType.armorCutoutNoCull(ARMOR_TEXTURE));
        // 胴と肩当て
        renderPart(this.outerModel, poseStack, consumer, packedLight, true, false);
        // 脚 ( レギンスとブーツ )
        renderPart(this.innerModel, poseStack, consumer, packedLight, false, true);
    }

    /** 本体モデルの pivot・回転・ちび用スケールを引き継いで、指定の部位だけ描く。 */
    private void renderPart(HumanoidModel<MiniMobEntity> model, PoseStack poseStack, VertexConsumer consumer,
            int packedLight, boolean upper, boolean lower) {
        this.getParentModel().copyPropertiesTo(model);
        // 頭は player_head 由来のスキンを見せたいので兜は描かない。
        model.head.visible = false;
        model.hat.visible = false;
        model.body.visible = upper;
        model.rightArm.visible = upper;
        model.leftArm.visible = upper;
        model.rightLeg.visible = lower;
        model.leftLeg.visible = lower;
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 1.0f);
    }
}
