package the_four_primitives_and_weapons.client.screens;

import the_four_primitives_and_weapons.item.ParryShieldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * シールドバッシュのチャージ量をクロスヘア直下に表示するオーバーレイ。
 *
 * 表示条件: メインハンドに ParryShieldItem を持ち、右クリック長押し中
 *
 * バーのデザイン:
 *   ┌──────────────────────────────┐  ← 外枠（黒）
 *   │ ████████████████░░░░░░░░░░░ │  ← 塗り(チャージ色) / 空き(暗灰)
 *   └──────────────────────────────┘
 *              ↑
 *      幅60px, 高さ4px, クロスヘアの12px下に描画
 *
 * チャージ色グラデーション:
 *   0%   → アンバー  (#FF8C00)
 *   50%  → イエロー  (#FFFF00)
 *   100% → ゴールドホワイト (#FFFFBB)、さらに白いパルスを重ねる
 *
 * 最低チャージ閾値マーカー（白い縦1px線）を MIN_CHARGE/MAX_CHARGE の位置に表示。
 * この線より左は「不発ゾーン」。
 */
@Mod.EventBusSubscriber({Dist.CLIENT})
public class ShieldChargeHudRenderer {

    private static final int BAR_WIDTH  = 60;
    private static final int BAR_HEIGHT = 4;
    /** クロスヘア中心から何px下に描画するか */
    private static final int OFFSET_Y   = 12;

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (mc.options.hideGui) return;

        // メインハンドに ParryShieldItem を持ち、右クリック長押し中か
        if (!(player.getMainHandItem().getItem() instanceof ParryShieldItem)) return;
        if (!player.isUsingItem()) return;
        if (player.getUsedItemHand() != InteractionHand.MAIN_HAND) return;

        // チャージ量を計算
        int remaining = player.getUseItemRemainingTicks();
        int held      = ParryShieldItem.USE_DURATION - remaining;

        // MIN_CHARGE_TICKS 未満でも表示して「もう少し」と伝える
        float chargeRaw = (float) held / ParryShieldItem.MAX_CHARGE_TICKS;
        float charge    = Math.min(chargeRaw, 1.0f);

        // 描画位置（クロスヘア中心を基準）
        int sw   = event.getWindow().getGuiScaledWidth();
        int sh   = event.getWindow().getGuiScaledHeight();
        int barX = sw / 2 - BAR_WIDTH / 2;
        int barY = sh / 2 + OFFSET_Y;

        var gui = event.getGuiGraphics();

        // ── 外枠（1px 黒）──
        gui.fill(barX - 1, barY - 1,
                 barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1,
                 0xCC000000);

        // ── 背景（空きゾーン）──
        gui.fill(barX, barY,
                 barX + BAR_WIDTH, barY + BAR_HEIGHT,
                 0xFF333333);

        // ── チャージ塗り ──
        int filledPx = (int)(BAR_WIDTH * charge);
        if (filledPx > 0) {
            int fillColor = chargeColor(charge);
            gui.fill(barX, barY,
                     barX + filledPx, barY + BAR_HEIGHT,
                     fillColor);

            // フルチャージ時: 白パルス（時間ベース）
            if (charge >= 1.0f) {
                float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.012) * 0.5 + 0.5);
                int alpha   = (int)(pulse * 0xBB);
                int white   = (alpha << 24) | 0xFFFFFF;
                gui.fill(barX, barY,
                         barX + BAR_WIDTH, barY + BAR_HEIGHT,
                         white);
            }
        }

        // ── 最低チャージ閾値マーカー（白い縦線）──
        // この線より左は離しても不発
        float thresholdRatio = (float) ParryShieldItem.MIN_CHARGE_TICKS
                             / ParryShieldItem.MAX_CHARGE_TICKS;
        int threshX = barX + (int)(BAR_WIDTH * thresholdRatio);
        gui.fill(threshX, barY - 1,
                 threshX + 1, barY + BAR_HEIGHT + 1,
                 0xAAFFFFFF);
    }

    /**
     * チャージ量 0.0〜1.0 に対応する ARGB カラーを返す。
     * アンバー(0%) → イエロー(50%) → ゴールドホワイト(100%)
     */
    private static int chargeColor(float t) {
        int r = 0xFF;
        int g, b;
        if (t < 0.5f) {
            // アンバー(140,60,0) → イエロー(255,255,0)
            float u = t * 2.0f;
            g = (int)(60  + u * 195); // 60  → 255
            b = 0;
        } else {
            // イエロー(255,255,0) → ゴールドホワイト(255,255,187)
            float u = (t - 0.5f) * 2.0f;
            g = 0xFF;
            b = (int)(u * 187);       // 0 → 187
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
