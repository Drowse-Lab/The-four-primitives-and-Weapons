package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import the_four_primitives_and_weapons.entity.MiniMobEntity;
import the_four_primitives_and_weapons.entity.model.MiniMobModel;

/**
 * ミニmobの描画。
 * モデルは小さいアーマースタンド。テクスチャは 64x64 で、頭だけプレイヤースキンと同じUV。
 */
public class MiniMobRenderer extends HumanoidMobRenderer<MiniMobEntity, MiniMobModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("the_four_primitives_and_weapons", "textures/entity/mini_mob.png");
    /** 目を閉じた表情。素材の player_head スキンが2枚あるので、まばたきに使う。 */
    private static final ResourceLocation TEXTURE_BLINK =
            new ResourceLocation("the_four_primitives_and_weapons", "textures/entity/mini_mob_blink.png");

    public MiniMobRenderer(EntityRendererProvider.Context context) {
        super(context, new MiniMobModel(context.bakeLayer(MiniMobModel.LAYER_LOCATION)), 0.3f);
        // 鉄防具は MOD 内蔵テクスチャの専用レイヤーで描く ( リソースパック非依存 )。
        this.addLayer(new MiniMobArmorLayer(this, context.getModelSet()));
    }

    @Override
    protected void scale(MiniMobEntity entity, PoseStack poseStack, float partialTicks) {
        if (entity.isInSittingPose())
            poseStack.translate(0.0f, 0.15f, 0.0f);
        super.scale(entity, poseStack, partialTicks);
    }

    @Override
    public ResourceLocation getTextureLocation(MiniMobEntity entity) {
        return entity.isBlinking() ? TEXTURE_BLINK : TEXTURE;
    }
}
