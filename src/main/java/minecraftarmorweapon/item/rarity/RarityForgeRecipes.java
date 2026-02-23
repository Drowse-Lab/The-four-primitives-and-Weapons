package minecraftarmorweapon.item.rarity;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * レアリティ強化台のレシピ一覧
 */
public final class RarityForgeRecipes {

    private static final List<RarityForgeRecipe> RECIPES = new ArrayList<>();

    private RarityForgeRecipes() {}

    static {
        // === 石系 (2 cobblestone + 1 stick) ===
        add(MinecraftArmorWeaponModItems.STONE_KATANA.get(),
                ing(Items.COBBLESTONE, 2), ing(Items.STICK, 1));

        // === 鉄系 (2 iron + 1 stick) ===
        add(MinecraftArmorWeaponModItems.IRON_KATANA.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.STICK, 1));
        add(MinecraftArmorWeaponModItems.MY_TEST_IRON_KATANA.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.STICK, 1));
        add(MinecraftArmorWeaponModItems.NINJATOU.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.STICK, 1));
        add(MinecraftArmorWeaponModItems.SMALL_SWORD.get(),
                ing(Items.IRON_INGOT, 1), ing(Items.STICK, 1));

        // === 金系 (2 gold + 1 stick) ===
        add(MinecraftArmorWeaponModItems.GOLD_KATANA.get(),
                ing(Items.GOLD_INGOT, 2), ing(Items.STICK, 1));

        // === ネザライト系 (2 netherite + 1 stick) ===
        add(MinecraftArmorWeaponModItems.NETHERITE_KATANA.get(),
                ing(Items.NETHERITE_INGOT, 2), ing(Items.STICK, 1));

        // === 倶利伽羅剣系 (2 iron + 1 blaze rod) ===
        add(MinecraftArmorWeaponModItems.KURIKARAKEN.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.BLAZE_ROD, 1));
        add(MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.BLAZE_ROD, 1));
        add(MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.BLAZE_ROD, 1));

        // === 特殊武器 ===
        add(MinecraftArmorWeaponModItems.SCYTHE.get(),
                ing(Items.IRON_INGOT, 3), ing(Items.STICK, 2));
        add(MinecraftArmorWeaponModItems.MACHETE.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.STICK, 1));
        add(MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.STICK, 1));
        add(MinecraftArmorWeaponModItems.WARABITETOU.get(),
                ing(Items.IRON_INGOT, 1), ing(Items.STICK, 1));
        add(MinecraftArmorWeaponModItems.OLD_KATANA.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.STICK, 1));

        // === Bluepurge系 (iron + lapis) ===
        add(MinecraftArmorWeaponModItems.BLUEPURGE.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.LAPIS_LAZULI, 2));
        add(MinecraftArmorWeaponModItems.BLUEPURGE_UTIGATANA.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.LAPIS_LAZULI, 2));
        add(MinecraftArmorWeaponModItems.BLUEPURGE_TYOKUTOU.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.LAPIS_LAZULI, 2));

        // === 闇系 (iron + wither rose) ===
        add(MinecraftArmorWeaponModItems.DARKNESS_KATANA.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.WITHER_ROSE, 1));
        add(MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get(),
                ing(Items.IRON_INGOT, 2), ing(Items.WITHER_ROSE, 1));
    }

    private static RarityForgeRecipe.Ingredient ing(net.minecraft.world.item.Item item, int count) {
        return new RarityForgeRecipe.Ingredient(item, count);
    }

    private static void add(net.minecraft.world.item.Item result, RarityForgeRecipe.Ingredient... ingredients) {
        RECIPES.add(new RarityForgeRecipe(result, ingredients));
    }

    /**
     * 全レシピを返す
     */
    public static List<RarityForgeRecipe> getAll() {
        return Collections.unmodifiableList(RECIPES);
    }

    /**
     * 入力スロットに一致するレシピを探す
     */
    public static RarityForgeRecipe findMatch(IItemHandler handler) {
        for (RarityForgeRecipe recipe : RECIPES) {
            if (recipe.matches(handler)) {
                return recipe;
            }
        }
        return null;
    }
}
