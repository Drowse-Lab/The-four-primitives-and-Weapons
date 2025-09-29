
package minecraftarmorweapon.enchantment;

import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
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
		// 通常の武器に加えて、弓とクロスボウにも適用可能
		return super.canEnchant(stack) 
			|| stack.getItem() instanceof BowItem 
			|| stack.getItem() instanceof CrossbowItem;
	}
	
	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		// エンチャントテーブルでも弓に適用可能
		return super.canApplyAtEnchantingTable(stack) 
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
