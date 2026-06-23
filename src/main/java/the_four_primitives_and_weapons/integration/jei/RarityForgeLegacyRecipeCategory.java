package the_four_primitives_and_weapons.integration.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.init.RarityForgeRegistration;
import the_four_primitives_and_weapons.item.rarity.RarityForgeCenterLogic;
import the_four_primitives_and_weapons.item.rarity.RarityForgeRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 旧 rarity_forge_recipes/*.json レシピ ( shaped + book + unbreakable ) を JEI で表示。
 *
 * 表示:
 *   [触媒0]   [3×3 grid]   →   [結果]
 */
public class RarityForgeLegacyRecipeCategory implements IRecipeCategory<RarityForgeRecipe> {

    public static final ResourceLocation UID = new ResourceLocation(
            "the_four_primitives_and_weapons", "rarity_forge_legacy");
    public static final RecipeType<RarityForgeRecipe> RECIPE_TYPE =
            RecipeType.create("the_four_primitives_and_weapons", "rarity_forge_legacy", RarityForgeRecipe.class);

    private static final int CAT_X     = 4;
    private static final int CAT_Y     = 19;
    private static final int GRID_X    = 28;
    private static final int GRID_Y    = 0;
    private static final int RESULT_X  = 100;
    private static final int RESULT_Y  = 19;
    private static final int BG_WIDTH  = 140;
    private static final int BG_HEIGHT = 60;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public RarityForgeLegacyRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(RarityForgeRegistration.getBlock()));
        this.title = Component.literal("レアリティ解放テーブル ( JSON )");
    }

    @Override public RecipeType<RarityForgeRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RarityForgeRecipe recipe, IFocusGroup focuses) {
        // 1) 触媒スロット ( book recipe で catalyst_levels がある場合のみ )
        if (recipe.hasCatalystLevels()) {
            List<ItemStack> cats = new ArrayList<>();
            recipe.getCatalystLevels().entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getValue))
                    .forEach(e -> {
                        ItemStack st = new ItemStack(e.getKey());
                        st.setHoverName(Component.literal(
                                e.getKey().getDescription().getString() + " → Lv. " + e.getValue()));
                        cats.add(st);
                    });
            builder.addSlot(RecipeIngredientRole.INPUT, CAT_X + 1, CAT_Y + 1)
                    .addItemStacks(cats);
        }

        // 2) 3×3 グリッド ( pattern の中央寄せ )
        Item[][] pattern = recipe.getPattern();
        int pw = recipe.getPatternWidth();
        int ph = recipe.getPatternHeight();
        int offsetX = ((3 - pw) / 2) * 18;
        int offsetY = ((3 - ph) / 2) * 18;
        int inputSlotIdx = recipe.getInputSlotIndex();
        for (int y = 0; y < ph; y++) {
            for (int x = 0; x < pw; x++) {
                int patIdx = y * pw + x;
                int sx = GRID_X + 1 + offsetX + x * 18;
                int sy = GRID_Y + 1 + offsetY + y * 18;
                if (pattern[y][x] != null) {
                    builder.addSlot(RecipeIngredientRole.INPUT, sx, sy)
                            .addItemStack(new ItemStack(pattern[y][x]));
                } else if (recipe.isUnbreakable() && patIdx == inputSlotIdx) {
                    builder.addSlot(RecipeIngredientRole.INPUT, sx, sy)
                            .addItemStack(new ItemStack(Items.IRON_SWORD));
                }
            }
        }

        // 3) 結果スロット
        if (recipe.isUnbreakable()) {
            ItemStack out = new ItemStack(Items.IRON_SWORD);
            out.getOrCreateTag().putBoolean("Unbreakable", true);
            builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, RESULT_Y + 1)
                    .addItemStack(out);
        } else if (recipe.isBookRecipe() && recipe.hasCatalystLevels()) {
            List<ItemStack> outs = new ArrayList<>();
            ElementType type = RarityForgeCenterLogic.getBookElement(new ItemStack(recipe.getResult()));
            recipe.getCatalystLevels().entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getValue))
                    .forEach(e -> {
                        ItemStack out = new ItemStack(recipe.getResult());
                        if (type != ElementType.NONE) {
                            ElementalDamageUtils.setElement(out, type, e.getValue());
                        }
                        out.setHoverName(Component.literal(
                                recipe.getResult().getDescription().getString() + " Lv. " + e.getValue()));
                        outs.add(out);
                    });
            builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, RESULT_Y + 1)
                    .addItemStacks(outs);
        } else {
            builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, RESULT_Y + 1)
                    .addItemStack(new ItemStack(recipe.getResult()));
        }
    }

    @Override
    public void draw(RarityForgeRecipe recipe, IRecipeSlotsView slots, GuiGraphics gfx,
                     double mouseX, double mouseY) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (recipe.hasCatalystLevels()) {
            drawSlotBg(gfx, CAT_X, CAT_Y);
        }
        int pw = recipe.getPatternWidth();
        int ph = recipe.getPatternHeight();
        int offsetX = ((3 - pw) / 2) * 18;
        int offsetY = ((3 - ph) / 2) * 18;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                drawSlotBg(gfx, GRID_X + c * 18, GRID_Y + r * 18);
            }
        }
        drawSlotBg(gfx, RESULT_X, RESULT_Y);
        gfx.drawString(mc.font, "→", RESULT_X - 12, RESULT_Y + 5, 0x404040, false);
    }

    private static void drawSlotBg(GuiGraphics gfx, int sx, int sy) {
        gfx.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
        gfx.fill(sx, sy, sx + 18, sy + 1, 0xFF373737);
        gfx.fill(sx, sy, sx + 1, sy + 18, 0xFF373737);
        gfx.fill(sx + 17, sy, sx + 18, sy + 18, 0xFFFFFFFF);
        gfx.fill(sx, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
    }
}
