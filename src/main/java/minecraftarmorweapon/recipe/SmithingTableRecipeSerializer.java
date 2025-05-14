package minecraftarmorweapon.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.Registry; // Registry をインポート
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SmithingTableRecipeSerializer implements RecipeSerializer<SmithingTableRecipe> {

    @Override
    public SmithingTableRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        Ingredient input = Ingredient.fromJson(json.get("input"));
        Ingredient addition = Ingredient.fromJson(json.get("addition"));

        JsonObject resultJson = json.getAsJsonObject("result");
        ItemStack result = new ItemStack(
            Registry.ITEM.get(new ResourceLocation(resultJson.get("item").getAsString())), // Registry を使用
            resultJson.get("count").getAsInt()
        );

        return new SmithingTableRecipe(recipeId, input, addition, result);
    }

    @Override
    public SmithingTableRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        Ingredient input = Ingredient.fromNetwork(buffer);
        Ingredient addition = Ingredient.fromNetwork(buffer);
        ItemStack result = buffer.readItem();

        return new SmithingTableRecipe(recipeId, input, addition, result);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, SmithingTableRecipe recipe) {
        recipe.getInput().toNetwork(buffer);
        recipe.getAddition().toNetwork(buffer);
        buffer.writeItem(recipe.getResultItem());
    }
}