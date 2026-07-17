package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.GloveItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 染色可能アイテム（バニラの革防具・本MODの制服 上着など）のツールチップに、
 * 色を16進数(#RRGGBB)で表示する。 言語対応(translatable)・クラフト染色にも対応。
 * HideFlags:64 (TooltipPart.DYE) が立っていれば非表示。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class DyeTooltipHandler {

	private static final String KEY_GLOVE = "tooltip.the_four_primitives_and_weapons.glove_color";
	private static final String KEY_COLOR = "tooltip.the_four_primitives_and_weapons.color";

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (!(stack.getItem() instanceof DyeableLeatherItem dye)) return;
		// 手袋 ( GloveItem ) は未染色でも常に現在色 ( defaultColor ) を表示する
		boolean isGlove = stack.getItem() instanceof GloveItem;
		if (!dye.hasCustomColor(stack) && !isGlove) return;

		// HideFlags:64 で非表示
		int hideFlags = (stack.getTag() != null && stack.getTag().contains("HideFlags", 99))
				? stack.getTag().getInt("HideFlags") : 0;
		if ((hideFlags & ItemStack.TooltipPart.DYE.getMask()) != 0) return;

		List<Component> tip = event.getToolTip();
		// バニラの "Dyed" / "Color: #..." 行は除去して、 自前の16進数行に統一
		tip.removeIf(c -> isKey(c, "item.dyed") || isKey(c, "item.color"));

		int color = dye.getColor(stack) & 0xFFFFFF;
		boolean glove = isGlove
				|| (stack.getTag() != null && stack.getTag().contains("GloveColor", 3)); // GloveColor タグ = 手袋色
		String key = glove ? KEY_GLOVE : KEY_COLOR;
		// カラーコード部分は実際の色で表示する
		Component hex = Component.literal(String.format("#%06X", color))
				.withStyle(style -> style.withColor(color));
		tip.add(Component.translatable(key, hex).withStyle(ChatFormatting.GRAY));
	}

	private static boolean isKey(Component c, String key) {
		return c.getContents() instanceof TranslatableContents tc && key.equals(tc.getKey());
	}
}
