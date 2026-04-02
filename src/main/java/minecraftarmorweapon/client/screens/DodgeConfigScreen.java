package minecraftarmorweapon.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import minecraftarmorweapon.config.DodgeConfig;

/**
 * 回避設定画面
 * Controls画面のボタンから開く
 */
public class DodgeConfigScreen extends Screen {

    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int HEADER_COLOR = 0xFF2D6A4F;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DESC_COLOR = 0xFFAAAAAA;

    private final Screen parent;

    public DodgeConfigScreen(Screen parent) {
        super(Component.literal("回避設定"));
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
        int startY = this.height / 2 - 85;
        int btnW = 240;

        // 設定1: ファンクショナルブロック保護
        addRenderableWidget(Button.builder(
                getFunctionalBlockText(),
                btn -> {
                    DodgeConfig.functionalBlockRequiresShift = !DodgeConfig.functionalBlockRequiresShift;
                    DodgeConfig.save();
                    btn.setMessage(getFunctionalBlockText());
                })
                .bounds(centerX - btnW / 2, startY + 30, btnW, 20)
                .build());

        // 設定2: 不活性アイテムで回避
        addRenderableWidget(Button.builder(
                getInertItemText(),
                btn -> {
                    DodgeConfig.dodgeWithInertItems = !DodgeConfig.dodgeWithInertItems;
                    DodgeConfig.save();
                    btn.setMessage(getInertItemText());
                })
                .bounds(centerX - btnW / 2, startY + 80, btnW, 20)
                .build());

        // 設定3: アクティブアイテムで回避
        addRenderableWidget(Button.builder(
                getActiveItemText(),
                btn -> {
                    DodgeConfig.dodgeWithActiveItems = !DodgeConfig.dodgeWithActiveItems;
                    DodgeConfig.save();
                    btn.setMessage(getActiveItemText());
                })
                .bounds(centerX - btnW / 2, startY + 130, btnW, 20)
                .build());

        // 設定4: 回避無効化
        addRenderableWidget(Button.builder(
                getDodgeDisabledText(),
                btn -> {
                    DodgeConfig.dodgeDisabled = !DodgeConfig.dodgeDisabled;
                    DodgeConfig.save();
                    btn.setMessage(getDodgeDisabledText());
                })
                .bounds(centerX - btnW / 2, startY + 180, btnW, 20)
                .build());

        // 戻るボタン
        addRenderableWidget(Button.builder(
                Component.literal("戻る"),
                btn -> this.onClose())
                .bounds(centerX - 50, startY + 230, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 85;
        int panelW = 300;
        int panelH = 270;
        int panelX = centerX - panelW / 2;

        // パネル背景
        graphics.fill(panelX, startY, panelX + panelW, startY + panelH, BG_COLOR);
        // ヘッダー
        graphics.fill(panelX, startY, panelX + panelW, startY + 22, HEADER_COLOR);
        graphics.drawCenteredString(this.font, "§l回避設定", centerX, startY + 7, TEXT_COLOR);

        // 設定1ラベル
        graphics.drawCenteredString(this.font, "§eファンクショナルブロック保護", centerX, startY + 26, TEXT_COLOR);
        graphics.drawCenteredString(this.font, "§7ON: チェスト等はShift+右クリックで回避", centerX, startY + 52, DESC_COLOR);

        // 設定2ラベル
        graphics.drawCenteredString(this.font, "§e不活性アイテムで回避", centerX, startY + 76, TEXT_COLOR);
        graphics.drawCenteredString(this.font, "§7ON: 骨等を持っていてもオフハンド武器で回避可能", centerX, startY + 102, DESC_COLOR);

        // 設定3ラベル
        graphics.drawCenteredString(this.font, "§eアクティブアイテムで回避", centerX, startY + 126, TEXT_COLOR);
        graphics.drawCenteredString(this.font, "§7ON: 弓・ポーション等を持っていても回避可能", centerX, startY + 152, DESC_COLOR);

        // 設定4ラベル
        graphics.drawCenteredString(this.font, "§e回避無効化", centerX, startY + 176, TEXT_COLOR);
        graphics.drawCenteredString(this.font, "§7ON: 右クリックでアイテム本来の動作を使用", centerX, startY + 202, DESC_COLOR);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component getFunctionalBlockText() {
        if (DodgeConfig.functionalBlockRequiresShift) {
            return Component.literal("§aON §f- Shift必要");
        } else {
            return Component.literal("§cOFF §f- 常に回避優先");
        }
    }

    private Component getInertItemText() {
        if (DodgeConfig.dodgeWithInertItems) {
            return Component.literal("§aON §f- 骨等でも回避可能");
        } else {
            return Component.literal("§cOFF §f- 武器のみ回避");
        }
    }

    private Component getActiveItemText() {
        if (DodgeConfig.dodgeWithActiveItems) {
            return Component.literal("§aON §f- 弓等でも回避可能");
        } else {
            return Component.literal("§cOFF §f- アイテム動作優先");
        }
    }

    private Component getDodgeDisabledText() {
        if (DodgeConfig.dodgeDisabled) {
            return Component.literal("§cON §f- 回避無効（特殊技優先）");
        } else {
            return Component.literal("§aOFF §f- 回避有効");
        }
    }
}
