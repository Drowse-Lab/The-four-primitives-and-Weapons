package the_four_primitives_and_weapons.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import the_four_primitives_and_weapons.init.CustomEntityInit;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.init.RarityForgeRegistration;
import the_four_primitives_and_weapons.item.rarity.RarityForgeNewRecipes;
import the_four_primitives_and_weapons.item.rarity.RarityForgeRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

@JeiPlugin
public class RarityForgeJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("the_four_primitives_and_weapons", "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new RarityForgeRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(
                new RarityForgeLegacyRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RarityForgeRecipeCategory.RECIPE_TYPE, RarityForgeNewRecipes.build());
        // 旧 rarity_forge_recipes/*.json を JEI に表示
        registration.addRecipes(RarityForgeLegacyRecipeCategory.RECIPE_TYPE, RarityForgeRecipes.getAll());

        // 鞘クラフトレシピをJEIに登録
        registerSayaCraftingRecipes(registration);

        // JEI 情報パネル: アイテム選択時に操作/効果を説明表示
        registerKnifeItemInfo(registration);
    }

    // ナイフ系・ガイドブックに "info" パネルを付与。
    // vanilla の crafting レシピ (data配下のrecipesフォルダに置いた json) は
    // JEI が自動で拾うのでレシピ登録は不要。ここでは使用方法の説明文のみ。
    private void registerKnifeItemInfo(IRecipeRegistration registration) {
        registration.addIngredientInfo(
            new ItemStack(CustomEntityInit.KNIFE_LAUNCHER.get()),
            VanillaTypes.ITEM_STACK,
            Component.literal("§6ナイフホルダー"),
            Component.literal("右クリックで扇形に投擲。"),
            Component.literal("Shift+左クリックで技選択画面を開く。"),
            Component.literal("モードと本数 (1〜10) を設定できる。"),
            Component.literal("§c本数が多いほどブレが大きくなる。"),
            Component.literal("弾は各種ナイフをインベントリから消費。")
        );
        registration.addIngredientInfo(
            new ItemStack(CustomEntityInit.GUIDE_BOOK.get()),
            VanillaTypes.ITEM_STACK,
            Component.literal("§6始まりのガイドブック"),
            Component.literal("右クリックでガイド画面を開く。"),
            Component.literal("操作方法・ナイフの技・Mob の情報を"),
            Component.literal("画像付きで確認できる。"),
            Component.literal("ワールド初回参加時に自動支給。")
        );
        registration.addIngredientInfo(
            new ItemStack(CustomEntityInit.THROWING_KNIFE.get()),
            VanillaTypes.ITEM_STACK,
            Component.literal("§f通常ナイフ"),
            Component.literal("右クリックで投擲、左クリックで近接攻撃。"),
            Component.literal("敵に当てれば消滅、ブロックに刺さる。"),
            Component.literal("MP 消費なし / クールダウン 8t。")
        );
        registration.addIngredientInfo(
            new ItemStack(CustomEntityInit.STUN_KNIFE.get()),
            VanillaTypes.ITEM_STACK,
            Component.literal("§eスタンナイフ"),
            Component.literal("命中で感電: 移動低下 + 弱体化。"),
            Component.literal("MP 25/本 / クールダウン 16t。")
        );
        registration.addIngredientInfo(
            new ItemStack(CustomEntityInit.SCREW_KNIFE.get()),
            VanillaTypes.ITEM_STACK,
            Component.literal("§bスクリューナイフ"),
            Component.literal("木材/葉を破壊できる。"),
            Component.literal("葉は素手扱い → 苗木/棒が低確率。"),
            Component.literal("木に当たると消滅、葉は貫通継続。"),
            Component.literal("MP 10/本 / クールダウン 10t。")
        );

        // Re:Cross Hookshot Long
        registration.addIngredientInfo(
            new ItemStack(CustomEntityInit.RECROSS_HOOKSHOT_LONG.get()),
            VanillaTypes.ITEM_STACK,
            Component.literal("§6Re:Cross Hookshot (Long)"),
            Component.literal("§f射程: 80 ブロック"),
            Component.literal("右クリック押し → 離して発射。"),
            Component.literal("地形に当てると着弾点まで自動移動。"),
            Component.literal("エンティティ/アイテムに当てると引き寄せ。"),
            Component.literal("§cガスト/ジャイアント/ウィザー/エンドラ等"),
            Component.literal("§cヘヴィ系に当てると逆に自分が引っ張られる。"),
            Component.literal("引き寄せ中に Sneak でキャンセル。"),
            Component.literal("空中で Sneak 押下中は浮遊 (燃料 40t、着地リセット)。")
        );

        // Re:Cross Hookshot Short
        registration.addIngredientInfo(
            new ItemStack(CustomEntityInit.RECROSS_HOOKSHOT_SHORT.get()),
            VanillaTypes.ITEM_STACK,
            Component.literal("§6Re:Cross Hookshot (Short)"),
            Component.literal("§f射程: 60 ブロック"),
            Component.literal("操作・特性は Long と同じ。"),
            Component.literal("射程と引き換えにレシピが安価。")
        );
    }

    private void registerSayaCraftingRecipes(IRecipeRegistration registration) {
        // 霊刀スタイル鞘: 紫染料 + 鞘 + 糸
        {
            ItemStack result = new ItemStack(TheFourPrimitivesAndWeaponsModItems.SAYA.get());
            CompoundTag tag = new CompoundTag();
            tag.putString("SayaStyle", "reitou");
            result.setTag(tag);

            NonNullList<Ingredient> inputs = NonNullList.of(Ingredient.EMPTY,
                    Ingredient.of(TheFourPrimitivesAndWeaponsModItems.SAYA.get()),
                    Ingredient.of(Items.PURPLE_DYE),
                    Ingredient.of(Items.STRING));

            ShapelessRecipe recipe = new ShapelessRecipe(
                    new ResourceLocation("the_four_primitives_and_weapons", "reitou_saya_jei"),
                    "", CraftingBookCategory.MISC, result, inputs);
            registration.addRecipes(RecipeTypes.CRAFTING, List.of(recipe));
        }

        // 封印鞘: 鞘 + お札 + 糸
        {
            ItemStack result = new ItemStack(TheFourPrimitivesAndWeaponsModItems.SAYA.get());
            CompoundTag tag = new CompoundTag();
            tag.putString("Feyn", "sigiled");
            result.setTag(tag);

            NonNullList<Ingredient> inputs = NonNullList.of(Ingredient.EMPTY,
                    Ingredient.of(TheFourPrimitivesAndWeaponsModItems.SAYA.get()),
                    Ingredient.of(TheFourPrimitivesAndWeaponsModItems.OFUDA.get()),
                    Ingredient.of(Items.STRING));

            ShapelessRecipe recipe = new ShapelessRecipe(
                    new ResourceLocation("the_four_primitives_and_weapons", "reitou_sigiled_saya_jei"),
                    "", CraftingBookCategory.MISC, result, inputs);
            registration.addRecipes(RecipeTypes.CRAFTING, List.of(recipe));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(RarityForgeRegistration.getBlock()),
                RarityForgeRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(RarityForgeRegistration.getBlock()),
                RarityForgeLegacyRecipeCategory.RECIPE_TYPE);
        // バニラ crafting カテゴリでも RarityForge ブロックを触媒として表示
        // ( = レアリティ作業台でもバニラ crafting レシピが作れるとユーザに伝える )
        registration.addRecipeCatalyst(
                new ItemStack(RarityForgeRegistration.getBlock()),
                mezz.jei.api.constants.RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeTransferHandlers(
            mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        // JEI レシピの「+」 ボタンで グリッドに素材を自動転送
        registration.addRecipeTransferHandler(
                new RarityForgeRecipeTransferInfo());
    }

    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        // 結晶ポーチのフローティングパネル領域を JEI の占有域として登録
        // ( パネル越しに背後のアイテム名が出るのを防ぐ )
        registration.addGlobalGuiHandler(new PouchPanelGuiHandler());
    }
}
