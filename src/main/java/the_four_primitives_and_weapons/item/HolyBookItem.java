
package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.ElementalTooltipEvent;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import javax.annotation.Nullable;

public class HolyBookItem extends Item implements ICurioItem {
	public HolyBookItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		int lv = ElementalDamageUtils.getElementLevel(stack);
		if (lv >= 1 && lv <= 10) {
			tooltip.add(Component.literal("§6").append(Component.translatable("tooltip.the_four_primitives_and_weapons.element.holy")).append(Component.literal(" " + ElementalTooltipEvent.toRoman(lv))));
		} else if (lv != 0) {
			tooltip.add(ErasureBookItem.buildErasureComponent());
		}
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		return true;
	}

	@Override
	public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
		return true;
	}
}
