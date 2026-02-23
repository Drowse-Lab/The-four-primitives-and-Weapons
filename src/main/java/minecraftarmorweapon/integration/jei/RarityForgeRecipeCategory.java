package minecraftarmorweapon.integration.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import minecraftarmorweapon.init.RarityForgeRegistration;
import minecraftarmorweapon.item.rarity.RarityForgeRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class RarityForgeRecipeCategory implements IRecipeCategory<RarityForgeRecipe> {

    public static final ResourceLocation UID = new ResourceLocation("minecraft_armor_weapon", "rarity_forge");
    public static final RecipeType<RarityForgeRecipe> RECIPE_TYPE = RecipeType.create("minecraft_armor_weapon", "rarity_forge", RarityForgeRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public RarityForgeRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(120, 36);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(RarityForgeRegistration.getBlock()));
        this.title = Component.literal("Rarity Forge");
    }

    @Override
    public RecipeType<RarityForgeRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RarityForgeRecipe recipe, IFocusGroup focuses) {
        java.util.List<RarityForgeRecipe.Ingredient> ingredients = recipe.getIngredients();

        // 素材1 (左)
        if (ingredients.size() >= 1) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 10)
                    .addItemStack(ingredients.get(0).toStack());
        }

        // 素材2 (左から2番目)
        if (ingredients.size() >= 2) {
            builder.addSlot(RecipeIngredientRole.INPUT, 25, 10)
                    .addItemStack(ingredients.get(1).toStack());
        }

        // 結果 (右)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 10)
                .addItemStack(new ItemStack(recipe.getResult()));
    }

    @Override
    public void draw(RarityForgeRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
        // 矢印を描画 (テキストで代用)
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        mc.font.draw(stack, "\u2192", 55, 14, 0x404040);
    }
}
