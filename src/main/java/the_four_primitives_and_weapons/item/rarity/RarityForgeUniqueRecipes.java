package the_four_primitives_and_weapons.item.rarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * レアリティ解放テーブル専用レシピの registry。
 * バニラの crafting recipe には載らず、 このテーブルだけで作れる特殊レシピ。
 * RarityForgeUniqueRecipeManager が data/<ns>/rarity_forge_unique_recipes/ の
 * JSON を読み込んで setRecipes() でセットする。
 */
public final class RarityForgeUniqueRecipes {

    private static List<RarityForgeUniqueRecipe> RECIPES = new ArrayList<>();

    private RarityForgeUniqueRecipes() {}

    public static void setRecipes(List<RarityForgeUniqueRecipe> recipes) {
        RECIPES = new ArrayList<>(recipes);
    }

    public static List<RarityForgeUniqueRecipe> getAll() {
        return Collections.unmodifiableList(RECIPES);
    }
}
