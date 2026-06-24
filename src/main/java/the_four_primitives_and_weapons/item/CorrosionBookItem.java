
package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
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

public class CorrosionBookItem extends Item implements ICurioItem {
	public CorrosionBookItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		// 正常 lv (1-10) の "Corrosion Element X" 表示は ElementalTooltipEvent が
		// 全アイテム共通で挿入するのでここでは出さない ( 二重表示防止 )。
		// 異常 lv ( = NBT 改ざん / 範囲外 ) のときだけ ERROR 表記を出す。
		int lv = ElementalDamageUtils.getElementLevel(stack);
		if (lv != 0 && (lv < 1 || lv > 10)) {
			tooltip.add(ErrorBookItem.buildErrorComponent());
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
