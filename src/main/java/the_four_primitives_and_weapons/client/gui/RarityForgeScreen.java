package the_four_primitives_and_weapons.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import the_four_primitives_and_weapons.item.rarity.RarityCraftingLogic;
import the_four_primitives_and_weapons.item.rarity.RarityForgeCenterLogic;
import the_four_primitives_and_weapons.item.rarity.WeaponRarity;
import the_four_primitives_and_weapons.world.inventory.RarityForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * シンプル化レアリティ解放テーブル GUI
 *   [触媒0] [触媒1]   [中央]   →  [結果]
 */
public class RarityForgeScreen extends AbstractContainerScreen<RarityForgeMenu> {

    public RarityForgeScreen(RarityForgeMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 198;
        this.imageHeight = 184;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTicks);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        int lx = this.leftPos;
        int ly = this.topPos;

        // メイン背景
        gfx.fill(lx, ly, lx + imageWidth, ly + imageHeight, 0xFFC6C6C6);
        draw3DBorder(gfx, lx, ly, imageWidth, imageHeight, true);

        // 上部パネル
        drawInsetPanel(gfx, lx + 6, ly + 18, imageWidth - 12, 68);

        // 触媒 ×2
        drawSlotBg(gfx, lx + 20, ly + 56);
        drawSlotBg(gfx, lx + 44, ly + 56);
        // 中央スロット
        drawSlotBg(gfx, lx + 96, ly + 56);
        // 結果スロット
        drawSlotBg(gfx, lx + 152, ly + 56);

        // プレイヤーインベントリ枠
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                drawSlotBg(gfx, lx + 8 + c * 18, ly + 100 + r * 18);
        for (int c = 0; c < 9; c++)
            drawSlotBg(gfx, lx + 8 + c * 18, ly + 158);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, Component.literal("§lレアリティ解放テーブル"), 5, 6, 0x404040, false);
        gfx.drawString(font, Component.literal("触媒"), 24, 44, 0x404040, false);
        gfx.drawString(font, Component.literal("中央"), 100, 44, 0x404040, false);
        gfx.drawString(font, Component.literal("結果"), 152, 44, 0x404040, false);
        gfx.drawString(font, Component.literal("→"), 138, 60, 0x404040, false);
        gfx.drawString(font, Component.literal("インベントリ"), 8, 90, 0x404040, false);

        // モード表示
        ItemStack center = menu.getInternal().getStackInSlot(RarityForgeMenu.CENTER_SLOT);
        ItemStack cat0 = menu.getInternal().getStackInSlot(RarityForgeMenu.CAT_SLOT_0);
        ItemStack cat1 = menu.getInternal().getStackInSlot(RarityForgeMenu.CAT_SLOT_1);
        RarityForgeCenterLogic.Mode mode = RarityForgeCenterLogic.resolveMode(center, cat0, cat1);
        String modeLabel;
        switch (mode) {
            case BOOK_ELEMENT: modeLabel = "§b魔導書 element 付与"; break;
            case UNBREAKABLE:  modeLabel = "§6Unbreakable 付与"; break;
            case RARITY:       modeLabel = "§dレアリティ抽選"; break;
            case NONE: default: modeLabel = "§7( 中央と触媒を設置 )"; break;
        }
        gfx.drawString(font, Component.literal(modeLabel), 6, 76, 0xFFFFFF, false);

        // RARITY モードのみ確率表示
        if (mode == RarityForgeCenterLogic.Mode.RARITY) {
            int dy = 18;
            float scale = 0.8f;
            WeaponRarity transferred = RarityCraftingLogic.getTransferredRarity(cat0, cat1);
            int[] weights = RarityCraftingLogic.getWeightsForCatalysts(cat0, cat1);
            if (weights == null) {
                gfx.drawString(font, Component.literal("§4Forbidden §c100%"), 110, dy, 0xFFFFFF, false);
            } else if (transferred != null) {
                String line = transferred.getColorCode() + transferred.getDisplayName() + " §f100%";
                gfx.drawString(font, Component.literal(line), 110, dy, 0xFFFFFF, false);
            } else {
                int total = 0;
                for (int w : weights) total += w;
                WeaponRarity[] rarities = WeaponRarity.values();
                int curY = dy;
                for (int i = 0; i < weights.length && i < rarities.length; i++) {
                    if (weights[i] <= 0) continue;
                    int pct = weights[i] * 100 / total;
                    String line = rarities[i].getColorCode() + rarities[i].getDisplayName() + " §f" + pct + "%";
                    gfx.pose().pushPose();
                    gfx.pose().translate(110, curY, 0);
                    gfx.pose().scale(scale, scale, 1);
                    gfx.drawString(font, Component.literal(line), 0, 0, 0xFFFFFF, false);
                    gfx.pose().popPose();
                    curY += 8;
                }
            }
        } else if (mode == RarityForgeCenterLogic.Mode.BOOK_ELEMENT) {
            int lvl = Math.max(
                    RarityForgeCenterLogic.getCatalystLevel(cat0),
                    RarityForgeCenterLogic.getCatalystLevel(cat1));
            gfx.drawString(font, Component.literal("§bLv " + lvl), 110, 18, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    // === 描画ヘルパー ===

    private void draw3DBorder(GuiGraphics gfx, int x, int y, int w, int h, boolean raised) {
        int light = raised ? 0xFFFFFFFF : 0xFF373737;
        int dark = raised ? 0xFF555555 : 0xFFFFFFFF;
        gfx.fill(x, y, x + w, y + 1, light);
        gfx.fill(x, y, x + 1, y + h, light);
        gfx.fill(x + w - 1, y, x + w, y + h, dark);
        gfx.fill(x, y + h - 1, x + w, y + h, dark);
    }

    private void drawInsetPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, 0xFF8B8B8B);
        draw3DBorder(gfx, x, y, w, h, false);
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        gfx.fill(x, y, x + 18, y + 1, 0xFF373737);
        gfx.fill(x, y, x + 1, y + 18, 0xFF373737);
        gfx.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
        gfx.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }
}
