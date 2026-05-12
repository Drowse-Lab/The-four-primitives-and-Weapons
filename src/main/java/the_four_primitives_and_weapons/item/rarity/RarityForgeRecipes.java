package the_four_primitives_and_weapons.item.rarity;

import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * レアリティ強化台のレシピ一覧。
 * RarityForgeRecipeManager が data/the_four_primitives_and_weapons/rarity_forge_recipes/ の
 * JSONを読み込んで setRecipes() でセットする。
 */
public final class RarityForgeRecipes {

    private static List<RarityForgeRecipe> RECIPES = new ArrayList<>();

    private RarityForgeRecipes() {}

    /** RarityForgeRecipeManager から呼ばれる */
    public static void setRecipes(List<RarityForgeRecipe> recipes) {
        RECIPES = new ArrayList<>(recipes);
    }

    public static List<RarityForgeRecipe> getAll() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static RarityForgeRecipe findMatch(IItemHandler handler) {
        for (RarityForgeRecipe recipe : RECIPES) {
            if (recipe.matches(handler)) {
                return recipe;
            }
        }
        return null;
    }
}
