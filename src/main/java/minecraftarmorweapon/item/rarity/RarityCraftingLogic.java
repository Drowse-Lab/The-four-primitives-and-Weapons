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

    // 確率テーブル（累積: Common, Uncommon, Rare, Epic, Legendary）
    // 素材なし: 50, 30, 15, 4, 1
    private static final int[] WEIGHTS_NONE        = { 50, 30, 15, 4, 1 };
    // ダイヤ系: 20, 35, 30, 12, 3
    private static final int[] WEIGHTS_DIAMOND     = { 20, 35, 30, 12, 3 };
    // ネザースター: 5, 15, 35, 30, 15
    private static final int[] WEIGHTS_NETHER_STAR = { 5, 15, 35, 30, 15 };

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
     * 素材に応じてランダムにレアリティを決定
     */
    public static WeaponRarity rollRarity(ItemStack material) {
        MaterialTier tier = getMaterialTier(material);
        int[] weights = switch (tier) {
            case DIAMOND -> WEIGHTS_DIAMOND;
            case NETHER_STAR -> WEIGHTS_NETHER_STAR;
            default -> WEIGHTS_NONE;
        };

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
