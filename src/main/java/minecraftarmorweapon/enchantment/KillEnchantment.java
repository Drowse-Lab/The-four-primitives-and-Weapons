
package minecraftarmorweapon.enchantment;

import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.entity.EquipmentSlot;

public class KillEnchantment extends Enchantment {
	public KillEnchantment(EquipmentSlot... slots) {
		super(Enchantment.Rarity.RARE, EnchantmentCategory.WEAPON, slots);
	}

	@Override
	public int getMaxLevel() {
		return 10;
	}
	//levelが1上がると即死の確率が10%上がる
	@Override
	public boolean canEnchant(ItemStack stack) {
		// 剣、斧、弓、クロスボウに適用可能
		return super.canEnchant(stack)
			|| stack.getItem() instanceof SwordItem
			|| stack.getItem() instanceof AxeItem
			|| stack.getItem() instanceof BowItem
			|| stack.getItem() instanceof CrossbowItem;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		// エンチャントテーブルでも剣、斧、弓、クロスボウに適用可能
		return super.canApplyAtEnchantingTable(stack)
			|| stack.getItem() instanceof SwordItem
			|| stack.getItem() instanceof AxeItem
			|| stack.getItem() instanceof BowItem
			|| stack.getItem() instanceof CrossbowItem;
	}

	@Override
	public boolean isTreasureOnly() {
		return true;
	}

	@Override
	public boolean isTradeable() {
		return false;
	}
}
