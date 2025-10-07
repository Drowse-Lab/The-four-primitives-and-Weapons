
package minecraftarmorweapon.enchantment;

import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.entity.EquipmentSlot;

public class DemonizedEnchantment extends Enchantment {
	public DemonizedEnchantment(EquipmentSlot... slots) {
		super(Enchantment.Rarity.RARE, EnchantmentCategory.WEAPON, slots);
	}

	@Override
	public int getMaxLevel() {
		return 10;
	}

	@Override
	public boolean canEnchant(ItemStack stack) {
		// 剣と斧に適用可能
		return super.canEnchant(stack)
			|| stack.getItem() instanceof SwordItem
			|| stack.getItem() instanceof AxeItem;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		// エンチャントテーブルでも剣と斧に適用可能
		return super.canApplyAtEnchantingTable(stack)
			|| stack.getItem() instanceof SwordItem
			|| stack.getItem() instanceof AxeItem;
	}

	@Override
	public boolean isTradeable() {
		return false;
	}
}
