package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class ScabbardCurioRenderer implements ICurioRenderer {

    // =============================================
    // デバッグモード: ScabbardDebugScreen (F8) でリアルタイム調整する時に使う
    // 通常時は false にしておく（ハードコード値が使われる）
    // =============================================
    // public static boolean DEBUG_MODE = true;
    public static boolean DEBUG_MODE = false;

    // --- デバッグ用 static変数（DEBUG_MODE=true の時のみ使用） ---
    // --- ベルト ---
    public static double beltX = 0.280, beltY = 0.660, beltZ = -0.240;
    public static float beltRotX = -100f, beltRotY = 180f, beltRotZ = 0f;
    public static float beltScaleX = 1.15f, beltScaleY = 1.3f, beltScaleZ = 1.15f;
    // --- 背中 ---
    public static double backX = -0.330, backY = 0.040, backZ = 0.130;
    public static float backRotX = 0f, backRotY = 270f, backRotZ = 140f;
    public static float backScaleX = 1.15f, backScaleY = 1.3f, backScaleZ = 1.15f;

    @Override
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

        poseStack.pushPose();

        // sb_worn_display Blockbench プラグインは DisplayMode.loadHead() を踏み台に
        // 「head の visual center (顔の高さ)」を anchor として display 値を解釈する。
        // HumanoidModel の head bone pivot は head box の下端 (= 首) にあるため、
        // visual center に合わせるには pivot から head box 内側に半分 (4 unit / 16
        // = 0.25) 上げる必要がある。
        // 1) model.body.translateAndRotate で body yaw に追従させる
        //    (head ではなく body を使うのは、プレイヤーが見回したとき鞘が
        //     回らないようにするため)
        // 2) translate(0, -0.25, 0) で PoseStack 原点を head visual center まで持ち上げる
        //    (Y軸反転フレーム内なので「-0.25」が world Y で「+0.234」)
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            @SuppressWarnings("unchecked")
            HumanoidModel<LivingEntity> model = (HumanoidModel<LivingEntity>) humanoidModel;
            ICurioRenderer.followBodyRotations(slotContext.entity(), model);
            model.body.translateAndRotate(poseStack);
            poseStack.translate(0.0, -0.25, 0.0);
        }

        String slotId = slotContext.identifier();

        // slot に応じてカスタム DisplayContext を選択。
        ItemDisplayContext displayCtx;
        if ("back".equals(slotId)) {
            displayCtx = the_four_primitives_and_weapons.client.MawDisplayContexts.SAYA_BACK;
        } else if ("belt".equals(slotId)) {
            displayCtx = the_four_primitives_and_weapons.client.MawDisplayContexts.SAYA_BELT;
        } else {
            displayCtx = ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                displayCtx,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                slotContext.entity().level(),
                slotContext.entity().getId()
        );

        poseStack.popPose();
    }
}
