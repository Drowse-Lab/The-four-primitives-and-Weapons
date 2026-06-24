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

import java.util.List;

/**
 * Hybrid レアリティ解放テーブル GUI
 *   [触媒0]   [3×3 グリッド]   →   [結果]     [候補リスト]
 *   [触媒1]
 *
 * 右パネル: 現在のグリッドに部分マッチするバニラレシピを並べる ( 表示のみ )。
 */
public class RarityForgeScreen extends AbstractContainerScreen<RarityForgeMenu> {

    // 右パネル ( 候補リスト ) レイアウト
    private static final int LIST_X = 165;
    private static final int LIST_Y = 18;
    private static final int LIST_W = 105;
    private static final int VISIBLE_ROWS = 5;
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_H = VISIBLE_ROWS * ROW_HEIGHT;

    private List<ItemStack> matches = List.of();
    private int scrollOffset = 0;

    public RarityForgeScreen(RarityForgeMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 278;
        this.imageHeight = 184;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshMatches();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        // slot 同期が containerTick より遅れる場合があるので、 render でも最新化する。
        // ( これで「アイテムを置いた瞬間に candidates 側が反映されない」 問題を回避 )
        refreshMatches();
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTicks);
        renderCandidateList(gfx, mouseX, mouseY);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    private void refreshMatches() {
        matches = menu.getPartialMatchResults();
        int maxScroll = Math.max(0, matches.size() - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        int lx = this.leftPos;
        int ly = this.topPos;

        gfx.fill(lx, ly, lx + imageWidth, ly + imageHeight, 0xFFC6C6C6);
        draw3DBorder(gfx, lx, ly, imageWidth, imageHeight, true);

        // 左 + 中央パネル
        drawInsetPanel(gfx, lx + 6, ly + 14, 153, 78);
        // 右パネル ( 候補リスト )
        drawInsetPanel(gfx, lx + 162, ly + 14, 110, 78);

        // slot 背景は slot 位置から -1, -1 オフセット ( 18×18 の枠で 16×16 アイテムを中央配置 )
        // 触媒 ×2
        drawSlotBg(gfx, lx + 15, ly + 36);
        drawSlotBg(gfx, lx + 15, ly + 64);
        // 3×3 グリッド
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                drawSlotBg(gfx, lx + 51 + c * 18, ly + 22 + r * 18);
        // 結果スロット
        drawSlotBg(gfx, lx + 131, ly + 40);

        // インベントリ
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                drawSlotBg(gfx, lx + 9 + c * 18, ly + 101 + r * 18);
        for (int c = 0; c < 9; c++)
            drawSlotBg(gfx, lx + 9 + c * 18, ly + 159);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, Component.translatable("gui.the_four_primitives_and_weapons.rarity_forge.title").withStyle(net.minecraft.ChatFormatting.BOLD), 5, 4, 0x404040, false);
        gfx.drawString(font, Component.literal("→"), 116, 43, 0x404040, false);
        gfx.drawString(font, Component.translatable("gui.the_four_primitives_and_weapons.rarity_forge.candidates"), 165, 6, 0x404040, false);
        gfx.drawString(font, Component.translatable("gui.the_four_primitives_and_weapons.rarity_forge.inventory"), 8, 90, 0x404040, false);

        // モード判定 + ラベル
        boolean gridEmpty = true;
        for (int i = 0; i < RarityForgeMenu.GRID_SIZE; i++) {
            if (!menu.getInternal().getStackInSlot(RarityForgeMenu.GRID_START + i).isEmpty()) {
                gridEmpty = false; break;
            }
        }
        ItemStack cat0 = menu.getInternal().getStackInSlot(RarityForgeMenu.CAT_SLOT_0);
        ItemStack cat1 = menu.getInternal().getStackInSlot(RarityForgeMenu.CAT_SLOT_1);

        Component modeLabel;
        if (gridEmpty) {
            RarityForgeCenterLogic.Mode em = RarityForgeCenterLogic.resolveEnhanceMode(cat0, cat1);
            switch (em) {
                case BOOK_ELEMENT:
                    modeLabel = Component.translatable("gui.the_four_primitives_and_weapons.rarity_forge.mode.book").withStyle(net.minecraft.ChatFormatting.AQUA);
                    break;
                case RARITY:
                    modeLabel = Component.translatable("gui.the_four_primitives_and_weapons.rarity_forge.mode.rarity").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
                    break;
                default:
                    modeLabel = Component.translatable("gui.the_four_primitives_and_weapons.rarity_forge.mode.enhance_empty").withStyle(net.minecraft.ChatFormatting.GRAY);
                    break;
            }
        } else {
            modeLabel = Component.translatable("gui.the_four_primitives_and_weapons.rarity_forge.mode.craft").withStyle(net.minecraft.ChatFormatting.GREEN);
        }
        // 「Inventory」 ラベル ( 8, 90 ) と重ならないように、 結果スロットの右下の空きへ
        gfx.drawString(font, modeLabel, 80, 92, 0xFFFFFF, false);
    }

    /** 右パネルに部分マッチ候補を並べる。 */
    private void renderCandidateList(GuiGraphics gfx, int mouseX, int mouseY) {
        int lx = this.leftPos + LIST_X;
        int ly = this.topPos + LIST_Y;
        if (matches.isEmpty()) return;

        gfx.enableScissor(lx, ly, lx + LIST_W, ly + LIST_H);
        for (int i = 0; i < matches.size(); i++) {
            int di = i - scrollOffset;
            if (di < 0 || di >= VISIBLE_ROWS) continue;

            ItemStack stack = matches.get(i);
            int rowY = ly + di * ROW_HEIGHT;

            boolean hovered = mouseX >= lx && mouseX < lx + LIST_W
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            gfx.fill(lx, rowY, lx + LIST_W, rowY + ROW_HEIGHT,
                    hovered ? 0x4000FF00 : ((i % 2 == 0) ? 0x20000000 : 0x10000000));

            if (!stack.isEmpty()) {
                gfx.renderItem(stack, lx + 1, rowY + 1);
                String name = stack.getHoverName().getString();
                int maxW = LIST_W - 22;
                while (name.length() > 1 && font.width(name + "..") > maxW) {
                    name = name.substring(0, name.length() - 1);
                }
                if (font.width(name) > maxW) name += "..";
                gfx.drawString(font, name, lx + 19, rowY + 5, 0xFFFFFF, true);
            }
        }
        gfx.disableScissor();

        // スクロールバー
        if (matches.size() > VISIBLE_ROWS) {
            int sbX = lx + LIST_W - 4;
            int maxS = matches.size() - VISIBLE_ROWS;
            int thumbH = Math.max(10, LIST_H * VISIBLE_ROWS / matches.size());
            int thumbY = ly + (LIST_H - thumbH) * scrollOffset / maxS;
            gfx.fill(sbX, ly, sbX + 4, ly + LIST_H, 0x40000000);
            gfx.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xA0FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // 候補リストのクリック → サーバに「この候補を作りたい」 通知
        int lx = this.leftPos + LIST_X;
        int ly = this.topPos + LIST_Y;
        if (button == 0 && mx >= lx && mx < lx + LIST_W && my >= ly && my < ly + LIST_H) {
            int row = (int) ((my - ly) / ROW_HEIGHT);
            int index = scrollOffset + row;
            if (index >= 0 && index < matches.size()) {
                ItemStack picked = matches.get(index);
                String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getKey(picked.getItem()).toString();
                the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
                        new the_four_primitives_and_weapons.network.RarityForgeSelectCandidatePacket(itemId));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dy) {
        int lx = this.leftPos + LIST_X;
        int ly = this.topPos + LIST_Y;
        if (mx >= lx && mx < lx + LIST_W && my >= ly && my < ly + LIST_H) {
            int maxScroll = Math.max(0, matches.size() - VISIBLE_ROWS);
            if (dy > 0 && scrollOffset > 0) scrollOffset--;
            else if (dy < 0 && scrollOffset < maxScroll) scrollOffset++;
            return true;
        }
        return super.mouseScrolled(mx, my, dy);
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
