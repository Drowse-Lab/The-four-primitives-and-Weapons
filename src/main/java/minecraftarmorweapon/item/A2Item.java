
package minecraftarmorweapon.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;

import minecraftarmorweapon.procedures.A2aitemuwoShoudeChituteiruJiannoteitukuProcedure;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

public class A2Item extends Item {
	public A2Item() {
		super(new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_YOPKEINAMONO).stacksTo(1).rarity(Rarity.COMMON));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			A2aitemuwoShoudeChituteiruJiannoteitukuProcedure.execute(entity);
	}
}
