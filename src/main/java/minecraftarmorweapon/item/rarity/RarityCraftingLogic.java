package minecraftarmorweapon.item.rarity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * レアリティ強化のクラフトロジック。
 * 素材ティア・確率テーブル・ボーナスは全て
 * data/minecraft_armor_weapon/rarity_config/ のJSONから読み込まれる。
 */
public final class RarityCraftingLogic {

    private static final Random RANDOM = new Random();

    private RarityCraftingLogic() {}

    /** 素材ティア（5段階 + CURSED） */
    public enum MaterialTier {
        NONE,         // 0 - 空スロット
        BASIC,        // 1 - 基本素材（ティア未定義の全アイテム）
        IRON,         // 2 - 中級素材
        DIAMOND,      // 3 - 高級素材
        NETHER_STAR,  // 4 - 超希少素材
        CURSED        // 5 - 呪い素材
    }

    // === JSONから読み込まれるデータ（デフォルト値はフォールバック） ===

    private static Set<String> cursedItems = new HashSet<>(List.of(
            "minecraft_armor_weapon:rose",
            "minecraft_armor_weapon:blood_bottle"
    ));

    private static Set<String> netherStarItems = new HashSet<>(List.of(
            "minecraft:nether_star",
            "minecraft:dragon_egg",
            "minecraft:dragon_breath",
            "minecraft:totem_of_undying",
            "minecraft:heart_of_the_sea"
    ));

    private static Set<String> diamondItems = new HashSet<>(List.of(
            "minecraft:diamond",
            "minecraft:diamond_block",
            "minecraft:emerald",
            "minecraft:emerald_block",
            "minecraft:amethyst_shard",
            "minecraft:netherite_scrap",
            "minecraft:netherite_ingot"
    ));

    private static Set<String> ironItems = new HashSet<>(List.of(
            "minecraft:iron_ingot",
            "minecraft:iron_block",
            "minecraft:gold_ingot",
            "minecraft:gold_block",
            "minecraft:copper_ingot",
            "minecraft:lapis_lazuli",
            "minecraft:redstone",
            "minecraft:quartz",
            "minecraft:ender_pearl",
            "minecraft:blaze_rod",
            "minecraft:blaze_powder",
            "minecraft:ghast_tear",
            "minecraft:phantom_membrane",
            "minecraft:prismarine_shard",
            "minecraft:prismarine_crystals",
            "minecraft:shulker_shell",
            "minecraft:nautilus_shell",
            "minecraft:obsidian"
    ));

    private static int[][] weightsByLevel = {
            { 100,  0,  0,  0,  0 },
            {  50, 35, 12,  3,  0 },
            {  20, 35, 30, 12,  3 },
            {  10, 25, 35, 22,  8 },
            {   5, 15, 35, 30, 15 },
            {   3, 10, 27, 35, 25 },
            {   2,  8, 25, 40, 25 },
            {   1,  5, 18, 40, 36 },
            {   1,  4, 15, 40, 40 },
    };

    private static Map<String, Double> tierBonuses = new HashMap<>(Map.of(
            "none", 0.0,
            "basic", 0.5,
            "iron", 1.0,
            "diamond", 2.0,
            "nether_star", 3.0,
            "cursed", 4.0
    ));

    // === RarityCraftingConfigManager から呼ばれるセッター ===

    public static void setMaterialTiers(Set<String> cursed, Set<String> netherStar,
                                        Set<String> diamond, Set<String> iron) {
        cursedItems = cursed;
        netherStarItems = netherStar;
        diamondItems = diamond;
        ironItems = iron;
    }

    public static void setWeightsAndBonuses(int[][] weights, Map<String, Double> bonuses) {
        weightsByLevel = weights;
        tierBonuses = bonuses;
    }

    // === ロジック ===

    public static MaterialTier getMaterialTier(ItemStack material) {
        if (material.isEmpty()) return MaterialTier.NONE;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(material.getItem());
        String id = itemId.toString();

        if (cursedItems.contains(id)) return MaterialTier.BASIC;
        if (netherStarItems.contains(id)) return MaterialTier.NETHER_STAR;
        if (diamondItems.contains(id)) return MaterialTier.DIAMOND;
        if (ironItems.contains(id)) return MaterialTier.IRON;

        return MaterialTier.BASIC;
    }

    public static WeaponRarity rollRarity(ItemStack material) {
        return rollRarity(material, ItemStack.EMPTY);
    }

    public static WeaponRarity getTransferredRarity(ItemStack mat1, ItemStack mat2) {
        WeaponRarity r1 = WeaponRarity.getFromStack(mat1);
        WeaponRarity r2 = WeaponRarity.getFromStack(mat2);
        if (r1 != null && r2 != null) {
            return r1.getId() >= r2.getId() ? r1 : r2;
        }
        if (r1 != null) return r1;
        if (r2 != null) return r2;
        return null;
    }

    public static WeaponRarity rollRarity(ItemStack mat1, ItemStack mat2) {
        if (hasCursedPair(mat1, mat2)) {
            return WeaponRarity.FORBIDDEN;
        }

        WeaponRarity transferred = getTransferredRarity(mat1, mat2);
        if (transferred != null) return transferred;

        int level = getMaterialTier(mat1).ordinal() + getMaterialTier(mat2).ordinal();
        level = Math.min(level, weightsByLevel.length - 1);
        int[] weights = weightsByLevel[level];

        int total = 0;
        for (int w : weights) total += w;

        int roll = RANDOM.nextInt(total);
        int cumulative = 0;
        WeaponRarity[] rarities = WeaponRarity.values();

        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return rarities[i];
            }
        }

        return WeaponRarity.COMMON;
    }

    public static int[] getWeightsForCatalysts(ItemStack mat1, ItemStack mat2) {
        if (hasCursedPair(mat1, mat2)) {
            return null;
        }
        int level = getMaterialTier(mat1).ordinal() + getMaterialTier(mat2).ordinal();
        level = Math.min(level, weightsByLevel.length - 1);
        return weightsByLevel[level];
    }

    public static double getCatalystBonus(ItemStack mat1, ItemStack mat2) {
        return getTierBonus(getMaterialTier(mat1)) + getTierBonus(getMaterialTier(mat2));
    }

    private static boolean hasCursedPair(ItemStack mat1, ItemStack mat2) {
        if (mat1.isEmpty() || mat2.isEmpty()) return false;
        String id1 = ForgeRegistries.ITEMS.getKey(mat1.getItem()).toString();
        String id2 = ForgeRegistries.ITEMS.getKey(mat2.getItem()).toString();
        return cursedItems.contains(id1) && cursedItems.contains(id2) && !id1.equals(id2);
    }

    private static double getTierBonus(MaterialTier tier) {
        String key = tier.name().toLowerCase(java.util.Locale.ROOT);
        return tierBonuses.getOrDefault(key, 0.0);
    }
}
