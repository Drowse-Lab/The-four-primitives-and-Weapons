package the_four_primitives_and_weapons.item.rarity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.ForgeRegistries;

import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * JEI 表示用レシピ list を、 datapack で load された RarityForgeCenterLogic の
 * 各 table から動的に組み立てる。
 *
 * 呼ばれるタイミング: JEI の registerRecipes ( 起動直後 )。
 * datapack を /reload で書き換えても JEI 表示は変わらないが、
 * リスタートすれば反映される。
 */
public final class RarityForgeNewRecipes {

    private RarityForgeNewRecipes() {}

    public static List<RarityForgeNewRecipe> build() {
        List<RarityForgeNewRecipe> list = new ArrayList<>();

        // === 触媒 candidates ( Lv 昇順 ) ===
        List<ItemStack> levelCatalysts = new ArrayList<>();
        RarityForgeCenterLogic.getCatalystLevelTable().entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .forEach(e -> {
                    Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(e.getKey()));
                    if (it == null || it == Items.AIR) return;
                    ItemStack st = new ItemStack(it);
                    st.setHoverName(Component.literal(
                            it.getDescription().getString() + " → Lv. " + e.getValue()));
                    levelCatalysts.add(st);
                });

        // === 1) BOOK_ELEMENT — 魔導書ごとに 1 レシピ ===
        RarityForgeCenterLogic.getBookElementTable().forEach((id, element) -> {
            Item book = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            if (book == null || book == Items.AIR) return;
            ItemStack centerStack = new ItemStack(book);

            List<ItemStack> outputs = new ArrayList<>();
            RarityForgeCenterLogic.getCatalystLevelTable().entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getValue))
                    .forEach(e -> {
                        ItemStack out = new ItemStack(book);
                        ElementalDamageUtils.setElement(out, element, e.getValue());
                        out.setHoverName(Component.literal(
                                book.getDescription().getString() + " Lv. " + e.getValue()));
                        outputs.add(out);
                    });
            list.add(new RarityForgeNewRecipe(
                    RarityForgeNewRecipe.Kind.BOOK_ELEMENT,
                    List.of(centerStack),
                    levelCatalysts,
                    List.of(),
                    outputs,
                    "魔導書 element 付与 — 触媒で Lv 決定"
            ));
        });

        // === 2) UNBREAKABLE — Pair ごとに 1 レシピ ===
        List<ItemStack> swordSamples = collectSwordSamples();
        ItemStack unbreakableSample = swordSamples.isEmpty()
                ? new ItemStack(Items.IRON_SWORD)
                : swordSamples.get(0).copy();
        unbreakableSample.getOrCreateTag().putBoolean("Unbreakable", true);

        for (RarityForgeCenterLogic.UnbreakablePair pair : RarityForgeCenterLogic.getUnbreakablePairs()) {
            Item a = ForgeRegistries.ITEMS.getValue(new ResourceLocation(pair.catA()));
            Item b = ForgeRegistries.ITEMS.getValue(new ResourceLocation(pair.catB()));
            if (a == null || b == null || a == Items.AIR || b == Items.AIR) continue;
            list.add(new RarityForgeNewRecipe(
                    RarityForgeNewRecipe.Kind.UNBREAKABLE,
                    swordSamples,
                    List.of(new ItemStack(a)),
                    List.of(new ItemStack(b)),
                    List.of(unbreakableSample),
                    "Unbreakable 化 — 任意の剣 + 指定触媒 2 個"
            ));
        }

        // === 3) RARITY — 全 SwordItem 対象、 1 レシピ ===
        // 触媒候補 — tier に応じてサンプル ( これは JEI 表示用、 実際は任意のアイテムで OK )
        List<ItemStack> rarityCat = List.of(
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.EMERALD),
                new ItemStack(Items.NETHERITE_INGOT),
                new ItemStack(Items.NETHER_STAR),
                new ItemStack(Items.DRAGON_EGG)
        );
        if (!swordSamples.isEmpty()) {
            list.add(new RarityForgeNewRecipe(
                    RarityForgeNewRecipe.Kind.RARITY,
                    swordSamples,
                    rarityCat,
                    rarityCat,
                    List.of(swordSamples.get(0)),
                    "レアリティ抽選 — 触媒のティアで確率変動"
            ));
        }

        return Collections.unmodifiableList(list);
    }

    /** 登録済みアイテムから SwordItem の sample を全部集める ( JEI ローテ表示用 )。 */
    private static List<ItemStack> collectSwordSamples() {
        List<ItemStack> swords = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof SwordItem) {
                swords.add(new ItemStack(item));
            }
        }
        return swords;
    }
}
