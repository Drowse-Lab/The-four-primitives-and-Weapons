package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.GloveItem;
import the_four_primitives_and_weapons.item.IronGauntletsItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Curios スロットに装備できるアイテムに「〜スロットに装備可能」ツールチップを表示する
 * ( ElytraSlot / Trinkets 風の表示 )。
 *
 *   エリトラ ( バニラ ) → elytra スロット
 *   手袋類 ( GloveItem 系 / 鉄の籠手 ) → hands スロット
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class CuriosEquippableTooltipHandler {

	private static final String KEY = "tooltip.the_four_primitives_and_weapons.curios_equippable";

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		Item item = stack.getItem();

		String slotId;
		if (item == Items.ELYTRA) {
			slotId = "elytra";
		} else if (item instanceof GloveItem || item instanceof IronGauntletsItem) {
			slotId = "hands";
		} else {
			return;
		}

		event.getToolTip().add(Component.translatable(KEY,
				Component.translatable("curios.identifier." + slotId).withStyle(ChatFormatting.BLUE))
				.withStyle(ChatFormatting.GRAY));
	}
}
