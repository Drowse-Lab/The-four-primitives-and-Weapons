package minecraftarmorweapon.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.util.GsonHelper;

public class SmithingTableRecipeSerializer implements RecipeSerializer<SmithingTableRecipe> {

    @Override
    public SmithingTableRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        // 入力アイテムを読み取る
        Ingredient input = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "input"));

        // 追加アイテムを読み取る
        Ingredient addition = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "addition"));

        // 結果アイテムを読み取る
        if (!json.has("result")) {
            throw new JsonSyntaxException("Missing result field for smithing recipe");
        }
        ItemStack result = ItemStack.of(GsonHelper.getAsJsonObject(json, "result"));

        // レシピオブジェクトを生成して返す
        return new SmithingTableRecipe(recipeId, input, addition, result);
    }

    @Override
    public SmithingTableRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        // ネットワークデータを読み取る
        Ingredient input = Ingredient.fromNetwork(buffer);
        Ingredient addition = Ingredient.fromNetwork(buffer);
        ItemStack result = buffer.readItem();

        // レシピオブジェクトを生成して返す
        return new SmithingTableRecipe(recipeId, input, addition, result);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, SmithingTableRecipe recipe) {
        // ネットワークデータに書き込む
        recipe.getInput().toNetwork(buffer);
        recipe.getAddition().toNetwork(buffer);
        buffer.writeItem(recipe.getResultItem());
    }
}
