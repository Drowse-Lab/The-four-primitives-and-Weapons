package minecraftarmorweapon.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import minecraftarmorweapon.config.DebugConfig;

/**
 * デバッグ設定画面
 * Controls画面のボタンから開く
 */
public class DebugConfigScreen extends Screen {

    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int HEADER_COLOR = 0xFF2D6A4F;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DESC_COLOR = 0xFFAAAAAA;

    private final Screen parent;

    public DebugConfigScreen(Screen parent) {
        super(Component.literal("デバッグ設定"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 60;
        int btnW = 240;

        // 設定1: 属性デバッグ表示
        addRenderableWidget(Button.builder(
                getElementalDebugText(),
                btn -> {
                    DebugConfig.elementalDebugEnabled = !DebugConfig.elementalDebugEnabled;
                    DebugConfig.save();
                    btn.setMessage(getElementalDebugText());
                })
                .bounds(centerX - btnW / 2, startY + 30, btnW, 20)
                .build());

        // 戻るボタン
        addRenderableWidget(Button.builder(
                Component.literal("戻る"),
                btn -> this.onClose())
                .bounds(centerX - 50, startY + 90, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 60;
        int panelW = 300;
        int panelH = 140;
        int panelX = centerX - panelW / 2;

        // パネル背景
        graphics.fill(panelX, startY, panelX + panelW, startY + panelH, BG_COLOR);
        // ヘッダー
        graphics.fill(panelX, startY, panelX + panelW, startY + 22, HEADER_COLOR);
        graphics.drawCenteredString(this.font, "§lデバッグ設定", centerX, startY + 7, TEXT_COLOR);

        // 設定1ラベル
        graphics.drawCenteredString(this.font, "§e属性デバッグ表示", centerX, startY + 26, TEXT_COLOR);
        graphics.drawCenteredString(this.font, "§7ON: 攻撃時にアクションバーへ属性情報を表示", centerX, startY + 52, DESC_COLOR);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component getElementalDebugText() {
        if (DebugConfig.elementalDebugEnabled) {
            return Component.literal("§aON §f- デバッグ表示");
        } else {
            return Component.literal("§cOFF §f- 非表示（デフォルト）");
        }
    }
}
