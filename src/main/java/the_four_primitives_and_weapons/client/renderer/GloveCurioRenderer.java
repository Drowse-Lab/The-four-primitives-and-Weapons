package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import the_four_primitives_and_weapons.client.model.MeijiUniformArmorModels;

/**
 * Curios hands スロットに装備した手袋 ( GloveItem ) を両手に描画する。
 *
 * <p>ジオメトリは制服から分離した {@code ModelMeijiUniform} の
 * right_glove / left_glove パーツ ( slim / wide 対応 ) を使用し、
 * {@link DyeableLeatherItem#getColor} の色 ( 未染色 = 白 ) を乗算 tint する。</p>
 */
public class GloveCurioRenderer implements ICurioRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "the_four_primitives_and_weapons", "textures/entities/gloves.png");

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

        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> parentModel)) return;
        LivingEntity entity = slotContext.entity();

        HumanoidModel model = MeijiUniformArmorModels.gloves(entity, (HumanoidModel) parentModel);
        // 腕の振り・スニーク等のポーズを本体モデルからコピー
        ICurioRenderer.followBodyRotations(entity, model);

        int rgb;
        if (stack.getItem() instanceof DyeableLeatherItem dye) {
            rgb = dye.getColor(stack); // GloveItem 系: 染色色 ( 未染色は defaultColor )
        } else if (stack.getItem() instanceof the_four_primitives_and_weapons.item.IronGauntletsItem) {
            rgb = the_four_primitives_and_weapons.item.IronGauntletsItem.TINT; // 鉄色 ( 染色不可 )
        } else {
            rgb = 0xFFFFFF;
        }
        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;

        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                bufferSource, RenderType.armorCutoutNoCull(TEXTURE), false, stack.hasFoil());
        model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
    }
}
