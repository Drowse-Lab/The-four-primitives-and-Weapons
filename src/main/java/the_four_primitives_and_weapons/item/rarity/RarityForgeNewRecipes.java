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
 * JEI 表示用レシピ list — 強化モード ( 媒体 + 触媒 → 結果 ) を表示する。
 *
 * クラフトモード ( 3×3 グリッド + 触媒 ) のレシピはバニラ CraftingRecipe
 * そのままなので JEI のバニラカテゴリで自動表示される。 ここでは強化のみ。
 */
public final class RarityForgeNewRecipes {

    private RarityForgeNewRecipes() {}

    public static List<RarityForgeNewRecipe> build() {
        List<RarityForgeNewRecipe> list = new ArrayList<>();

        // 触媒 candidates ( Lv 昇順 )
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

        // === 1) 強化 BOOK_ELEMENT — 魔導書ごとに 1 レシピ ===
        // 媒体 = 魔導書 ( cat0 )、 触媒 = level table ( cat1 )
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
            // cat0 = 媒体 ( 魔導書 ), cat1 = 触媒 ( level table )
            list.add(new RarityForgeNewRecipe(
                    RarityForgeNewRecipe.Kind.BOOK_ELEMENT,
                    List.of(),                   // center は使わない
                    List.of(centerStack),        // cat0 = 媒体 ( 魔導書 )
                    levelCatalysts,              // cat1 = 触媒 ( level table )
                    outputs,
                    "強化: 魔導書 + 触媒 → element 付与"
            ));
        });

        // === 2) 強化 RARITY — SwordItem サンプルで 1 レシピ ===
        List<ItemStack> swordSamples = collectSwordSamples();
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
                    List.of(),                   // center 不使用
                    swordSamples,                // cat0 = 媒体 ( 武器 )
                    rarityCat,                   // cat1 = 触媒
                    List.of(swordSamples.get(0)),
                    "強化: 武器 + 触媒 → レアリティ抽選"
            ));
        }

        // === 3) クラフトモード Unbreakable — 任意の剣 + 特定触媒 2 個 ===
        // クラフトモードはグリッドにも素材が必要なので、 ここでは「触媒 2 個で Unbreakable 化」
        // ということだけ説明的に表示する。
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
                    swordSamples,                // 「media」表示用 ( クラフト結果が剣の場合 )
                    List.of(new ItemStack(a)),
                    List.of(new ItemStack(b)),
                    List.of(unbreakableSample),
                    "クラフト時 触媒 2 個で Unbreakable 化 ( グリッドで剣を作成 )"
            ));
        }

        return Collections.unmodifiableList(list);
    }

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
