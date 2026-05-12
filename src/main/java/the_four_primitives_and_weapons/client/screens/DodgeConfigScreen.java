package the_four_primitives_and_weapons.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import the_four_primitives_and_weapons.config.DodgeConfig;

/**
 * 回避設定画面 (vanilla Options 画面スタイル)
 * Controls画面のボタンから開く
 */
public class DodgeConfigScreen extends Screen {

    private final Screen parent;

    public DodgeConfigScreen(Screen parent) {
        super(Component.translatable("screen.the_four_primitives_and_weapons.dodge_config.title"));
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
        int btnW = 310;
        int btnH = 20;
        int spacing = 24;
        // バニラ Options スクリーン同様、タイトルから 40px 下に最初のボタンを配置
        int firstY = 40;

        // 設定1: ファンクショナルブロック保護
        addRenderableWidget(Button.builder(
                getFunctionalBlockText(),
                btn -> {
                    DodgeConfig.functionalBlockRequiresShift = !DodgeConfig.functionalBlockRequiresShift;
                    DodgeConfig.save();
                    btn.setMessage(getFunctionalBlockText());
                })
                .bounds(centerX - btnW / 2, firstY, btnW, btnH)
                .build());

        // 設定2: 不活性アイテムで回避
        addRenderableWidget(Button.builder(
                getInertItemText(),
                btn -> {
                    DodgeConfig.dodgeWithInertItems = !DodgeConfig.dodgeWithInertItems;
                    DodgeConfig.save();
                    btn.setMessage(getInertItemText());
                })
                .bounds(centerX - btnW / 2, firstY + spacing, btnW, btnH)
                .build());

        // 設定3: アクティブアイテムで回避
        addRenderableWidget(Button.builder(
                getActiveItemText(),
                btn -> {
                    DodgeConfig.dodgeWithActiveItems = !DodgeConfig.dodgeWithActiveItems;
                    DodgeConfig.save();
                    btn.setMessage(getActiveItemText());
                })
                .bounds(centerX - btnW / 2, firstY + spacing * 2, btnW, btnH)
                .build());

        // 設定4: 回避無効化
        addRenderableWidget(Button.builder(
                getDodgeDisabledText(),
                btn -> {
                    DodgeConfig.dodgeDisabled = !DodgeConfig.dodgeDisabled;
                    DodgeConfig.save();
                    btn.setMessage(getDodgeDisabledText());
                })
                .bounds(centerX - btnW / 2, firstY + spacing * 3, btnW, btnH)
                .build());

        // Done ボタン (バニラと同じ位置: 画面下から 27px 上)
        addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                btn -> this.onClose())
                .bounds(centerX - 100, this.height - 27, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        // バニラと同じくタイトルを画面上部 (y=15) に中央表示
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    private Component getFunctionalBlockText() {
        Component status = DodgeConfig.functionalBlockRequiresShift
                ? Component.literal("§aON")
                : Component.literal("§cOFF");
        return Component.translatable("screen.the_four_primitives_and_weapons.dodge_config.functional_block").append(": ").append(status);
    }

    private Component getInertItemText() {
        Component status = DodgeConfig.dodgeWithInertItems
                ? Component.literal("§aON")
                : Component.literal("§cOFF");
        return Component.translatable("screen.the_four_primitives_and_weapons.dodge_config.inert_item").append(": ").append(status);
    }

    private Component getActiveItemText() {
        Component status = DodgeConfig.dodgeWithActiveItems
                ? Component.literal("§aON")
                : Component.literal("§cOFF");
        return Component.translatable("screen.the_four_primitives_and_weapons.dodge_config.active_item").append(": ").append(status);
    }

    private Component getDodgeDisabledText() {
        Component status = DodgeConfig.dodgeDisabled
                ? Component.literal("§cON")
                : Component.literal("§aOFF");
        return Component.translatable("screen.the_four_primitives_and_weapons.dodge_config.disabled").append(": ").append(status);
    }
}
