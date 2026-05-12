package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * お札 - Ofuda (Seal Talisman)
 * 封印に使う護符。霊刀の鞘と組み合わせて封印鞘を作れる。
 */
public class OfudaItem extends Item {
	public OfudaItem() {
		super(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, world, list, flag);
		list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.ofuda")
				.setStyle(Style.EMPTY.withColor(TextColor.parseColor("#3366FF")).withItalic(true)));
	}
}