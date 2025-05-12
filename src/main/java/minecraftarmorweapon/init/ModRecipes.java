package minecraftarmorweapon.init;

import minecraftarmorweapon.recipe.SmithingTableRecipe;
import minecraftarmorweapon.recipe.SmithingTableRecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "minecraftarmorweapon");

    public static final RegistryObject<RecipeSerializer<SmithingTableRecipe>> SMITHING_RECIPE_SERIALIZER =
        RECIPE_SERIALIZERS.register("smithing_recipe", SmithingTableRecipeSerializer::new);

    public static final RecipeType<SmithingTableRecipe> SMITHING_RECIPE_TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "minecraftarmorweapon:smithing_recipe";
        }
    };
}
