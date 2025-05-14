package minecraftarmorweapon.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import minecraftarmorweapon.init.ModRecipes;

public class SmithingTableRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final Ingredient addition;
    private final ItemStack result;

    public SmithingTableRecipe(ResourceLocation id, Ingredient input, Ingredient addition, ItemStack result) {
        this.id = id;
        this.input = input;
        this.addition = addition;
        this.result = result;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return input.test(container.getItem(0)) && addition.test(container.getItem(1));
    }

    @Override
    public ItemStack assemble(Container container) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
    }

    public Ingredient getInput() {
        return input;
    }

    public Ingredient getAddition() {
        return addition;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
public RecipeSerializer<?> getSerializer() {
    return ModRecipes.SMITHING_RECIPE_SERIALIZER.get(); // .get() を追加
}

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SMITHING_RECIPE_TYPE;
    }
}