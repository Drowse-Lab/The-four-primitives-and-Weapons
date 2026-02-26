package minecraftarmorweapon.item.rarity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Random;
import java.util.Set;

/**
 * レアリティ強化のクラフトロジック
 * 素材に応じてレアリティの確率が変化する
 */
public final class RarityCraftingLogic {

    private static final Random RANDOM = new Random();

    private RarityCraftingLogic() {}

    /** 素材ティア */
    public enum MaterialTier {
        NONE,         // 素材なし
        DIAMOND,      // ダイヤ系
        NETHER_STAR   // ネザースター系
    }

    // 確率テーブル（Common, Uncommon, Rare, Epic, Legendary）
    // レベル0: 触媒なし → 必ずCommon
    private static final int[] WEIGHTS_NONE        = { 1, 0, 0, 0, 0 };
    // レベル1: ダイヤ系×1
    private static final int[] WEIGHTS_DIAMOND     = { 20, 35, 30, 12, 3 };
    // レベル2: ネザースター×1 or ダイヤ系×2
    private static final int[] WEIGHTS_NETHER_STAR = { 5, 15, 35, 30, 15 };
    // レベル3: ダイヤ系+ネザースター
    private static final int[] WEIGHTS_LEVEL3      = { 2, 8, 25, 40, 25 };
    // レベル4: ネザースター×2
    private static final int[] WEIGHTS_LEVEL4      = { 1, 4, 15, 40, 40 };

    /** レベル別テーブル */
    private static final int[][] WEIGHTS_BY_LEVEL = {
            WEIGHTS_NONE, WEIGHTS_DIAMOND, WEIGHTS_NETHER_STAR, WEIGHTS_LEVEL3, WEIGHTS_LEVEL4
    };

    /** ダイヤ系素材のアイテムID */
    private static final Set<String> DIAMOND_ITEMS = Set.of(
            "minecraft:diamond",
            "minecraft:diamond_block",
            "minecraft:emerald",
            "minecraft:emerald_block",
            "minecraft:amethyst_shard"
    );

    /** ネザースター系素材のアイテムID */
    private static final Set<String> NETHER_STAR_ITEMS = Set.of(
            "minecraft:nether_star",
            "minecraft:dragon_egg",
            "minecraft:dragon_breath",
            "minecraft:totem_of_undying"
    );

    /**
     * 素材アイテムからティアを判定
     */
    public static MaterialTier getMaterialTier(ItemStack material) {
        if (material.isEmpty()) return MaterialTier.NONE;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(material.getItem());
        String id = itemId.toString();

        if (NETHER_STAR_ITEMS.contains(id)) return MaterialTier.NETHER_STAR;
        if (DIAMOND_ITEMS.contains(id)) return MaterialTier.DIAMOND;

        return MaterialTier.NONE;
    }

    /**
     * 素材1つでレアリティを決定
     */
    public static WeaponRarity rollRarity(ItemStack material) {
        return rollRarity(material, ItemStack.EMPTY);
    }

    /**
     * 触媒2つの合算レベルでレアリティを決定
     * NONE=0, DIAMOND=1, NETHER_STAR=2 の合計（0～4）
     */
    public static WeaponRarity rollRarity(ItemStack mat1, ItemStack mat2) {
        int level = getMaterialTier(mat1).ordinal() + getMaterialTier(mat2).ordinal();
        level = Math.min(level, WEIGHTS_BY_LEVEL.length - 1);
        int[] weights = WEIGHTS_BY_LEVEL[level];

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
}
