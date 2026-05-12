package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * 特殊個体ラベル（特性/イベント指定）を Mob の頭上に描画する。
 *
 * バニラの CustomName と違い Font.DisplayMode.NORMAL を使うので
 * ブロック越しには見えない (depth test 有効)。
 *
 * NBT キー:
 *   - MMWTraitLabel: 特性ラベル（例: "§9[鉄壁]"）
 *   - MMWSpecialLabel: イベントラベル（例: "§5[アンデットアーミー Wave3]"）
 */
public class SpecialLabelRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public static final String NBT_TRAIT_LABEL = "MMWTraitLabel";
    public static final String NBT_SPECIAL_LABEL = "MMWSpecialLabel";
    private static final double MAX_RENDER_DIST_SQ = 1024.0; // 32^2

    public SpecialLabelRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        CompoundTag data = entity.getPersistentData();
        String special = data.contains(NBT_SPECIAL_LABEL) ? data.getString(NBT_SPECIAL_LABEL) : "";
        String trait = data.contains(NBT_TRAIT_LABEL) ? data.getString(NBT_TRAIT_LABEL) : "";
        if (special.isEmpty() && trait.isEmpty()) return;

        if (Minecraft.getInstance().cameraEntity == null) return;
        double distSq = entity.distanceToSqr(Minecraft.getInstance().cameraEntity);
        if (distSq > MAX_RENDER_DIST_SQ) return;

        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        // ネームプレート位置よりさらに上に配置（AILevelRenderLayer より高く）
        float entityHeight = entity.getBbHeight() + 0.9f;
        poseStack.translate(0.0, entityHeight, 0.0);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.02f, -0.02f, 0.02f);

        Matrix4f matrix = poseStack.last().pose();

        // 行をまとめる（上から special, trait の順で積む）
        int lineY = 0;
        int lineHeight = 10;
        if (!special.isEmpty()) {
            drawLine(font, matrix, buffer, packedLight, special, 0, lineY);
            lineY += lineHeight;
        }
        if (!trait.isEmpty()) {
            drawLine(font, matrix, buffer, packedLight, trait, 0, lineY);
        }

        poseStack.popPose();
    }

    private static void drawLine(Font font, Matrix4f matrix, MultiBufferSource buffer,
                                 int packedLight, String text, int baseX, int baseY) {
        int textWidth = font.width(text);
        float x = baseX - textWidth / 2.0f;

        // 背景（半透明黒）
        font.drawInBatch(
            text,
            x + 1, baseY + 1,
            0x44000000,
            false,
            matrix,
            buffer,
            Font.DisplayMode.NORMAL, // depth testあり → ブロック越しに見えない
            0x40000000,
            packedLight
        );
        // テキスト本体
        font.drawInBatch(
            text,
            x, baseY,
            0xFFFFFFFF,
            false,
            matrix,
            buffer,
            Font.DisplayMode.NORMAL,
            0,
            packedLight
        );
    }
}
