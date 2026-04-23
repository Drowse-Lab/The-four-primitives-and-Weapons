package minecraftarmorweapon.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;

import minecraftarmorweapon.world.inventory.KnifeHolderMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * ナイフホルダーの内蔵在庫 GUI。
 * 上段 2×8 = 16 スロット (各最大 64 本) + プレイヤーインベントリ。
 *
 * 背景テクスチャは用意せず、プログラムで描画 (ソリッドな色＋枠＋スロット描画)。
 */
public class KnifeHolderScreen extends AbstractContainerScreen<KnifeHolderMenu> {

    public KnifeHolderScreen(KnifeHolderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // コンテナサイズ
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // タイトル位置を中央寄せ
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    // --- vanilla inventory.png と同じ配色 ---
    private static final int BG_FILL       = 0xFFC6C6C6; // 明るいベージュ (メイン背景)
    private static final int HL_TOP_LEFT   = 0xFFFFFFFF; // 左上ハイライト
    private static final int SH_BOTTOM_RIGHT = 0xFF555555; // 右下シャドウ
    private static final int SLOT_SHADOW   = 0xFF373737; // スロット陰影
    private static final int SLOT_FILL     = 0xFF8B8B8B; // スロット内部 (背景より暗め)

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // パネル本体 (vanilla のベージュ)
        g.fill(x, y, x + w, y + h, BG_FILL);
        // 立体枠: 上・左に明るいライン、下・右に暗いライン
        g.fill(x, y, x + w, y + 1, HL_TOP_LEFT);                    // top
        g.fill(x, y, x + 1, y + h, HL_TOP_LEFT);                    // left
        g.fill(x, y + h - 1, x + w, y + h, SH_BOTTOM_RIGHT);        // bottom
        g.fill(x + w - 1, y, x + w, y + h, SH_BOTTOM_RIGHT);        // right

        // ホルダースロット 8×2
        for (int row = 0; row < KnifeHolderMenu.HOLDER_ROWS; row++) {
            for (int col = 0; col < KnifeHolderMenu.HOLDER_COLS; col++) {
                drawSlot(g, x + 8 + col * 18, y + 18 + row * 18);
            }
        }
        // プレイヤーインベントリ 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, x + 8 + col * 18, y + 68 + row * 18);
            }
        }
        // ホットバー (下段 9)
        for (int col = 0; col < 9; col++) {
            drawSlot(g, x + 8 + col * 18, y + 126);
        }

        RenderSystem.disableBlend();
    }

    /** vanilla と同じ "彫り込み" スロット (16×16 + 1px 外枠で立体感) */
    private static void drawSlot(GuiGraphics g, int sx, int sy) {
        // 外枠 (暗い側: 左・上) / (明るい側: 右・下) = 彫り込み表現
        g.fill(sx - 1, sy - 1, sx + 17, sy,      SLOT_SHADOW); // top shadow
        g.fill(sx - 1, sy - 1, sx,      sy + 17, SLOT_SHADOW); // left shadow
        g.fill(sx - 1, sy + 17, sx + 17, sy + 18, HL_TOP_LEFT); // bottom highlight
        g.fill(sx + 17, sy - 1, sx + 18, sy + 18, HL_TOP_LEFT); // right highlight
        // スロット内部
        g.fill(sx, sy, sx + 16, sy + 16, SLOT_FILL);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
