package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Curios elytra スロットに装備したエリトラを背中に描画する (ElytraSlot mod 相当機能)。
 *
 * <p>バニラ {@code ElytraLayer} はチェストスロットのみ見るため、Curios スロット用に
 * 同じ描画ロジック ( ElytraModel + ケープ/カスタムエリトラテクスチャ対応 ) を
 * {@link ICurioRenderer} として再実装している。</p>
 */
public class ElytraCurioRenderer implements ICurioRenderer {

    private static final ResourceLocation WINGS_LOCATION =
            new ResourceLocation("textures/entity/elytra.png");

    /** FMLClientSetup 時点では EntityModelSet が未完成の場合があるので遅延 bake する。 */
    private ElytraModel<LivingEntity> elytraModel;

    private ElytraModel<LivingEntity> getModel() {
        if (elytraModel == null) {
            elytraModel = new ElytraModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELYTRA));
        }
        return elytraModel;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource bufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        LivingEntity entity = slotContext.entity();
        ElytraModel<LivingEntity> model = getModel();

        // バニラ ElytraLayer と同じテクスチャ選択:
        //   カスタムエリトラ > ケープ ( 表示 ON 時 ) > デフォルト羽
        ResourceLocation texture = WINGS_LOCATION;
        if (entity instanceof AbstractClientPlayer player) {
            if (player.isElytraLoaded() && player.getElytraTextureLocation() != null) {
                texture = player.getElytraTextureLocation();
            } else if (player.isCapeLoaded() && player.getCloakTextureLocation() != null
                    && player.isModelPartShown(PlayerModelPart.CAPE)) {
                texture = player.getCloakTextureLocation();
            }
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        ((EntityModel) renderLayerParent.getModel()).copyPropertiesTo(model);
        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                bufferSource, RenderType.armorCutoutNoCull(texture), false, stack.hasFoil());
        model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
