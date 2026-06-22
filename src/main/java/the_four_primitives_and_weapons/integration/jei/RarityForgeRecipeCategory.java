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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RarityForgeRecipeCategory implements IRecipeCategory<RarityForgeRecipe> {

    public static final ResourceLocation UID = new ResourceLocation("the_four_primitives_and_weapons", "rarity_forge");
    public static final RecipeType<RarityForgeRecipe> RECIPE_TYPE =
            RecipeType.create("the_four_primitives_and_weapons", "rarity_forge", RarityForgeRecipe.class);

    // レイアウト:
    //   [触媒]   [3×3 grid]   →   [result]
    //    0           24                94
    //   1px 余白 + 18px slot
    private static final int CATALYST_X = 1;
    private static final int CATALYST_Y = 19;
    private static final int GRID_X     = 24; // 触媒分 (18+5=23) のあと
    private static final int GRID_Y     = 0;
    private static final int RESULT_X   = 95;
    private static final int RESULT_Y   = 19;
    private static final int BG_WIDTH   = 140;
    private static final int BG_HEIGHT  = 54;

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
        // 1) 触媒スロット: catalyst_levels が設定されてる book recipe では required 入力として表示
        if (recipe.hasCatalystLevels()) {
            Map<Item, Integer> levels = recipe.getCatalystLevels();
            // Lv 昇順で表示してローテーション
            List<ItemStack> catalysts = new ArrayList<>();
            levels.entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getValue))
                    .forEach(e -> {
                        ItemStack st = new ItemStack(e.getKey());
                        // Lv 情報を tooltip に重ねる
                        st.setHoverName(Component.literal(
                                e.getKey().getDescription().getString() + "  → Lv." + e.getValue()));
                        catalysts.add(st);
                    });
            builder.addSlot(RecipeIngredientRole.INPUT, CATALYST_X, CATALYST_Y)
                    .addItemStacks(catalysts)
                    .addTooltipCallback((slotView, tooltip) -> {
                        tooltip.add(Component.literal("§7触媒スロット — 選んだ item で Lv が決まる"));
                    });
        }

        // 2) 3×3 グリッドの中央寄せ
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
                    // #inputスロット: 鉄の剣をサンプルとして表示
                    builder.addSlot(RecipeIngredientRole.INPUT, sx, sy)
                            .addItemStack(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
                }
            }
        }

        // 3) 結果スロット
        if (recipe.isUnbreakable()) {
            ItemStack resultDisplay = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            resultDisplay.getOrCreateTag().putBoolean("Unbreakable", true);
            builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X, RESULT_Y)
                    .addItemStack(resultDisplay);
        } else if (recipe.isBookRecipe() && recipe.hasCatalystLevels()) {
            // book recipe の結果は Lv 値ごとに 10 種類 (ローテーション表示)
            List<ItemStack> bookResults = new ArrayList<>();
            recipe.getCatalystLevels().entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getValue))
                    .forEach(e -> {
                        ItemStack st = new ItemStack(recipe.getResult());
                        // 表示名に Lv を載せる (visual only — element 付与は menu 側で行う)
                        st.setHoverName(Component.literal(
                                recipe.getResult().getDescription().getString() + " Lv." + e.getValue()));
                        bookResults.add(st);
                    });
            builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X, RESULT_Y)
                    .addItemStacks(bookResults);
        } else {
            builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X, RESULT_Y)
                    .addItemStack(new ItemStack(recipe.getResult()));
        }
    }

    @Override
    public void draw(RarityForgeRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics gfx, double mouseX, double mouseY) {
        // 触媒スロット背景 (book recipe のみ)
        if (recipe.hasCatalystLevels()) {
            drawSlotBg(gfx, CATALYST_X - 1, CATALYST_Y - 1);
        }

        // 3×3グリッド背景
        int pw = recipe.getPatternWidth();
        int ph = recipe.getPatternHeight();
        int offsetX = ((3 - pw) / 2) * 18;
        int offsetY = ((3 - ph) / 2) * 18;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int sx = GRID_X + c * 18;
                int sy = GRID_Y + r * 18;
                drawSlotBg(gfx, sx, sy);
            }
        }

        // 結果スロット背景
        drawSlotBg(gfx, RESULT_X - 1, RESULT_Y - 1);

        // 矢印
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        gfx.drawString(mc.font, "→", RESULT_X - 13, RESULT_Y + 4, 0x404040, false);
    }

    private static void drawSlotBg(GuiGraphics gfx, int sx, int sy) {
        gfx.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
        gfx.fill(sx, sy, sx + 18, sy + 1, 0xFF373737);
        gfx.fill(sx, sy, sx + 1, sy + 18, 0xFF373737);
        gfx.fill(sx + 17, sy, sx + 18, sy + 18, 0xFFFFFFFF);
        gfx.fill(sx, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
    }
}
