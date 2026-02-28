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

    /** 素材ティア（5段階 + CURSED） */
    public enum MaterialTier {
        NONE,         // 0 - 空スロット
        BASIC,        // 1 - 基本素材（土,石,木材,石炭,骨,革...）
        IRON,         // 2 - 中級素材（鉄,金,銅,ラピス,エンダーパール...）
        DIAMOND,      // 3 - 高級素材（ダイヤ,エメラルド,アメジスト...）
        NETHER_STAR,  // 4 - 超希少素材（ネザースター,ドラゴンエッグ...）
        CURSED        // 5 - 呪い（薔薇・血の瓶）→ 禁忌確定
    }

    // 確率テーブル（Common, Uncommon, Rare, Epic, Legendary）
    // 2つの触媒のordinal合計がレベルになる（max = 4+4 = 8）
    private static final int[][] WEIGHTS_BY_LEVEL = {
            { 100,  0,  0,  0,  0 },  // Level 0: 触媒なし
            {  50, 35, 12,  3,  0 },  // Level 1: BASIC×1
            {  20, 35, 30, 12,  3 },  // Level 2: BASIC×2 or IRON×1
            {  10, 25, 35, 22,  8 },  // Level 3: IRON+BASIC or DIAMOND
            {   5, 15, 35, 30, 15 },  // Level 4: IRON×2 or DIAMOND+BASIC
            {   3, 10, 27, 35, 25 },  // Level 5: DIAMOND+IRON
            {   2,  8, 25, 40, 25 },  // Level 6: DIAMOND×2 or NS+IRON
            {   1,  5, 18, 40, 36 },  // Level 7: NS+DIAMOND
            {   1,  4, 15, 40, 40 },  // Level 8: NS×2
    };

    /** 呪い系素材のアイテムID（薔薇・血の瓶）→ 両方揃えて禁忌確定 */
    private static final String CURSED_ROSE = "minecraft_armor_weapon:rose";
    private static final String CURSED_BLOOD_BOTTLE = "minecraft_armor_weapon:blood_bottle";
    private static final Set<String> CURSED_ITEMS = Set.of(CURSED_ROSE, CURSED_BLOOD_BOTTLE);

    /** ネザースター系素材のアイテムID */
    private static final Set<String> NETHER_STAR_ITEMS = Set.of(
            "minecraft:nether_star",
            "minecraft:dragon_egg",
            "minecraft:dragon_breath",
            "minecraft:totem_of_undying",
            "minecraft:heart_of_the_sea"
    );

    /** ダイヤ系素材のアイテムID */
    private static final Set<String> DIAMOND_ITEMS = Set.of(
            "minecraft:diamond",
            "minecraft:diamond_block",
            "minecraft:emerald",
            "minecraft:emerald_block",
            "minecraft:amethyst_shard",
            "minecraft:netherite_scrap",
            "minecraft:netherite_ingot"
    );

    /** 鉄系素材のアイテムID */
    private static final Set<String> IRON_ITEMS = Set.of(
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
    );

    /**
     * 素材アイテムからティアを判定
     */
    public static MaterialTier getMaterialTier(ItemStack material) {
        if (material.isEmpty()) return MaterialTier.NONE;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(material.getItem());
        String id = itemId.toString();

        // 呪い素材は単体ではBASIC扱い（ペア判定はhasCursedPairで行う）
        if (CURSED_ITEMS.contains(id)) return MaterialTier.BASIC;
        if (NETHER_STAR_ITEMS.contains(id)) return MaterialTier.NETHER_STAR;
        if (DIAMOND_ITEMS.contains(id)) return MaterialTier.DIAMOND;
        if (IRON_ITEMS.contains(id)) return MaterialTier.IRON;

        // それ以外の全アイテム → BASIC
        return MaterialTier.BASIC;
    }

    /**
     * 素材1つでレアリティを決定
     */
    public static WeaponRarity rollRarity(ItemStack material) {
        return rollRarity(material, ItemStack.EMPTY);
    }

    /**
     * 触媒にレアリティ付きアイテムがあればそのレアリティを返す（なければnull）
     */
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

    /**
     * 触媒2つの合算レベルでレアリティを決定
     */
    public static WeaponRarity rollRarity(ItemStack mat1, ItemStack mat2) {
        // 薔薇と血の瓶の両方が揃っていれば禁忌確定
        if (hasCursedPair(mat1, mat2)) {
            return WeaponRarity.FORBIDDEN;
        }

        // 触媒にレアリティ付きアイテムがあれば、そのレアリティをそのまま使用
        WeaponRarity transferred = getTransferredRarity(mat1, mat2);
        if (transferred != null) return transferred;

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

    /**
     * 触媒2つの合算レベルに応じた重みテーブルを返す
     * どちらかがCURSEDの場合はnullを返す（禁忌確定）
     */
    public static int[] getWeightsForCatalysts(ItemStack mat1, ItemStack mat2) {
        if (hasCursedPair(mat1, mat2)) {
            return null;
        }
        int level = getMaterialTier(mat1).ordinal() + getMaterialTier(mat2).ordinal();
        level = Math.min(level, WEIGHTS_BY_LEVEL.length - 1);
        return WEIGHTS_BY_LEVEL[level];
    }

    /**
     * 触媒2つの攻撃力ボーナス合計を返す
     */
    public static double getCatalystBonus(ItemStack mat1, ItemStack mat2) {
        return getTierBonus(getMaterialTier(mat1)) + getTierBonus(getMaterialTier(mat2));
    }

    /**
     * 薔薇と血の瓶が両方揃っているか判定（順不同）
     */
    private static boolean hasCursedPair(ItemStack mat1, ItemStack mat2) {
        if (mat1.isEmpty() || mat2.isEmpty()) return false;
        String id1 = ForgeRegistries.ITEMS.getKey(mat1.getItem()).toString();
        String id2 = ForgeRegistries.ITEMS.getKey(mat2.getItem()).toString();
        return (id1.equals(CURSED_ROSE) && id2.equals(CURSED_BLOOD_BOTTLE))
            || (id1.equals(CURSED_BLOOD_BOTTLE) && id2.equals(CURSED_ROSE));
    }

    private static double getTierBonus(MaterialTier tier) {
        if (tier == MaterialTier.CURSED) return 4;
        if (tier == MaterialTier.NETHER_STAR) return 3;
        if (tier == MaterialTier.DIAMOND) return 2;
        if (tier == MaterialTier.IRON) return 1;
        if (tier == MaterialTier.BASIC) return 0.5;
        return 0;
    }
}
