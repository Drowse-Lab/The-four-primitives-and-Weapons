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

    // render() が一度でも呼ばれたか確認するための一回限りログフラグ。
    // ゲーム起動 → saya を curio スロットに装備 → ログ出力されれば
    // renderer は呼ばれている。出ないなら CuriosRendererRegistry 登録か
    // capability 側の問題。
    private static final java.util.concurrent.atomic.AtomicBoolean LOGGED_ONCE =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("MAW/ScabbardCurioRenderer");

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

        if (LOGGED_ONCE.compareAndSet(false, true)) {
            LOGGER.info("[MAW] ScabbardCurioRenderer.render() called: slot={} stack={} item={}",
                    slotContext.identifier(), stack, stack.getItem());
            try {
                LOGGER.info("[MAW]   SAYA_BACK = {}, SAYA_BELT = {}",
                        the_four_primitives_and_weapons.client.MawDisplayContexts.SAYA_BACK,
                        the_four_primitives_and_weapons.client.MawDisplayContexts.SAYA_BELT);
            } catch (Throwable t) {
                LOGGER.error("[MAW]   MawDisplayContexts access failed", t);
            }
        }

        poseStack.pushPose();

        // sb_worn_display Blockbench プラグインが各 slot で使う anchor 位置に
        // PoseStack を合わせる。プラグイン側で display_area.position.y を設定して
        // いる値と同じ Minecraft world Y に PoseStack 原点を移動する。
        //
        //   Blockbench scene Y → Minecraft world Y の換算:
        //     world_y = scene_y / 16 × 0.9375 (LivingEntityRenderer の player scale)
        //
        //   Blockbench Steve リファレンス内の主な高さ:
        //     y=28 → 顔の中心   (world 1.640)  ← loadHead デフォルト
        //     y=24 → 首・頭根元 (world 1.406)  ← body/head bone pivot
        //     y=18 → 胸・背中   (world 1.054)  ← :back の anchor
        //     y=12 → 腰・ベルト (world 0.703)  ← :belt の anchor
        //
        //   model.body.translateAndRotate で PoseStack は世界 y=1.406 (= scene 24)
        //   に位置する。そこから各 slot の anchor まで下ろす translate を入れる。
        //   PoseStack は Y軸反転フレームなので「+Δ in PoseStack」=「-Δ × 0.9375 in
        //   world」になる。slot ごとの translate Y:
        //     belt: 1.406 → 0.703 = -0.703 world = +0.703/0.9375 = +0.75 PoseStack
        //     back: 1.406 → 1.054 = -0.352 world = +0.352/0.9375 = +0.375 PoseStack
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            @SuppressWarnings("unchecked")
            HumanoidModel<LivingEntity> model = (HumanoidModel<LivingEntity>) humanoidModel;
            ICurioRenderer.followBodyRotations(slotContext.entity(), model);
            model.body.translateAndRotate(poseStack);
        }

        String slotId = slotContext.identifier();
        if ("belt".equals(slotId)) {
            poseStack.translate(0.0, 0.75, 0.0);
        } else if ("back".equals(slotId)) {
            poseStack.translate(0.0, 0.375, 0.0);
        }

        // Blockbench プラグイン (loadHead 経由) はリファレンスに da.scale = 0.625
        // を適用しているが、Minecraft は PlayerRenderer.scale で 0.9375 を適用する。
        // 同じ display 値で in-game は 0.9375/0.625 = 1.5 倍大きく見える。
        // ここで scale(2/3) を入れて Blockbench と一致させる
        // (1.5 × 2/3 = 1 → 同じ大きさで描画される)。
        final float SCALE_MATCH = 2.0F / 3.0F;
        poseStack.scale(SCALE_MATCH, SCALE_MATCH, SCALE_MATCH);

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
