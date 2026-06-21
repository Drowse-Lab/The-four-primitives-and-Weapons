package the_four_primitives_and_weapons.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.rarity.RarityCraftingLogic;
import the_four_primitives_and_weapons.item.rarity.RarityForgeRecipe;
import the_four_primitives_and_weapons.item.rarity.RarityForgeRecipes;
import the_four_primitives_and_weapons.item.rarity.WeaponRarity;
import the_four_primitives_and_weapons.network.RarityForgeButtonMessage;
import the_four_primitives_and_weapons.world.inventory.RarityForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * レアリティ解放テーブルのGUI画面
 * 左: 触媒スロット×2, 中央: 3×3クラフトグリッド, 右: クラフト候補リスト
 */
public class RarityForgeScreen extends AbstractContainerScreen<RarityForgeMenu> {

    private final int x, y, z;

    /** クラフト可能なレシピのインデックス（RarityForgeRecipes.getAll()内） */
    private List<Integer> craftableIndices = List.of();
    private int scrollOffset = 0;
    /** ホバー中のレシピのグローバルインデックス（getAll()内） */
    private int hoveredRecipeIndex = -1;

    // --- 右パネル（候補リスト）レイアウト ---
    private static final int VISIBLE_ROWS = 5;
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_X = 143;
    private static final int LIST_Y = 34;
    private static final int LIST_W = 128;
    private static final int LIST_H = VISIBLE_ROWS * ROW_HEIGHT;

    // --- 中央パネル（クラフトグリッド）レイアウト ---
    private static final int GRID_X = 76;
    private static final int GRID_Y = 38;
    private static final int CELL = 18;

    public RarityForgeScreen(RarityForgeMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.imageWidth = 280;
        this.imageHeight = 220;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        List<RarityForgeRecipe> all = RarityForgeRecipes.getAll();
        List<RarityForgeRecipe> craftable = menu.getMatchingRecipes();
        List<Integer> newIndices = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (craftable.contains(all.get(i))) {
                newIndices.add(i);
            }
        }
        craftableIndices = newIndices;
        int maxScroll = Math.max(0, craftableIndices.size() - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTicks);

        // スロットツールチップ (結果スロットも含む)
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

        // === 左パネル（触媒）===
        drawInsetPanel(gfx, lx + 3, ly + 18, 63, 108);
        drawSlotBg(gfx, lx + 9, ly + 54);
        drawSlotBg(gfx, lx + 39, ly + 54);

        // === 中央パネル（3×3クラフトグリッド）===
        drawInsetPanel(gfx, lx + 69, ly + 18, 66, 108);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                drawSlotBg(gfx, lx + GRID_X + c * CELL, ly + GRID_Y + r * CELL);

        // === 右パネル（結果スロット + 候補リスト）===
        drawInsetPanel(gfx, lx + 139, ly + 18, 136, 108);
        // 結果スロット (バニラ作業台と同じ位置感、矢印の右側)
        drawSlotBg(gfx, lx + 166, ly + 56);

        // === プレイヤーインベントリ ===
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                drawSlotBg(gfx, lx + 58 + c * 18, ly + 137 + r * 18);
        for (int c = 0; c < 9; c++)
            drawSlotBg(gfx, lx + 58 + c * 18, ly + 195);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, Component.literal("\u00A7l\u30EC\u30A2\u30EA\u30C6\u30A3\u89E3\u653E\u30C6\u30FC\u30D6\u30EB"), 5, 6, 0x404040, false);
        gfx.drawString(font, Component.literal("\u89E6\u5A92"), 20, 22, 0x404040, false);
        gfx.drawString(font, Component.literal("\u30AF\u30E9\u30D5\u30C8"), 83, 26, 0x404040, false);
        gfx.drawString(font, Component.literal("\u7D50\u679C"), 175, 22, 0x404040, false); // \u300C\u7D50\u679C\u300D
        gfx.drawString(font, Component.literal("+"), 31, 59, 0x404040, false);
        gfx.drawString(font, Component.literal("\u2192"), 152, 58, 0x404040, false);
        gfx.drawString(font, Component.literal("\u30A4\u30F3\u30D9\u30F3\u30C8\u30EA"), 59, 128, 0x404040, false);

        // === 触媒によるレアリティ確率表示 ===
        ItemStack cat0 = menu.getInternal().getStackInSlot(0);
        ItemStack cat1 = menu.getInternal().getStackInSlot(1);
        if (!cat0.isEmpty() || !cat1.isEmpty()) {
            int dy = 78;
            float scale = 0.8f;

            // レアリティ付き触媒 → そのレアリティを確定表示
            WeaponRarity transferred = RarityCraftingLogic.getTransferredRarity(cat0, cat1);
            int[] weights = RarityCraftingLogic.getWeightsForCatalysts(cat0, cat1);

            if (weights == null) {
                // 禁忌確定（CURSED触媒）
                gfx.drawString(font, Component.literal("\u00A74Forbidden \u00A7c100%"), 5, dy, 0xFFFFFF, false);
            } else if (transferred != null) {
                // レアリティ付きアイテムが触媒 → そのレアリティ確定
                String line = transferred.getColorCode() + transferred.getDisplayName() + " \u00A7f100%";
                gfx.drawString(font, Component.literal(line), 5, dy, 0xFFFFFF, false);
            } else {
                int total = 0;
                for (int w : weights) total += w;
                WeaponRarity[] rarities = WeaponRarity.values();
                for (int i = 0; i < weights.length && i < rarities.length; i++) {
                    if (weights[i] <= 0) continue;
                    int pct = weights[i] * 100 / total;
                    String line = rarities[i].getColorCode() + rarities[i].getDisplayName()
                            + " \u00A7f" + pct + "%";
                    gfx.pose().pushPose();
                    gfx.pose().translate(5, dy, 0);
                    gfx.pose().scale(scale, scale, 1);
                    gfx.drawString(font, Component.literal(line), 0, 0, 0xFFFFFF, false);
                    gfx.pose().popPose();
                    dy += 8;
                }
            }
        }
    }

    /** クラフト候補リスト描画（クラフト可能なレシピのみ表示） — 旧 UI 用、現在は呼ばれない。 */
    @SuppressWarnings("unused")
    private void renderCandidateList(GuiGraphics gfx, int mouseX, int mouseY) {
        int lx = this.leftPos + LIST_X;
        int ly = this.topPos + LIST_Y;
        hoveredRecipeIndex = -1;

        if (craftableIndices.isEmpty()) return;

        List<RarityForgeRecipe> all = RarityForgeRecipes.getAll();
        gfx.enableScissor(lx, ly, lx + LIST_W, ly + LIST_H);
        for (int i = 0; i < craftableIndices.size(); i++) {
            int di = i - scrollOffset;
            if (di < 0 || di >= VISIBLE_ROWS) continue;

            int globalIdx = craftableIndices.get(i);
            RarityForgeRecipe recipe = all.get(globalIdx);
            int rowY = ly + di * ROW_HEIGHT;

            boolean hovered = mouseX >= lx && mouseX < lx + LIST_W
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                hoveredRecipeIndex = globalIdx;
                gfx.fill(lx, rowY, lx + LIST_W, rowY + ROW_HEIGHT, 0x6000FF00);
            } else {
                gfx.fill(lx, rowY, lx + LIST_W, rowY + ROW_HEIGHT,
                        (i % 2 == 0) ? 0x20000000 : 0x10000000);
            }

            ItemStack stack = new ItemStack(recipe.getResult());
            gfx.renderItem(stack, lx + 1, rowY + 1);

            String name = stack.getHoverName().getString();
            int maxW = LIST_W - 22;
            if (font.width(name) > maxW) {
                while (name.length() > 1 && font.width(name + "..") > maxW)
                    name = name.substring(0, name.length() - 1);
                name += "..";
            }
            gfx.drawString(font, name, lx + 19, rowY + 5, 0xFFFFFF, true);
        }
        gfx.disableScissor();

        // スクロールバー
        if (craftableIndices.size() > VISIBLE_ROWS) {
            int sbX = lx + LIST_W - 4;
            int maxS = craftableIndices.size() - VISIBLE_ROWS;
            int thumbH = Math.max(10, LIST_H * VISIBLE_ROWS / craftableIndices.size());
            int thumbY = ly + (LIST_H - thumbH) * scrollOffset / maxS;
            gfx.fill(sbX, ly, sbX + 4, ly + LIST_H, 0x40000000);
            gfx.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xA0FFFFFF);
        }
    }

    // 旧フロー (候補リストクリックで craft) は撤去。result スロットの通常クリックで craft される。

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
