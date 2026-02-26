package minecraftarmorweapon.item.rarity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * レアリティ強化台の3×3シェイプレシピ
 */
public class RarityForgeRecipe {

    private final Item[][] pattern; // [height][width], null = 空
    private final int patternWidth;
    private final int patternHeight;
    private final Item result;

    public RarityForgeRecipe(Item result, Item[][] pattern, int width, int height) {
        this.result = result;
        this.pattern = pattern;
        this.patternWidth = width;
        this.patternHeight = height;
    }

    public Item getResult() {
        return result;
    }

    public Item[][] getPattern() {
        return pattern;
    }

    public int getPatternWidth() {
        return patternWidth;
    }

    public int getPatternHeight() {
        return patternHeight;
    }

    /**
     * 3×3グリッド（スロット0-8）に一致するか確認（パターンのオフセット対応）
     */
    public boolean matches(IItemHandler handler) {
        return findMatchOffset(handler) != null;
    }

    /**
     * マッチするオフセットを見つける。見つからなければnull
     */
    public int[] findMatchOffset(IItemHandler handler) {
        for (int oy = 0; oy <= 3 - patternHeight; oy++) {
            for (int ox = 0; ox <= 3 - patternWidth; ox++) {
                if (matchesAt(handler, ox, oy)) return new int[]{ox, oy};
            }
        }
        return null;
    }

    private boolean matchesAt(IItemHandler handler, int ox, int oy) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int py = y - oy;
                int px = x - ox;
                Item expected = null;
                if (py >= 0 && py < patternHeight && px >= 0 && px < patternWidth) {
                    expected = pattern[py][px];
                }
                ItemStack slot = handler.getStackInSlot(y * 3 + x);
                if (expected == null) {
                    if (!slot.isEmpty()) return false;
                } else {
                    if (slot.isEmpty() || slot.getItem() != expected) return false;
                }
            }
        }
        return true;
    }

    /**
     * 素材を消費する（マッチしたパターン位置のスロットを各1個消費）
     */
    public void consumeIngredients(IItemHandler handler) {
        int[] offset = findMatchOffset(handler);
        if (offset == null) return;
        for (int y = 0; y < patternHeight; y++) {
            for (int x = 0; x < patternWidth; x++) {
                if (pattern[y][x] != null) {
                    int slot = (y + offset[1]) * 3 + (x + offset[0]);
                    handler.getStackInSlot(slot).shrink(1);
                }
            }
        }
    }

    /**
     * レシピに必要な素材とその個数を返す
     */
    public Map<Item, Integer> getIngredientCounts() {
        Map<Item, Integer> counts = new HashMap<>();
        for (int y = 0; y < patternHeight; y++) {
            for (int x = 0; x < patternWidth; x++) {
                if (pattern[y][x] != null) {
                    counts.merge(pattern[y][x], 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    /**
     * 2スロットの素材で作成可能かチェック
     */
    public boolean canCraftWith(IItemHandler handler) {
        Map<Item, Integer> required = getIngredientCounts();
        Map<Item, Integer> available = new HashMap<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                available.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        for (Entry<Item, Integer> entry : required.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        return true;
    }

    /**
     * 2スロットから素材を消費する
     */
    public void consumeFromHandler(IItemHandler handler) {
        Map<Item, Integer> toConsume = new HashMap<>(getIngredientCounts());
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            Integer need = toConsume.get(stack.getItem());
            if (need != null && need > 0) {
                int take = Math.min(need, stack.getCount());
                handler.extractItem(slot, take, false);
                need -= take;
                if (need <= 0) toConsume.remove(stack.getItem());
                else toConsume.put(stack.getItem(), need);
            }
        }
    }

    /**
     * パターン文字列から簡単にレシピを作成するヘルパー
     */
    public static RarityForgeRecipe shaped(Item result, String[] rows, Object... keys) {
        Map<Character, Item> keyMap = new HashMap<>();
        for (int i = 0; i < keys.length; i += 2) {
            keyMap.put((Character) keys[i], (Item) keys[i + 1]);
        }
        int height = rows.length;
        int width = 0;
        for (String row : rows) width = Math.max(width, row.length());

        Item[][] grid = new Item[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                if (c != ' ') {
                    grid[y][x] = keyMap.get(c);
                }
            }
        }
        return new RarityForgeRecipe(result, grid, width, height);
    }
}
