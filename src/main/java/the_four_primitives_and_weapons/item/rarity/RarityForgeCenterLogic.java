package the_four_primitives_and_weapons.item.rarity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.ForgeRegistries;

import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 新レアリティ解放テーブル ( 4 スロット版 ) のロジック。
 * 中央スロットに置かれたアイテムの種別で挙動が分岐する:
 *   1. 魔導書アイテム  : 触媒 0 のティアで element レベルを決定し、 魔導書に書き込む。
 *   2. SwordItem + Unbreakable 触媒組み合わせ : Unbreakable 化。
 *   3. それ以外 ( 武器 ) : 触媒 2 個で rarity を抽選 ( RarityCraftingLogic 流用 )。
 *
 * 各 table は datapack の JSON ( data/<ns>/rarity_forge/ ) から
 * RarityForgeDataReloader 経由で上書き可能。 デフォルト値はこのクラス内 fallback。
 */
public final class RarityForgeCenterLogic {

    private RarityForgeCenterLogic() {}

    public enum Mode {
        NONE,
        BOOK_ELEMENT,
        ELEMENT_FUSION,
        UNBREAKABLE,
        RARITY
    }

    /**
     * 強化モード:
     *   cat0 = 媒体 ( 強化対象 )、 cat1 = 触媒
     *   - 媒体 = 魔導書 + 触媒で element level → BOOK_ELEMENT
     *   - 媒体 = 武器 ( + 触媒 ) → RARITY
     * 媒体スロットが武器/魔導書でなければ NONE。
     */
    public static Mode resolveEnhanceMode(ItemStack medium, ItemStack catalyst) {
        if (medium.isEmpty() || catalyst.isEmpty()) return Mode.NONE;
        if (getFusionElement(medium, catalyst) != ElementType.NONE) return Mode.ELEMENT_FUSION;
        if (isBookItem(medium)) {
            if (getCatalystLevel(catalyst) > 0) return Mode.BOOK_ELEMENT;
            return Mode.NONE;
        }
        // 武器 ( SwordItem 含む任意 Item ) → rarity 抽選
        return Mode.RARITY;
    }

    public static ItemStack buildEnhancePreview(ItemStack medium, ItemStack catalyst) {
        Mode mode = resolveEnhanceMode(medium, catalyst);
        switch (mode) {
            case ELEMENT_FUSION:
                return buildFusionPreview(medium, catalyst, ItemStack.EMPTY);
            case BOOK_ELEMENT: {
                ElementType type = getBookElement(medium);
                int lvl = getCatalystLevel(catalyst);
                if (lvl <= 0) return ItemStack.EMPTY;
                ItemStack out = medium.copy();
                out.setCount(1);
                ElementalDamageUtils.setElement(out, type, lvl);
                return out;
            }
            case RARITY: {
                // preview はベース表示、 rarity は取り出し時に確定
                ItemStack out = medium.copy();
                out.setCount(1);
                return out;
            }
            default:
                return ItemStack.EMPTY;
        }
    }

    public static ItemStack finalizeEnhance(ItemStack medium, ItemStack catalyst) {
        Mode mode = resolveEnhanceMode(medium, catalyst);
        if (mode == Mode.RARITY) {
            ItemStack out = medium.copy();
            out.setCount(1);
            WeaponRarity rarity = RarityCraftingLogic.rollRarity(catalyst, ItemStack.EMPTY);
            double bonus = RarityCraftingLogic.getCatalystBonus(catalyst, ItemStack.EMPTY);
            WeaponRarity.setToStack(out, rarity);
            WeaponRarity.setCatalystBonus(out, bonus);
            return out;
        }
        return buildEnhancePreview(medium, catalyst);
    }

    /** Unbreakable 化条件 ( cat0/cat1 のペア、 順不同 ) */
    public record UnbreakablePair(String catA, String catB) {
        public boolean matches(String id0, String id1) {
            return (catA.equals(id0) && catB.equals(id1))
                || (catA.equals(id1) && catB.equals(id0));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // データ ( datapack で上書き可能 )
    // ─────────────────────────────────────────────────────────────

    private static Map<String, Integer> catalystLevels = defaultCatalystLevels();
    private static Map<String, ElementType> bookElements = defaultBookElements();
    private static List<UnbreakablePair> unbreakablePairs = defaultUnbreakablePairs();

    private static Map<String, Integer> defaultCatalystLevels() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("minecraft:oak_planks",      1);
        m.put("minecraft:stone",           2);
        m.put("minecraft:iron_ingot",      3);
        m.put("minecraft:gold_ingot",      4);
        m.put("minecraft:diamond",         5);
        m.put("minecraft:emerald",         6);
        m.put("minecraft:blaze_rod",       7);
        m.put("minecraft:netherite_scrap", 8);
        m.put("minecraft:nether_star",     9);
        m.put("minecraft:beacon",         10);
        return m;
    }

    private static Map<String, ElementType> defaultBookElements() {
        Map<String, ElementType> m = new LinkedHashMap<>();
        m.put("the_four_primitives_and_weapons:ice_book",       ElementType.ICE);
        m.put("the_four_primitives_and_weapons:electric_book",  ElementType.ELECTRIC);
        m.put("the_four_primitives_and_weapons:corrosion_book", ElementType.CORROSION);
        m.put("the_four_primitives_and_weapons:holy_book",      ElementType.HOLY);
        m.put("the_four_primitives_and_weapons:miasma_book",    ElementType.MIASMA);
        m.put("the_four_primitives_and_weapons:bubbles_book",   ElementType.WATER);
        m.put("the_four_primitives_and_weapons:fire_book",      ElementType.FIRE);
        m.put("the_four_primitives_and_weapons:thunder_book",   ElementType.THUNDER);
        m.put("the_four_primitives_and_weapons:wind_book",      ElementType.WIND);
        m.put("the_four_primitives_and_weapons:storm_book",     ElementType.THUNDER);
        m.put("the_four_primitives_and_weapons:darkness_book",  ElementType.DARK);
        return m;
    }

    private static List<UnbreakablePair> defaultUnbreakablePairs() {
        return new ArrayList<>(List.of(new UnbreakablePair(
                "minecraft:enchanted_golden_apple",
                "the_four_primitives_and_weapons:immortal_core")));
    }

    // ─────────────────────────────────────────────────────────────
    // datapack reloader 用 setter
    // ─────────────────────────────────────────────────────────────

    public static void setCatalystLevels(Map<String, Integer> map) {
        catalystLevels = (map == null || map.isEmpty()) ? defaultCatalystLevels() : new LinkedHashMap<>(map);
    }

    public static void setBookElements(Map<String, ElementType> map) {
        bookElements = (map == null || map.isEmpty()) ? defaultBookElements() : new LinkedHashMap<>(map);
    }

    public static void setUnbreakablePairs(List<UnbreakablePair> pairs) {
        unbreakablePairs = (pairs == null || pairs.isEmpty()) ? defaultUnbreakablePairs() : new ArrayList<>(pairs);
    }

    public static Map<String, Integer> getCatalystLevelTable() {
        return Collections.unmodifiableMap(catalystLevels);
    }

    public static Map<String, ElementType> getBookElementTable() {
        return Collections.unmodifiableMap(bookElements);
    }

    public static List<UnbreakablePair> getUnbreakablePairs() {
        return Collections.unmodifiableList(unbreakablePairs);
    }

    // ─────────────────────────────────────────────────────────────
    // 判定 API
    // ─────────────────────────────────────────────────────────────

    public static ElementType getBookElement(ItemStack center) {
        if (center.isEmpty()) return ElementType.NONE;
        Item item = center.getItem();
        String id = ForgeRegistries.ITEMS.getKey(item).toString();
        ElementType fromTable = bookElements.getOrDefault(id, ElementType.NONE);
        if (fromTable != ElementType.NONE) return fromTable;
        return ElementalDamageUtils.getBookElementFromItemStack(center);
    }

    public static boolean isBookItem(ItemStack center) {
        return getBookElement(center) != ElementType.NONE;
    }

    public static int getCatalystLevel(ItemStack catalyst) {
        if (catalyst.isEmpty()) return 0;
        String id = ForgeRegistries.ITEMS.getKey(catalyst.getItem()).toString();
        return catalystLevels.getOrDefault(id, 0);
    }

    public static boolean isUnbreakableCatalystPair(ItemStack cat0, ItemStack cat1) {
        if (cat0.isEmpty() || cat1.isEmpty()) return false;
        String id0 = ForgeRegistries.ITEMS.getKey(cat0.getItem()).toString();
        String id1 = ForgeRegistries.ITEMS.getKey(cat1.getItem()).toString();
        for (UnbreakablePair p : unbreakablePairs) {
            if (p.matches(id0, id1)) return true;
        }
        return false;
    }

    public static Mode resolveMode(ItemStack center, ItemStack cat0, ItemStack cat1) {
        if (center.isEmpty()) return Mode.NONE;
        if (getFusionElement(center, cat0) != ElementType.NONE
                || getFusionElement(center, cat1) != ElementType.NONE) {
            return Mode.ELEMENT_FUSION;
        }
        if (isBookItem(center)) {
            if (getCatalystLevel(cat0) > 0 || getCatalystLevel(cat1) > 0) {
                return Mode.BOOK_ELEMENT;
            }
            return Mode.NONE;
        }
        if (center.getItem() instanceof SwordItem
                && isUnbreakableCatalystPair(cat0, cat1)) {
            return Mode.UNBREAKABLE;
        }
        if (!cat0.isEmpty() || !cat1.isEmpty()) {
            return Mode.RARITY;
        }
        return Mode.NONE;
    }

    public static ItemStack buildPreview(ItemStack center, ItemStack cat0, ItemStack cat1) {
        Mode mode = resolveMode(center, cat0, cat1);
        switch (mode) {
            case ELEMENT_FUSION:
                return buildFusionPreview(center, cat0, cat1);
            case BOOK_ELEMENT: {
                ElementType type = getBookElement(center);
                int lvl = Math.max(getCatalystLevel(cat0), getCatalystLevel(cat1));
                if (lvl <= 0) return ItemStack.EMPTY;
                ItemStack out = center.copy();
                out.setCount(1);
                ElementalDamageUtils.setElement(out, type, lvl);
                return out;
            }
            case UNBREAKABLE: {
                ItemStack out = center.copy();
                out.setCount(1);
                out.getOrCreateTag().putBoolean("Unbreakable", true);
                return out;
            }
            case RARITY: {
                ItemStack out = center.copy();
                out.setCount(1);
                return out;
            }
            case NONE:
            default:
                return ItemStack.EMPTY;
        }
    }

    public static ItemStack finalize(ItemStack center, ItemStack cat0, ItemStack cat1) {
        Mode mode = resolveMode(center, cat0, cat1);
        if (mode == Mode.RARITY) {
            ItemStack out = center.copy();
            out.setCount(1);
            WeaponRarity rarity = RarityCraftingLogic.rollRarity(cat0, cat1);
            double bonus = RarityCraftingLogic.getCatalystBonus(cat0, cat1);
            WeaponRarity.setToStack(out, rarity);
            WeaponRarity.setCatalystBonus(out, bonus);
            return out;
        }
        return buildPreview(center, cat0, cat1);
    }

    private static ItemStack buildFusionPreview(ItemStack center, ItemStack cat0, ItemStack cat1) {
        ItemStack partner = ItemStack.EMPTY;
        ElementType fusion = getFusionElement(center, cat0);
        if (fusion != ElementType.NONE) {
            partner = cat0;
        } else {
            fusion = getFusionElement(center, cat1);
            if (fusion != ElementType.NONE) {
                partner = cat1;
            }
        }
        if (fusion == ElementType.NONE || partner.isEmpty()) return ItemStack.EMPTY;

        int lvl = Math.max(getStackElementLevel(center), getStackElementLevel(partner));
        if (lvl <= 0) lvl = 1;

        ItemStack out = center.copy();
        out.setCount(1);
        ElementalDamageUtils.setElement(out, fusion, lvl);
        return out;
    }

    private static ElementType getFusionElement(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return ElementType.NONE;
        ElementType left = getStackElement(a);
        ElementType right = getStackElement(b);
        if (isFireSoulPair(left, right)) return ElementType.SOUL_FIRE;
        return ElementType.NONE;
    }

    private static boolean isFireSoulPair(ElementType left, ElementType right) {
        return (left == ElementType.FIRE && right == ElementType.SOUL)
                || (left == ElementType.SOUL && right == ElementType.FIRE);
    }

    private static ElementType getStackElement(ItemStack stack) {
        if (stack.isEmpty()) return ElementType.NONE;
        ElementType explicit = ElementalDamageUtils.getElementType(stack);
        if (explicit != ElementType.NONE) return explicit;
        return getBookElement(stack);
    }

    private static int getStackElementLevel(ItemStack stack) {
        int level = ElementalDamageUtils.getElementLevel(stack);
        if (level > 0) return level;
        return getStackElement(stack) != ElementType.NONE ? 1 : 0;
    }
}
