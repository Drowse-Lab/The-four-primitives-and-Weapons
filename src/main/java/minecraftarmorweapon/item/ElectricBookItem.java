
package minecraftarmorweapon.item;

import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

public class ElectricBookItem extends Item implements ICurioItem {
	public ElectricBookItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
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
