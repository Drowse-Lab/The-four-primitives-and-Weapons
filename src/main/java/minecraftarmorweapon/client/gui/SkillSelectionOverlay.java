package minecraftarmorweapon.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.OpenSkillScreenPacket;

/**
 * インベントリ画面の左上にスキル選択ボタンを追加する
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, value = Dist.CLIENT)
public class SkillSelectionOverlay {

    private static final int COLOR_BUTTON_NORMAL = 0xFF2D6A4F;
    private static final int COLOR_BUTTON_HOVER = 0xFF3D8A6F;
    private static final int COLOR_BORDER = 0xFF4CAF50;

    @SubscribeEvent
    public static void onInventoryOpen(ScreenEvent.Init.Post event) {
        int guiLeft, guiTop;
        if (event.getScreen() instanceof InventoryScreen invScreen) {
            guiLeft = invScreen.getGuiLeft();
            guiTop = invScreen.getGuiTop();
        } else if (event.getScreen() instanceof CreativeModeInventoryScreen creativeScreen) {
            guiLeft = creativeScreen.getGuiLeft();
            guiTop = creativeScreen.getGuiTop();
        } else {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // スキルボタンを配置（クリエイティブはタブがあるので右上にずらす）
        boolean isCreative = event.getScreen() instanceof CreativeModeInventoryScreen;
        int x = isCreative ? guiLeft - 24 : guiLeft + 4;
        int y = isCreative ? guiTop + 4 : guiTop - 22;

        AbstractButton skillButton = new AbstractButton(x, y, 20, 20, Component.literal("\u2694")) {
            @Override
            public void onPress() {
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new OpenSkillScreenPacket());
            }

            @Override
            public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                boolean hovered = this.isHoveredOrFocused();
                int bgColor = hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_NORMAL;

                // 背景
                g.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + this.height, bgColor);

                // 枠線
                g.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + 1, COLOR_BORDER);
                g.fill(this.getX(), this.getY() + this.height - 1,
                    this.getX() + this.width, this.getY() + this.height, COLOR_BORDER);
                g.fill(this.getX(), this.getY(),
                    this.getX() + 1, this.getY() + this.height, COLOR_BORDER);
                g.fill(this.getX() + this.width - 1, this.getY(),
                    this.getX() + this.width, this.getY() + this.height, COLOR_BORDER);

                // アイコン
                g.drawCenteredString(mc.font, this.getMessage(),
                    this.getX() + this.width / 2,
                    this.getY() + (this.height - 8) / 2,
                    hovered ? 0xFFFFFF : 0xE0E0E0);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                this.defaultButtonNarrationText(narrationElementOutput);
            }
        };

        event.addListener(skillButton);
    }
}
