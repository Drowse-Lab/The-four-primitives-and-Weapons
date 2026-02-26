package minecraftarmorweapon.item.rarity;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * レアリティ強化台のレシピ一覧（3×3シェイプレシピ）
 */
public final class RarityForgeRecipes {

    private static final List<RarityForgeRecipe> RECIPES = new ArrayList<>();

    private RarityForgeRecipes() {}

    static {
        // === バニラ武器 ===
        // 木の剣
        sword(Items.WOODEN_SWORD, Items.OAK_PLANKS);
        // 石の剣
        sword(Items.STONE_SWORD, Items.COBBLESTONE);
        // 鉄の剣
        sword(Items.IRON_SWORD, Items.IRON_INGOT);
        // 金の剣
        sword(Items.GOLDEN_SWORD, Items.GOLD_INGOT);
        // ダイヤの剣
        sword(Items.DIAMOND_SWORD, Items.DIAMOND);
        // ネザライトの剣
        sword(Items.NETHERITE_SWORD, Items.NETHERITE_INGOT);

        // === バニラ斧 ===
        axe(Items.WOODEN_AXE, Items.OAK_PLANKS);
        axe(Items.STONE_AXE, Items.COBBLESTONE);
        axe(Items.IRON_AXE, Items.IRON_INGOT);
        axe(Items.GOLDEN_AXE, Items.GOLD_INGOT);
        axe(Items.DIAMOND_AXE, Items.DIAMOND);
        axe(Items.NETHERITE_AXE, Items.NETHERITE_INGOT);

        // === 石系 ===
        // 石カタナ: 丸石2 + 棒1 (剣パターン)
        sword(MinecraftArmorWeaponModItems.STONE_KATANA.get(), Items.COBBLESTONE);

        // === 鉄系 ===
        sword(MinecraftArmorWeaponModItems.IRON_KATANA.get(), Items.IRON_INGOT);
        sword(MinecraftArmorWeaponModItems.MY_TEST_IRON_KATANA.get(), Items.IRON_INGOT);
        sword(MinecraftArmorWeaponModItems.NINJATOU.get(), Items.IRON_INGOT);
        // 小太刀: 鉄1 + 棒1
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.SMALL_SWORD.get(),
                new String[]{"I", "S"}, 'I', Items.IRON_INGOT, 'S', Items.STICK));

        // === 金系 ===
        sword(MinecraftArmorWeaponModItems.GOLD_KATANA.get(), Items.GOLD_INGOT);

        // === ネザライト系 ===
        sword(MinecraftArmorWeaponModItems.NETHERITE_KATANA.get(), Items.NETHERITE_INGOT);

        // === 倶利伽羅剣系 (鉄 + ブレイズロッド) ===
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.KURIKARAKEN.get(),
                new String[]{"I", "I", "B"}, 'I', Items.IRON_INGOT, 'B', Items.BLAZE_ROD));
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get(),
                new String[]{"I", "I", "B"}, 'I', Items.IRON_INGOT, 'B', Items.BLAZE_ROD));
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get(),
                new String[]{"I", "I", "B"}, 'I', Items.IRON_INGOT, 'B', Items.BLAZE_ROD));

        // === 特殊武器 ===
        // 鎌: 鉄3 + 棒2
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.SCYTHE.get(),
                new String[]{"II", " S", " S"}, 'I', Items.IRON_INGOT, 'S', Items.STICK));
        // マチェーテ
        sword(MinecraftArmorWeaponModItems.MACHETE.get(), Items.IRON_INGOT);
        // 試作刀
        sword(MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get(), Items.IRON_INGOT);
        // 蕨手刀: 鉄1 + 棒1
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.WARABITETOU.get(),
                new String[]{"I", "S"}, 'I', Items.IRON_INGOT, 'S', Items.STICK));
        // 古刀
        sword(MinecraftArmorWeaponModItems.OLD_KATANA.get(), Items.IRON_INGOT);

        // === Bluepurge系 (鉄 + ラピス) ===
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.BLUEPURGE.get(),
                new String[]{"L", "I", "I"}, 'I', Items.IRON_INGOT, 'L', Items.LAPIS_LAZULI));
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.BLUEPURGE_UTIGATANA.get(),
                new String[]{"L", "I", "I"}, 'I', Items.IRON_INGOT, 'L', Items.LAPIS_LAZULI));
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.BLUEPURGE_TYOKUTOU.get(),
                new String[]{"L", "I", "I"}, 'I', Items.IRON_INGOT, 'L', Items.LAPIS_LAZULI));

        // === 闇系 (鉄 + ウィザーローズ) ===
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.DARKNESS_KATANA.get(),
                new String[]{"W", "I", "I"}, 'I', Items.IRON_INGOT, 'W', Items.WITHER_ROSE));
        add(RarityForgeRecipe.shaped(MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get(),
                new String[]{"W", "I", "I"}, 'I', Items.IRON_INGOT, 'W', Items.WITHER_ROSE));
    }

    /** 標準的な剣パターン: 素材2 + 棒1 (縦) */
    private static void sword(Item result, Item material) {
        add(RarityForgeRecipe.shaped(result,
                new String[]{"M", "M", "S"}, 'M', material, 'S', Items.STICK));
    }

    /** 標準的な斧パターン: 素材3 + 棒2 */
    private static void axe(Item result, Item material) {
        add(RarityForgeRecipe.shaped(result,
                new String[]{"MM", "MS", " S"}, 'M', material, 'S', Items.STICK));
    }

    private static void add(RarityForgeRecipe recipe) {
        RECIPES.add(recipe);
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
