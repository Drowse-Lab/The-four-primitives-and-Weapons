package minecraftarmorweapon.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import minecraftarmorweapon.config.DebugConfig;

/**
 * デバッグ設定画面 (vanilla Options 画面スタイル)
 * Controls画面のボタンから開く
 */
public class DebugConfigScreen extends Screen {

    private final Screen parent;

    public DebugConfigScreen(Screen parent) {
        super(Component.translatable("screen.minecraft_armor_weapon.debug_config.title"));
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
        int firstY = 40;

        // 設定1: 属性デバッグ表示
        addRenderableWidget(Button.builder(
                getElementalDebugText(),
                btn -> {
                    DebugConfig.elementalDebugEnabled = !DebugConfig.elementalDebugEnabled;
                    DebugConfig.save();
                    btn.setMessage(getElementalDebugText());
                })
                .bounds(centerX - btnW / 2, firstY, btnW, btnH)
                .build());

        // 設定2: Re:Cross Hookshot デバッグ表示
        addRenderableWidget(Button.builder(
                getRecrossDebugText(),
                btn -> {
                    DebugConfig.recrossHookshotDebugEnabled = !DebugConfig.recrossHookshotDebugEnabled;
                    DebugConfig.save();
                    btn.setMessage(getRecrossDebugText());
                })
                .bounds(centerX - btnW / 2, firstY + (btnH + 4), btnW, btnH)
                .build());

        // Done ボタン (バニラと同じ位置)
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
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    private Component getElementalDebugText() {
        Component status = DebugConfig.elementalDebugEnabled
                ? Component.literal("§aON")
                : Component.literal("§cOFF");
        return Component.translatable("screen.minecraft_armor_weapon.debug_config.elemental").append(": ").append(status);
    }

    private Component getRecrossDebugText() {
        Component status = DebugConfig.recrossHookshotDebugEnabled
                ? Component.literal("§aON")
                : Component.literal("§cOFF");
        return Component.translatable("screen.minecraft_armor_weapon.debug_config.recross").append(": ").append(status);
    }
}
