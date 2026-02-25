package minecraftarmorweapon.integration.jei;

import net.minecraft.client.gui.GuiGraphics;
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
        // パターンから素材スロットを配置
        net.minecraft.world.item.Item[][] pattern = recipe.getPattern();
        int slotX = 1;
        for (int y = 0; y < recipe.getPatternHeight(); y++) {
            for (int x = 0; x < recipe.getPatternWidth(); x++) {
                if (pattern[y][x] != null) {
                    builder.addSlot(RecipeIngredientRole.INPUT, slotX, 10)
                            .addItemStack(new ItemStack(pattern[y][x]));
                    slotX += 20;
                }
            }
        }

        // 結果 (右)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 10)
                .addItemStack(new ItemStack(recipe.getResult()));
    }

    @Override
    public void draw(RarityForgeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 矢印を描画 (テキストで代用)
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        guiGraphics.drawString(mc.font, "\u2192", 55, 14, 0x404040);
    }
}
