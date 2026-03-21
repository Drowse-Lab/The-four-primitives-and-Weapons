package minecraftarmorweapon.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.RarityForgeRegistration;
import minecraftarmorweapon.item.rarity.RarityForgeRecipes;
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
        return new ResourceLocation("minecraft_armor_weapon", "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new RarityForgeRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RarityForgeRecipeCategory.RECIPE_TYPE, RarityForgeRecipes.getAll());

        // 鞘クラフトレシピをJEIに登録
        registerSayaCraftingRecipes(registration);
    }

    private void registerSayaCraftingRecipes(IRecipeRegistration registration) {
        // 霊刀スタイル鞘: 紫染料 + 鞘 + 糸
        {
            ItemStack result = new ItemStack(MinecraftArmorWeaponModItems.SAYA.get());
            CompoundTag tag = new CompoundTag();
            tag.putString("SayaStyle", "reitou");
            result.setTag(tag);

            NonNullList<Ingredient> inputs = NonNullList.of(Ingredient.EMPTY,
                    Ingredient.of(MinecraftArmorWeaponModItems.SAYA.get()),
                    Ingredient.of(Items.PURPLE_DYE),
                    Ingredient.of(Items.STRING));

            ShapelessRecipe recipe = new ShapelessRecipe(
                    new ResourceLocation("minecraft_armor_weapon", "reitou_saya_jei"),
                    "", CraftingBookCategory.MISC, result, inputs);
            registration.addRecipes(RecipeTypes.CRAFTING, List.of(recipe));
        }

        // 封印鞘: 鞘 + お札 + 糸
        {
            ItemStack result = new ItemStack(MinecraftArmorWeaponModItems.SAYA.get());
            CompoundTag tag = new CompoundTag();
            tag.putString("Feyn", "sigiled");
            result.setTag(tag);

            NonNullList<Ingredient> inputs = NonNullList.of(Ingredient.EMPTY,
                    Ingredient.of(MinecraftArmorWeaponModItems.SAYA.get()),
                    Ingredient.of(MinecraftArmorWeaponModItems.OFUDA.get()),
                    Ingredient.of(Items.STRING));

            ShapelessRecipe recipe = new ShapelessRecipe(
                    new ResourceLocation("minecraft_armor_weapon", "reitou_sigiled_saya_jei"),
                    "", CraftingBookCategory.MISC, result, inputs);
            registration.addRecipes(RecipeTypes.CRAFTING, List.of(recipe));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(RarityForgeRegistration.getBlock()),
                RarityForgeRecipeCategory.RECIPE_TYPE);
    }
}
