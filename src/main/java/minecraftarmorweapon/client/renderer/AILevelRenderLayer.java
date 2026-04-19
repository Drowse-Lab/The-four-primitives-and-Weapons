package minecraftarmorweapon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

import minecraftarmorweapon.ai.lisp.MobAILevelHandler;

/**
 * AIレベルをMobの頭上に表示するレンダーレイヤー。
 *
 * バニラのネームプレートと違い、depth testを有効にしたまま描画するため
 * ブロック越しには見えない。
 *
 * @param <T> エンティティタイプ
 * @param <M> モデルタイプ
 */
public class AILevelRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public AILevelRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        int aiLevel = MobAILevelHandler.getAILevel(entity);
        if (aiLevel <= 0) return;

        // 表示距離制限（32ブロック以内のみ）
        double distSq = entity.distanceToSqr(Minecraft.getInstance().cameraEntity);
        if (distSq > 1024) return; // 32^2

        String levelText = MobAILevelHandler.getLevelRank(aiLevel);
        String colorCode = MobAILevelHandler.getLevelColor(aiLevel);
        String displayText = colorCode + levelText;

        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();

        // エンティティの頭上に配置（ネームプレートより少し上）
        float entityHeight = entity.getBbHeight() + 0.6f;
        poseStack.translate(0.0, entityHeight, 0.0);

        // カメラに向かせる（ビルボード）
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.02f, -0.02f, 0.02f);

        Matrix4f matrix = poseStack.last().pose();
        int textWidth = font.width(displayText);
        float x = -textWidth / 2.0f;

        // === ブロック越しに見えない描画 ===
        // depth testが有効な通常バッファを使用（SEE_THROUGH ではない）

        // 背景（半透明の黒）— depth test有効
        font.drawInBatch(
            displayText,
            x + 1, 1,       // 影のオフセット
            0x44000000,      // 半透明の黒（影）
            false,           // shadow=false
            matrix,
            buffer,
            Font.DisplayMode.NORMAL,  // NORMAL = depth testあり（ブロック越しに見えない）
            0x40000000,      // 背景色（半透明黒）
            packedLight
        );

        // テキスト本体 — depth test有効
        font.drawInBatch(
            displayText,
            x, 0,
            0xFFFFFFFF,      // 白（色コードで上書きされる）
            false,
            matrix,
            buffer,
            Font.DisplayMode.NORMAL,  // NORMAL = depth testあり
            0,               // 背景なし
            packedLight
        );

        poseStack.popPose();
    }
}
