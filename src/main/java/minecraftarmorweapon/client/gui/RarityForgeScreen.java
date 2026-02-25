package minecraftarmorweapon.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.RarityForgeButtonMessage;
import minecraftarmorweapon.world.inventory.RarityForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RarityForgeScreen extends AbstractContainerScreen<RarityForgeMenu> {

    private static final ResourceLocation BG_BASE = new ResourceLocation("minecraft_armor_weapon:textures/screens/smithing_table_gui_2.png");
    private static final ResourceLocation BG_OVERLAY = new ResourceLocation("minecraft_armor_weapon:textures/screens/smithing_table_1.20.png");

    private final int x, y, z;

    public RarityForgeScreen(RarityForgeMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.imageWidth = 170;
        this.imageHeight = 160;
    }

    @Override
    public void render(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        super.render(ms, mouseX, mouseY, partialTicks);
        this.renderTooltip(ms, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics ms, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ms.blit(BG_BASE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        ms.blit(BG_OVERLAY, this.leftPos, this.topPos - 5, 0, 0, 170, 160, 170, 160);

        RenderSystem.disableBlend();
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.literal("\u00A7lRarity Forge"), 39, 15, 0x404040);
    }

    @Override
    public void init() {
        super.init();

        this.addRenderableWidget(Button.builder(
                Component.literal("\u00A76Forge!"),
                button -> {
                    MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                            new RarityForgeButtonMessage(x, y, z));
                }
        ).bounds(this.leftPos + 56, this.topPos + 62, 58, 16).build());
    }
}
