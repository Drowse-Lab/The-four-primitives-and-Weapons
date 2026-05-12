package the_four_primitives_and_weapons.integration.jei;

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
import the_four_primitives_and_weapons.init.RarityForgeRegistration;
import the_four_primitives_and_weapons.item.rarity.RarityForgeRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RarityForgeRecipeCategory implements IRecipeCategory<RarityForgeRecipe> {

    public static final ResourceLocation UID = new ResourceLocation("the_four_primitives_and_weapons", "rarity_forge");
    public static final RecipeType<RarityForgeRecipe> RECIPE_TYPE =
            RecipeType.create("the_four_primitives_and_weapons", "rarity_forge", RarityForgeRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public RarityForgeRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(116, 54);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(RarityForgeRegistration.getBlock()));
        this.title = Component.literal("\u30EC\u30A2\u30EA\u30C6\u30A3\u89E3\u653E\u30C6\u30FC\u30D6\u30EB");
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
        Item[][] pattern = recipe.getPattern();
        int pw = recipe.getPatternWidth();
        int ph = recipe.getPatternHeight();

        // 3×3グリッドの中央寄せオフセット（セル単位にスナップ）
        int offsetX = ((3 - pw) / 2) * 18;
        int offsetY = ((3 - ph) / 2) * 18;

        int inputSlotIdx = recipe.getInputSlotIndex();

        for (int y = 0; y < ph; y++) {
            for (int x = 0; x < pw; x++) {
                int patIdx = y * pw + x;
                if (pattern[y][x] != null) {
                    builder.addSlot(RecipeIngredientRole.INPUT,
                                    1 + offsetX + x * 18,
                                    1 + offsetY + y * 18)
                            .addItemStack(new ItemStack(pattern[y][x]));
                } else if (recipe.isUnbreakable() && patIdx == inputSlotIdx) {
                    // #inputスロット: 鉄の剣をサンプルとして表示
                    builder.addSlot(RecipeIngredientRole.INPUT,
                                    1 + offsetX + x * 18,
                                    1 + offsetY + y * 18)
                            .addItemStack(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
                }
            }
        }

        // 結果スロット
        if (recipe.isUnbreakable()) {
            // Unbreakableレシピ: 結果に Unbreakable:1 を表示
            ItemStack resultDisplay = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            resultDisplay.getOrCreateTag().putBoolean("Unbreakable", true);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 19)
                    .addItemStack(resultDisplay);
        } else {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 19)
                    .addItemStack(new ItemStack(recipe.getResult()));
        }
    }

    @Override
    public void draw(RarityForgeRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics gfx, double mouseX, double mouseY) {
        // 3×3グリッド背景
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int sx = c * 18;
                int sy = r * 18;
                gfx.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
                gfx.fill(sx, sy, sx + 18, sy + 1, 0xFF373737);
                gfx.fill(sx, sy, sx + 1, sy + 18, 0xFF373737);
                gfx.fill(sx + 17, sy, sx + 18, sy + 18, 0xFFFFFFFF);
                gfx.fill(sx, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
            }
        }

        // 結果スロット背景
        int ox = 94, oy = 18;
        gfx.fill(ox, oy, ox + 18, oy + 18, 0xFF8B8B8B);
        gfx.fill(ox, oy, ox + 18, oy + 1, 0xFF373737);
        gfx.fill(ox, oy, ox + 1, oy + 18, 0xFF373737);
        gfx.fill(ox + 17, oy, ox + 18, oy + 18, 0xFFFFFFFF);
        gfx.fill(ox, oy + 17, ox + 18, oy + 18, 0xFFFFFFFF);

        // 矢印
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        gfx.drawString(mc.font, "\u2192", 62, 22, 0x404040, false);
    }
}
