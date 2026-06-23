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
import the_four_primitives_and_weapons.item.rarity.RarityForgeNewRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Hybrid レアリティ解放テーブル ( 強化モード ) の JEI カテゴリ。
 *
 * 表示:
 *   [媒体]   [触媒]    →    [結果]
 *
 * クラフトモード ( バニラレシピ + 触媒 ) は バニラ crafting カテゴリで
 * 自動表示される。 ここでは強化モード + Unbreakable 説明のみ。
 */
public class RarityForgeRecipeCategory implements IRecipeCategory<RarityForgeNewRecipe> {

    public static final ResourceLocation UID = new ResourceLocation("the_four_primitives_and_weapons", "rarity_forge");
    public static final RecipeType<RarityForgeNewRecipe> RECIPE_TYPE =
            RecipeType.create("the_four_primitives_and_weapons", "rarity_forge", RarityForgeNewRecipe.class);

    private static final int MEDIUM_X  = 6;
    private static final int CAT_X     = 30;
    private static final int ARROW_X   = 56;
    private static final int RESULT_X  = 76;
    private static final int SLOT_Y    = 22;
    private static final int BG_WIDTH  = 102;
    private static final int BG_HEIGHT = 60;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public RarityForgeRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(RarityForgeRegistration.getBlock()));
        this.title = Component.translatable("jei.the_four_primitives_and_weapons.rarity_forge.enhance");
    }

    @Override
    public RecipeType<RarityForgeNewRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, RarityForgeNewRecipe recipe, IFocusGroup focuses) {
        // 媒体 = cat0Candidates, 触媒 = cat1Candidates ( 新 hybrid 仕様 )
        if (!recipe.getCat0Candidates().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, MEDIUM_X + 1, SLOT_Y + 1)
                    .addItemStacks(recipe.getCat0Candidates());
        }
        if (!recipe.getCat1Candidates().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, CAT_X + 1, SLOT_Y + 1)
                    .addItemStacks(recipe.getCat1Candidates());
        }
        if (!recipe.getOutputCandidates().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, SLOT_Y + 1)
                    .addItemStacks(recipe.getOutputCandidates());
        }
    }

    @Override
    public void draw(RarityForgeNewRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics gfx, double mouseX, double mouseY) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        String mode = switch (recipe.getKind()) {
            case BOOK_ELEMENT -> "§b" + recipe.getDescription();
            case UNBREAKABLE  -> "§6" + recipe.getDescription();
            case RARITY       -> "§d" + recipe.getDescription();
        };
        gfx.drawString(mc.font, mode, 2, 4, 0xFFFFFF, false);

        // ラベル
        gfx.drawString(mc.font, "§7媒体", MEDIUM_X, SLOT_Y - 9, 0x404040, false);
        gfx.drawString(mc.font, "§7触媒", CAT_X, SLOT_Y - 9, 0x404040, false);
        gfx.drawString(mc.font, "§7結果", RESULT_X, SLOT_Y - 9, 0x404040, false);

        // スロット背景
        drawSlotBg(gfx, MEDIUM_X, SLOT_Y);
        drawSlotBg(gfx, CAT_X, SLOT_Y);
        drawSlotBg(gfx, RESULT_X, SLOT_Y);

        // 矢印
        gfx.drawString(mc.font, "→", ARROW_X, SLOT_Y + 5, 0x404040, false);
    }

    private static void drawSlotBg(GuiGraphics gfx, int sx, int sy) {
        gfx.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
        gfx.fill(sx, sy, sx + 18, sy + 1, 0xFF373737);
        gfx.fill(sx, sy, sx + 1, sy + 18, 0xFF373737);
        gfx.fill(sx + 17, sy, sx + 18, sy + 18, 0xFFFFFFFF);
        gfx.fill(sx, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
    }
}
