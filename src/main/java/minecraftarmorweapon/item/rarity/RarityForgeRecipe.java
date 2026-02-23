package minecraftarmorweapon.item.rarity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * レアリティ強化台のレシピ定義
 */
public class RarityForgeRecipe {

    private final List<Ingredient> ingredients;
    private final Item result;

    public RarityForgeRecipe(Item result, Ingredient... ingredients) {
        this.result = result;
        this.ingredients = List.of(ingredients);
    }

    public Item getResult() {
        return result;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    /**
     * スロット0(素材1)とスロット1(素材2)が一致するか確認
     */
    public boolean matches(IItemHandler handler) {
        // 素材スロット0とスロット1をチェック
        ItemStack slot0 = handler.getStackInSlot(0);
        ItemStack slot1 = handler.getStackInSlot(1);

        if (ingredients.size() == 1) {
            Ingredient ing = ingredients.get(0);
            return ing.matches(slot0) && slot1.isEmpty();
        }
        if (ingredients.size() == 2) {
            Ingredient ing0 = ingredients.get(0);
            Ingredient ing1 = ingredients.get(1);
            return ing0.matches(slot0) && ing1.matches(slot1);
        }
        return false;
    }

    /**
     * 素材を消費する
     */
    public void consumeIngredients(IItemHandler handler) {
        ItemStack slot0 = handler.getStackInSlot(0);
        ItemStack slot1 = handler.getStackInSlot(1);

        if (ingredients.size() >= 1) {
            slot0.shrink(ingredients.get(0).count);
        }
        if (ingredients.size() >= 2) {
            slot1.shrink(ingredients.get(1).count);
        }
    }

    /**
     * レシピの素材定義
     */
    public static class Ingredient {
        private final Item item;
        private final int count;

        public Ingredient(Item item, int count) {
            this.item = item;
            this.count = count;
        }

        public Item getItem() { return item; }
        public int getCount() { return count; }

        public ItemStack toStack() {
            return new ItemStack(item, count);
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == item && stack.getCount() >= count;
        }
    }
}
