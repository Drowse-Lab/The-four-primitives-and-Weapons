package the_four_primitives_and_weapons.integration.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 漆の採取 ( 原木に 火打石/空きビン を使う ) を Create mod のように <b>3Dの原木</b> で見せる JEI カテゴリ。
 *
 * 表示: [道具] → 3D原木 → [生漆]
 */
public class UrushiTapCategory implements IRecipeCategory<UrushiTapRecipe> {

	public static final RecipeType<UrushiTapRecipe> RECIPE_TYPE =
			RecipeType.create("the_four_primitives_and_weapons", "urushi_tap", UrushiTapRecipe.class);

	private static final int BG_WIDTH  = 120;
	private static final int BG_HEIGHT = 60;
	private static final int TOOL_X = 6,  TOOL_Y = 22;     // 道具スロット
	private static final int RESULT_X = 96, RESULT_Y = 22; // 生漆スロット
	private static final int BLOCK_CX = 56, BLOCK_CY = 32; // 3D原木の中心
	private static final float BLOCK_SCALE = 22f;

	private final IDrawable background;
	private final IDrawable icon;
	private final Component title;

	public UrushiTapCategory(IGuiHelper guiHelper) {
		this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
		this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
				new ItemStack(the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems.RAW_URUSHI.get()));
		this.title = Component.translatable("jei.the_four_primitives_and_weapons.urushi_tap");
	}

	@Override public RecipeType<UrushiTapRecipe> getRecipeType() { return RECIPE_TYPE; }
	@Override public Component getTitle() { return title; }
	@Override public IDrawable getBackground() { return background; }
	@Override public IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, UrushiTapRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.CATALYST, TOOL_X + 1, TOOL_Y + 1)
				.addItemStacks(recipe.tools);
		builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, RESULT_Y + 1)
				.addItemStack(recipe.result);
	}

	@Override
	public void draw(UrushiTapRecipe recipe, IRecipeSlotsView slots, GuiGraphics gfx,
					 double mouseX, double mouseY) {
		Minecraft mc = Minecraft.getInstance();
		// 説明
		gfx.drawString(mc.font, Component.translatable("jei.the_four_primitives_and_weapons.urushi_tap.hint"),
				2, 2, 0x555555, false);
		// スロット枠
		drawSlotBg(gfx, TOOL_X, TOOL_Y);
		drawSlotBg(gfx, RESULT_X, RESULT_Y);
		// 矢印
		gfx.drawString(mc.font, "→", 80, RESULT_Y + 5, 0x404040, false);
		// 3D 原木
		renderBlock3D(gfx, recipe.block, BLOCK_CX, BLOCK_CY, BLOCK_SCALE);
	}

	/** GUI 内にブロックモデルをアイソメトリックな 3D で描画する。 */
	private static void renderBlock3D(GuiGraphics gfx, net.minecraft.world.level.block.state.BlockState state,
									  int cx, int cy, float scale) {
		Minecraft mc = Minecraft.getInstance();
		PoseStack pose = gfx.pose();
		pose.pushPose();
		pose.translate(cx, cy, 100.0);
		pose.scale(scale, -scale, scale);          // Y 反転 ( GUI 座標系 )
		pose.mulPose(Axis.XP.rotationDegrees(30f)); // 俯瞰
		pose.mulPose(Axis.YP.rotationDegrees(45f)); // 斜め
		pose.translate(-0.5, -0.5, -0.5);           // ブロック中心を原点に
		Lighting.setupFor3DItems();
		MultiBufferSource.BufferSource buf = gfx.bufferSource();
		mc.getBlockRenderer().renderSingleBlock(state, pose, buf,
				LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
		gfx.flush();
		pose.popPose();
	}

	private static void drawSlotBg(GuiGraphics gfx, int sx, int sy) {
		gfx.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
		gfx.fill(sx, sy, sx + 18, sy + 1, 0xFF373737);
		gfx.fill(sx, sy, sx + 1, sy + 18, 0xFF373737);
		gfx.fill(sx + 17, sy, sx + 18, sy + 18, 0xFFFFFFFF);
		gfx.fill(sx, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
	}

	@SuppressWarnings("unused")
	private static final ResourceLocation UID =
			new ResourceLocation("the_four_primitives_and_weapons", "urushi_tap");
}
