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
 * シンプル化レアリティ解放テーブル ( 新仕様 ) の JEI カテゴリ。
 *
 * 表示:
 *   [触媒0] [触媒1]    [中央]    →   [結果]
 *    +モード説明テキスト
 */
public class RarityForgeRecipeCategory implements IRecipeCategory<RarityForgeNewRecipe> {

    public static final ResourceLocation UID = new ResourceLocation("the_four_primitives_and_weapons", "rarity_forge");
    public static final RecipeType<RarityForgeNewRecipe> RECIPE_TYPE =
            RecipeType.create("the_four_primitives_and_weapons", "rarity_forge", RarityForgeNewRecipe.class);

    // レイアウト ( 上から: モード説明 / スロット行 )
    private static final int CAT0_X   = 4;
    private static final int CAT1_X   = 24;
    private static final int CENTER_X = 58;
    private static final int ARROW_X  = 84;
    private static final int RESULT_X = 102;
    private static final int SLOT_Y   = 18;
    private static final int BG_WIDTH  = 132;
    private static final int BG_HEIGHT = 58;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public RarityForgeRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(RarityForgeRegistration.getBlock()));
        this.title = Component.literal("レアリティ解放テーブル");
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
        if (!recipe.getCat0Candidates().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, CAT0_X + 1, SLOT_Y + 1)
                    .addItemStacks(recipe.getCat0Candidates());
        }
        if (!recipe.getCat1Candidates().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, CAT1_X + 1, SLOT_Y + 1)
                    .addItemStacks(recipe.getCat1Candidates());
        }
        if (!recipe.getCenterCandidates().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, CENTER_X + 1, SLOT_Y + 1)
                    .addItemStacks(recipe.getCenterCandidates());
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

        // 上部にモード説明
        String mode = switch (recipe.getKind()) {
            case BOOK_ELEMENT -> "§b" + recipe.getDescription();
            case UNBREAKABLE  -> "§6" + recipe.getDescription();
            case RARITY       -> "§d" + recipe.getDescription();
        };
        gfx.drawString(mc.font, mode, 4, 4, 0xFFFFFF, false);

        // スロット背景
        if (!recipe.getCat0Candidates().isEmpty()) drawSlotBg(gfx, CAT0_X, SLOT_Y);
        if (!recipe.getCat1Candidates().isEmpty()) drawSlotBg(gfx, CAT1_X, SLOT_Y);
        if (!recipe.getCenterCandidates().isEmpty()) drawSlotBg(gfx, CENTER_X, SLOT_Y);
        if (!recipe.getOutputCandidates().isEmpty()) drawSlotBg(gfx, RESULT_X, SLOT_Y);

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
