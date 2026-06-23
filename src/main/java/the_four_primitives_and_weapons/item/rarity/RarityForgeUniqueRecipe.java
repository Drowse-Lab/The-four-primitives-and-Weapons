package the_four_primitives_and_weapons.item.rarity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * レアリティ解放テーブル専用 shaped レシピ。
 * 3×3 グリッドのみ ( 触媒は無視 — 既存の RarityForgeCenterLogic は通常通り適用 )。
 */
public class RarityForgeUniqueRecipe {

    private final Item[][] pattern;  // [height][width]、 null = 空セル
    private final int patternWidth;
    private final int patternHeight;
    private final Item result;
    private final int resultCount;

    public RarityForgeUniqueRecipe(Item result, int resultCount, Item[][] pattern, int width, int height) {
        this.result = result;
        this.resultCount = Math.max(1, resultCount);
        this.pattern = pattern;
        this.patternWidth = width;
        this.patternHeight = height;
    }

    public Item getResult() { return result; }
    public int getResultCount() { return resultCount; }
    public ItemStack getResultStack() { return new ItemStack(result, resultCount); }
    public Item[][] getPattern() { return pattern; }
    public int getPatternWidth() { return patternWidth; }
    public int getPatternHeight() { return patternHeight; }

    /** 3×3 grid ( 9 スロット ) にこのレシピがマッチするか、 オフセット込みで判定。 */
    public int[] findMatchOffset(IItemHandler grid) {
        for (int oy = 0; oy <= 3 - patternHeight; oy++) {
            for (int ox = 0; ox <= 3 - patternWidth; ox++) {
                if (matchesAt(grid, ox, oy)) return new int[]{ox, oy};
            }
        }
        return null;
    }

    public boolean matches(IItemHandler grid) {
        return findMatchOffset(grid) != null;
    }

    private boolean matchesAt(IItemHandler grid, int ox, int oy) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int py = y - oy;
                int px = x - ox;
                Item expected = null;
                if (py >= 0 && py < patternHeight && px >= 0 && px < patternWidth) {
                    expected = pattern[py][px];
                }
                ItemStack slot = grid.getStackInSlot(y * 3 + x);
                if (expected == null) {
                    if (!slot.isEmpty()) return false;
                } else {
                    if (slot.isEmpty() || slot.getItem() != expected) return false;
                }
            }
        }
        return true;
    }

    /** マッチしたパターン位置の grid slot を 1 個ずつ shrink。 */
    public void consumeIngredients(IItemHandler grid) {
        int[] offset = findMatchOffset(grid);
        if (offset == null) return;
        for (int y = 0; y < patternHeight; y++) {
            for (int x = 0; x < patternWidth; x++) {
                if (pattern[y][x] != null) {
                    int slot = (y + offset[1]) * 3 + (x + offset[0]);
                    grid.extractItem(slot, 1, false);
                }
            }
        }
    }
}
